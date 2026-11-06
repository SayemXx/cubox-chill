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
            byte[] bytes;
            try(DataInputStream in=new DataInputStream(new FileInputStream(f))){
                long written=in.readLong();if(now-written>MAX_AGE||written>now+60000){return null;}
                bytes=new byte[(int)f.length()-8];in.readFully(bytes);
            }
            f.setLastModified(now);return bytes;
        }catch(Exception ignored){return null;}
    }
    synchronized void put(String url,byte[] bytes,long now){
        if(bytes.length==0||bytes.length>MAX_IMAGE||bytes.length+8>limit)return;
        File temp=null;
        try{
            if(!directory.isDirectory()&&!directory.mkdirs())return;
            File target=file(url);temp=File.createTempFile("artwork-",".tmp",directory);
            try(DataOutputStream out=new DataOutputStream(new FileOutputStream(temp))){out.writeLong(now);out.write(bytes);}
            Files.move(temp.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);target.setLastModified(now);
            File[] files=directory.listFiles((d,n)->n.endsWith(".img"));if(files==null)return;
            Arrays.sort(files,Comparator.comparingLong(File::lastModified));long size=0;for(File f:files)size+=f.length();
            for(File f:files){if(size<=limit)break;long n=f.length();if(f.delete())size-=n;}
        }catch(Exception ignored){}finally{if(temp!=null&&temp.exists())temp.delete();}
    }
}
