package app.moviebox.mobile;
import android.app.*;
import android.graphics.Bitmap;
import android.widget.ImageView;
import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

final class ArtworkSmoke {
    static void run(Instrumentation test,Activity activity)throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();Bitmap bitmap=Bitmap.createBitmap(4,4,Bitmap.Config.ARGB_8888);bitmap.eraseColor(android.graphics.Color.RED);bitmap.compress(Bitmap.CompressFormat.PNG,100,bytes);bitmap.recycle();byte[] png=bytes.toByteArray();
        AtomicInteger hits=new AtomicInteger();ServerSocket server=new ServerSocket(0,4,InetAddress.getByName("127.0.0.1"));
        Thread thread=new Thread(()->{while(!server.isClosed())try(Socket socket=server.accept()){
            socket.setSoTimeout(3000);BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream()));String line;while((line=in.readLine())!=null&&!line.isEmpty()){}
            hits.incrementAndGet();OutputStream out=socket.getOutputStream();out.write(("HTTP/1.1 200 OK\r\nContent-Type: image/png\r\nContent-Length: "+png.length+"\r\nConnection: close\r\n\r\n").getBytes());out.write(png);out.flush();
        }catch(Exception ignored){}});thread.setDaemon(true);thread.start();
        String url="http://127.0.0.1:"+server.getLocalPort()+"/poster-"+System.nanoTime()+".png";ImageView[] views=new ImageView[3];
        try{
            test.runOnMainSync(()->{for(int i=0;i<views.length;i++)views[i]=new ImageView(activity);Posters.load(views[0],url);Posters.load(views[1],url);});
            await(test,views[0]);await(test,views[1]);if(hits.get()!=1)throw new AssertionError("Duplicate artwork network requests");
            server.close();test.runOnMainSync(()->{Posters.trimMemory();Posters.load(views[2],url);});await(test,views[2]);
            if(hits.get()!=1)throw new AssertionError("Artwork was downloaded again instead of read from disk");
        }finally{server.close();}
    }
    static void await(Instrumentation test,ImageView view)throws Exception {
        boolean[] loaded={false};for(int i=0;i<100;i++){test.runOnMainSync(()->loaded[0]=view.getDrawable()!=null);if(loaded[0])return;Thread.sleep(100);}
        throw new AssertionError("Artwork did not load from the expected cache/source");
    }
}
