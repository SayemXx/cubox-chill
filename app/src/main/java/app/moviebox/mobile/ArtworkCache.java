package app.moviebox.mobile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Bounded persistent artwork cache; 30-day expiry and oldest-used eviction. */
final class ArtworkCache {
    static final long MAX_AGE=30L*24*60*60*1000;
    static final int MAX_IMAGE=5*1024*1024;
    final File directory; final long limit;
    ArtworkCache(File directory,long limit){this.directory=directory;this.limit=limit;}
    private File file(String url)throws Exception {
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));
        StringBuilder key=new StringBuilder();for(byte b:digest)key.append(String.format(Locale.ROOT,"%02x",b&255));
        return new File(directory,key+".img");
    }
    synchronized byte[] get(String url,long now){
        try{
            File f=file(url);if(!f.isFile())return null;
            if(f.length()<9||f.length()>MAX_IMAGE+8){f.delete();return null;}
            if(expired(f,now)){f.delete();return null;}
            byte[] bytes;
            try(DataInputStream in=new DataInputStream(new FileInputStream(f))){
                in.readLong(); // Preserve the existing on-disk format. Expiry is based on last use.

                bytes=new byte[(int)f.length()-8];in.readFully(bytes);
            }
            f.setLastModified(now);return bytes;
        }catch(Exception ignored){return null;}
    }
    private boolean expired(File file,long now){
        return now-file.lastModified()>MAX_AGE || file.lastModified()>now+60000;
    }
    synchronized void touch(String url,long now){
        try{File f=file(url);if(f.isFile())f.setLastModified(now);}catch(Exception ignored){}
    }
    /** Run on startup and periodically while artwork is used, including memory hits. */
    synchronized void prune(long now){
        File[] files=directory.listFiles((d,n)->n.endsWith(".img"));if(files==null)return;
        Arrays.sort(files,Comparator.comparingLong(File::lastModified));long size=0;
        for(File f:files){if(expired(f,now)&&f.delete())continue;size+=f.length();}
        for(File f:files){if(size<=limit)break;long n=f.length();if(f.delete())size-=n;}
    }
    synchronized void migrateFrom(File previous){
        if(!directory.isDirectory()&&!directory.mkdirs())return;
        File[] files=previous.listFiles((d,n)->n.endsWith(".img"));if(files==null)return;
        for(File f:files){
            File target=new File(directory,f.getName());
            try{if(!target.exists())Files.move(f.toPath(),target.toPath());}catch(IOException ignored){}
        }
    }
    synchronized void put(String url,byte[] bytes,long now){
        if(bytes.length==0||bytes.length>MAX_IMAGE||bytes.length+8>limit)return;
        File temp=null;
        try{
            if(!directory.isDirectory()&&!directory.mkdirs())return;
            File target=file(url);temp=File.createTempFile("artwork-",".tmp",directory);
            try(DataOutputStream out=new DataOutputStream(new FileOutputStream(temp))){out.writeLong(now);out.write(bytes);}
            Files.move(temp.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);target.setLastModified(now);
            prune(now);
        }catch(Exception ignored){}finally{if(temp!=null&&temp.exists())temp.delete();}
    }
}
