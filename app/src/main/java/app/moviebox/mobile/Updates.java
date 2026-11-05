package app.moviebox.mobile;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
final class Updates {
    static JSONObject latest(String repo)throws Exception{
        if(!Versions.validRepository(repo))throw new Exception("Use a GitHub owner/repository name.");
        HttpURLConnection c=(HttpURLConnection)new URL("https://api.github.com/repos/"+repo+"/releases/latest").openConnection();
        c.setConnectTimeout(15000);c.setReadTimeout(15000);c.setRequestProperty("Accept","application/vnd.github+json");c.setRequestProperty("User-Agent","cubox-chill");
        try{
            int status=c.getResponseCode();
            if(status==404)throw new Exception("No published release was found.");
            if(status==403||status==429)throw new Exception("GitHub rate limit reached. Try later.");
            if(status!=200)throw new Exception("Update check failed (HTTP "+status+").");
            try(InputStream in=c.getInputStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
                byte[] buffer=new byte[8192];int count;
                while((count=in.read(buffer))!=-1){if(bytes.size()+count>2*1024*1024)throw new IOException("Release response is too large");bytes.write(buffer,0,count);}
                return new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
            }
        }finally{c.disconnect();}
    }
    static String apk(JSONObject release){
        JSONArray a=release.optJSONArray("assets");
        if(a!=null)for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null&&x.optString("name").endsWith(".apk")){String url=x.optString("browser_download_url");if(url.startsWith("https://github.com/"))return url;}}
        return "";
    }
}
