package app.moviebox.mobile;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;
import java.io.*;
import java.lang.ref.WeakReference;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** Memory + disk artwork cache, shared requests, and bounded background decoding. */
final class Posters {
    private static final ExecutorService IO=Executors.newFixedThreadPool(2);
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final Map<String,List<WeakReference<ImageView>>> PENDING=new HashMap<>();
    private static ArtworkCache disk;
    static void trimMemory(){CACHE.evictAll();}
    private static final LruCache<String,Bitmap> CACHE=new LruCache<String,Bitmap>(12*1024*1024){
        @Override protected int sizeOf(String key,Bitmap bitmap){return bitmap.getByteCount();}
    };
    static void load(ImageView view,String url){
        view.setTag(url);
        if(url==null||(!url.startsWith("https://")&&!url.startsWith("http://")))return;
        Bitmap cached=CACHE.get(url);if(cached!=null){view.setImageBitmap(cached);return;}
        if(disk==null)disk=new ArtworkCache(new File(view.getContext().getApplicationContext().getCacheDir(),"artwork-v1"),64L*1024*1024);
        List<WeakReference<ImageView>> waiting=PENDING.get(url);
        if(waiting!=null){waiting.add(new WeakReference<>(view));return;}
        waiting=new ArrayList<>();waiting.add(new WeakReference<>(view));PENDING.put(url,waiting);
        IO.execute(()->{
            Bitmap bitmap=null;
            try{
                byte[] bytes=disk.get(url,System.currentTimeMillis());if(bytes!=null)bitmap=decode(bytes);
                if(bitmap==null){bytes=download(url);if(bytes!=null){bitmap=decode(bytes);if(bitmap!=null)disk.put(url,bytes,System.currentTimeMillis());}}
                if(bitmap!=null)CACHE.put(url,bitmap);
            }catch(Exception ignored){}
            Bitmap result=bitmap;
            MAIN.post(()->{
                List<WeakReference<ImageView>> targets=PENDING.remove(url);if(targets==null||result==null)return;
                for(WeakReference<ImageView> ref:targets){ImageView target=ref.get();if(target!=null&&url.equals(target.getTag()))target.setImageBitmap(result);}
            });
        });
    }
    private static Bitmap decode(byte[] bytes){
        BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(options.outWidth<=0||options.outHeight<=0)return null;
        options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>1000)options.inSampleSize*=2;
        options.inJustDecodeBounds=false;options.inPreferredConfig=Bitmap.Config.RGB_565;
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
    }
    private static byte[] download(String url)throws Exception {
        HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
        try{
            connection.setConnectTimeout(10000);connection.setReadTimeout(10000);
            if(connection.getResponseCode()!=200||connection.getContentLengthLong()>ArtworkCache.MAX_IMAGE)return null;
            try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buffer=new byte[8192];int count;
                while((count=in.read(buffer))!=-1){if(out.size()+count>ArtworkCache.MAX_IMAGE)return null;out.write(buffer,0,count);}
                return out.toByteArray();
            }
        }finally{connection.disconnect();}
    }
}
