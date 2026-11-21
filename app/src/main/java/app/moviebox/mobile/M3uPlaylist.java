package app.moviebox.mobile;

import java.net.*;
import java.util.*;
import java.util.regex.*;

/** Parses IPTV channel lists, never treats HLS media segments as separate channels. */
final class M3uPlaylist {
    static final int MAX_CHANNELS=10000;
    static final class Channel {
        final String name,url,logo,group,website,note;
        final Map<String,String> headers;
        final boolean external;
        Channel(String name,String url,String logo,String group,Map<String,String> headers){this(name,url,logo,group,headers,"","",false);}
        Channel(String name,String url,String logo,String group,Map<String,String> headers,String website,String note,boolean external){
            this.name=name;this.url=url;this.logo=logo;this.group=group;this.headers=Collections.unmodifiableMap(new LinkedHashMap<>(headers));this.website=website;this.note=note;this.external=external;
        }
    }
    static boolean web(String url){try{URI u=new URI(url);return ("https".equalsIgnoreCase(u.getScheme())||"http".equalsIgnoreCase(u.getScheme()))&&u.getHost()!=null;}catch(Exception e){return false;}}
    static String resolve(String base,String value){try{String u=new URI(base).resolve(value.trim()).toString();return web(u)?u:"";}catch(Exception e){return "";}}
    static int comma(String line){char quote=0;for(int i=0;i<line.length();i++){char c=line.charAt(i);if(quote!=0){if(c==quote)quote=0;}else if(c=='\"'||c=='\'')quote=c;else if(c==',')return i;}return -1;}
    static Map<String,String> attributes(String text){Map<String,String> out=new HashMap<>();Matcher m=Pattern.compile("([\\w-]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s]+))").matcher(text);while(m.find())out.put(m.group(1).toLowerCase(Locale.ROOT),m.group(2)!=null?m.group(2):m.group(3)!=null?m.group(3):m.group(4));return out;}
    static void header(Map<String,String> out,String key,String value){
        if(value.indexOf('\r')>=0||value.indexOf('\n')>=0||value.length()>2048)return;
        if(key.equalsIgnoreCase("http-user-agent")||key.equalsIgnoreCase("user-agent"))out.put("User-Agent",value);
        if(key.equalsIgnoreCase("http-referrer")||key.equalsIgnoreCase("http-referer")||key.equalsIgnoreCase("referer"))out.put("Referer",value);
        if(key.equalsIgnoreCase("origin"))out.put("Origin",value);
    }
    static List<Channel> parse(String input,String base){
        if(input.length()>5*1024*1024)throw new IllegalArgumentException("Playlist is larger than 5 MB");
        String text=input.replace("\ufeff","");
        if(Pattern.compile("(?m)^#EXT-X-").matcher(text).find()){
            if(!web(base))throw new IllegalArgumentException("Import an IPTV channel list, not a local HLS segment playlist");
            return Collections.singletonList(new Channel("Live stream",base,"","Live",Collections.emptyMap()));
        }
        List<Channel> out=new ArrayList<>();Set<String> seen=new HashSet<>();
        String name="",logo="",group="";boolean drm=false;Map<String,String> headers=new LinkedHashMap<>();
        for(String raw:text.split("\\r?\\n")){
            String line=raw.trim();if(line.isEmpty()||line.length()>16384)continue;
            if(line.startsWith("#EXTINF:")){
                int split=comma(line);Map<String,String> a=attributes(split<0?line:line.substring(0,split));
                name=split<0?a.getOrDefault("tvg-name",""):line.substring(split+1).trim();
                logo=resolve(base,a.getOrDefault("tvg-logo",""));if(a.getOrDefault("tvg-logo","").isEmpty())logo="";
                group=a.getOrDefault("group-title","");headers=new LinkedHashMap<>();drm=false;
                header(headers,"User-Agent",a.getOrDefault("user-agent",""));if(headers.getOrDefault("User-Agent","").isEmpty())headers.clear();
            }else if(line.startsWith("#EXTGRP:")){group=line.substring(8).trim();}
            else if(line.startsWith("#EXTVLCOPT:")){int equal=line.indexOf('=');if(equal>0)header(headers,line.substring(11,equal),line.substring(equal+1));}
            else if(line.startsWith("#KODIPROP:")&&line.toLowerCase(Locale.ROOT).contains("license")){drm=true;}
            else if(!line.startsWith("#")){
                String[] parts=line.split("\\|",2);String url=resolve(base,parts[0]);
                if(!text.trim().startsWith("#EXTM3U")&&!M3uPlaylist.web(parts[0]))continue;
                if(parts.length==2)for(String field:parts[1].split("&")){int eq=field.indexOf('=');if(eq>0)try{header(headers,field.substring(0,eq),URLDecoder.decode(field.substring(eq+1),"UTF-8"));}catch(Exception ignored){}}
                if(!drm&&!url.isEmpty()&&seen.add(url+headers.toString())){
                    out.add(new Channel(name.isEmpty()?"Channel "+(out.size()+1):name,url,logo,group,headers));
                    if(out.size()>MAX_CHANNELS)throw new IllegalArgumentException("Playlist exceeds 10,000 channels");
                }
                name="";logo="";group="";headers=new LinkedHashMap<>();drm=false;
            }
        }
        if(out.isEmpty())throw new IllegalArgumentException("No supported HTTP/HTTPS channels found in this playlist");
        return out;
    }
}
