package app.moviebox.mobile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Bounded artwork loading; failed artwork leaves the accessible title placeholder visible. */
final class Posters {
    private static final ExecutorService IO=Executors.newFixedThreadPool(2);
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final LruCache<String,Bitmap> CACHE=new LruCache<String,Bitmap>(12*1024*1024){
        @Override protected int sizeOf(String key,Bitmap bitmap){return bitmap.getByteCount();}
    };
    static void load(ImageView view,String url){
        view.setTag(url);
        if(url==null||(!url.startsWith("https://")&&!url.startsWith("http://")))return;
        Bitmap cached=CACHE.get(url);if(cached!=null){view.setImageBitmap(cached);return;}
        WeakReference<ImageView> ref=new WeakReference<>(view);
        IO.execute(()->{
            HttpURLConnection connection=null;
            try{
                connection=(HttpURLConnection)new URL(url).openConnection();
                connection.setConnectTimeout(10000);connection.setReadTimeout(10000);
                if(connection.getResponseCode()!=200)return;
                if(connection.getContentLengthLong()>5*1024*1024)return;
                byte[] bytes;
                try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    byte[] buffer=new byte[8192];int count;
                    while((count=in.read(buffer))!=-1){if(out.size()+count>5*1024*1024)return;out.write(buffer,0,count);}
                    bytes=out.toByteArray();
                }
                BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
                BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
                if(options.outWidth<=0||options.outHeight<=0)return;
                options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>1000)options.inSampleSize*=2;
                options.inJustDecodeBounds=false;options.inPreferredConfig=Bitmap.Config.RGB_565;
                Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
                if(bitmap==null)return;CACHE.put(url,bitmap);
                MAIN.post(()->{ImageView target=ref.get();if(target!=null&&url.equals(target.getTag())){target.setImageBitmap(bitmap);target.animate().alpha(1f).setDuration(180).start();}});
            }catch(Exception ignored){}finally{if(connection!=null)connection.disconnect();}
        });
    }
}
