package app.moviebox.mobile;

import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

final class LiveTvStore {
    static final long FRESH_MS=24*60*60*1000L;
    static final int MAX_BYTES=5*1024*1024;
    final Context context;final File directory;
    static final class Source {
        final String id,name,url,category;final boolean builtIn;
        Source(String id,String name,String url,String category,boolean builtIn){this.id=id;this.name=name;this.url=url;this.category=category;this.builtIn=builtIn;}
    }
    static final class Fetched { final String text,base; Fetched(String text,String base){this.text=text;this.base=base;} }
    static final class Listing {
        final List<M3uPlaylist.Channel> channels;final long updated;
        Listing(List<M3uPlaylist.Channel> channels,long updated){this.channels=channels;this.updated=updated;}
    }
    LiveTvStore(Context context){this.context=context.getApplicationContext();directory=new File(context.getFilesDir(),"live-playlists");directory.mkdirs();}
    List<Source> sources(String category){
        List<Source> list=new ArrayList<>();String path=category.equals("BD")?"countries/bd":category.equals("India")?"countries/in":"categories/sports";
        list.add(new Source("iptv-"+category,"IPTV-org · "+category,"https://iptv-org.github.io/iptv/"+path+".m3u",category,true));
        list.add(new Source("official-"+category,"Official picks","",category,true));
        try{JSONArray saved=new JSONArray(context.getSharedPreferences("live-tv",0).getString("sources","[]"));
            for(int i=0;i<saved.length();i++){JSONObject s=saved.getJSONObject(i);if(category.equals(s.optString("category")))list.add(new Source(s.getString("id"),s.getString("name"),s.optString("url"),category,false));}
        }catch(JSONException ignored){}
        return list;
    }
    static String hash(String value){try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(Locale.ROOT,"%02x",b));return s.toString();}catch(Exception e){throw new IllegalStateException(e);}}
    File file(Source s){return new File(directory,hash(s.id)+".m3u");}
    Listing cached(Source s)throws Exception{
        if(s.id.startsWith("official-"))return official(s.category);
        File file=file(s);if(!file.exists())return null;
        try(InputStream in=new FileInputStream(file)){JSONObject saved=new JSONObject(read(in,MAX_BYTES*2+8192));return new Listing(M3uPlaylist.parse(saved.getString("text"),saved.optString("base",s.url)),file.lastModified());}
    }
    synchronized Listing refresh(Source s)throws Exception{
        if(s.id.startsWith("official-"))return official(s.category);
        if(s.url.isEmpty()){Listing cached=cached(s);if(cached==null)throw new IOException("Import this playlist file again");return cached;}
        Fetched raw=fetch(s.url);List<M3uPlaylist.Channel> parsed=M3uPlaylist.parse(raw.text,raw.base);saveFile(s,raw.text,raw.base);return new Listing(parsed,System.currentTimeMillis());
    }
    void saveFile(Source s,String raw,String base)throws Exception{
        AtomicFile atomic=new AtomicFile(file(s));FileOutputStream out=null;
        try{out=atomic.startWrite();out.write(new JSONObject().put("text",raw).put("base",base).toString().getBytes(StandardCharsets.UTF_8));atomic.finishWrite(out);}catch(IOException e){if(out!=null)atomic.failWrite(out);throw e;}
    }
    synchronized void add(Source s,String raw,String base)throws Exception{
        M3uPlaylist.parse(raw,base);
        JSONArray saved=new JSONArray(context.getSharedPreferences("live-tv",0).getString("sources","[]"));
        if(saved.length()>=30)throw new IOException("Remove a playlist before adding more (maximum 30)");
        saveFile(s,raw,base);
        saved.put(new JSONObject().put("id",s.id).put("name",s.name).put("url",s.url).put("category",s.category));
        context.getSharedPreferences("live-tv",0).edit().putString("sources",saved.toString()).apply();
    }
    synchronized void remove(Source source)throws Exception{
        if(source.builtIn)return;
        JSONArray old=new JSONArray(context.getSharedPreferences("live-tv",0).getString("sources","[]")),kept=new JSONArray();
        for(int i=0;i<old.length();i++)if(!source.id.equals(old.getJSONObject(i).optString("id")))kept.put(old.get(i));
        context.getSharedPreferences("live-tv",0).edit().putString("sources",kept.toString()).apply();new AtomicFile(file(source)).delete();
    }
    Set<String> favorites(){return new HashSet<>(context.getSharedPreferences("live-tv",0).getStringSet("favorites",Collections.emptySet()));}
    void favorite(M3uPlaylist.Channel c){Set<String> ids=favorites();String key=hash(c.url);if(!ids.remove(key))ids.add(key);context.getSharedPreferences("live-tv",0).edit().putStringSet("favorites",ids).apply();}
    static String normalize(String url){
        String value=url.trim();
        try{URI u=new URI(value);if("github.com".equalsIgnoreCase(u.getHost())){String[] p=u.getRawPath().split("/",6);if(p.length==6&&p[3].equals("blob"))value="https://raw.githubusercontent.com/"+p[1]+"/"+p[2]+"/"+p[4]+"/"+p[5];}}catch(Exception ignored){}
        return value;
    }
    static Fetched fetch(String address)throws IOException{
        String url=normalize(address);
        for(int redirect=0;redirect<6;redirect++){
            if(!M3uPlaylist.web(url))throw new IOException("Use an HTTP or HTTPS playlist URL");
            HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
            connection.setConnectTimeout(15000);connection.setReadTimeout(15000);connection.setInstanceFollowRedirects(false);connection.setRequestProperty("User-Agent","CuboxChill/0.7.0");
            try{int code=connection.getResponseCode();
                if(code>=300&&code<400){String location=connection.getHeaderField("Location");if(location==null)throw new IOException("Invalid playlist redirect");url=new URL(new URL(url),location).toString();continue;}
                if(code!=200)throw new IOException("Playlist server returned HTTP "+code);
                if(connection.getContentLengthLong()>MAX_BYTES)throw new IOException("Playlist is larger than 5 MB");
                try(InputStream in=connection.getInputStream()){return new Fetched(read(in),url);}
            }finally{connection.disconnect();}
        }
        throw new IOException("Too many playlist redirects");
    }
    static String read(InputStream in)throws IOException{return read(in,MAX_BYTES);}
    static String read(InputStream in,int maximum)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[16384];int n;
        while((n=in.read(buffer))!=-1){if(out.size()+n>maximum)throw new IOException("Playlist is larger than 5 MB");out.write(buffer,0,n);}
        return out.toString("UTF-8");
    }
    Listing official(String category)throws Exception{
        JSONArray array;try(InputStream in=context.getAssets().open("live-official.json")){array=new JSONArray(read(in));}
        List<M3uPlaylist.Channel> out=new ArrayList<>();
        for(int i=0;i<array.length();i++){JSONObject c=array.getJSONObject(i);if(category.equals(c.optString("group")))out.add(new M3uPlaylist.Channel(c.getString("name"),c.getString("url"),c.optString("logo"),category,Collections.emptyMap(),c.optString("website"),c.optString("note"),c.optBoolean("external")));}
        return new Listing(out,0);
    }
}
