package app.moviebox.mobile;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.offline.*;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.json.*;

@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
final class MediaSmoke {
    static void run(Instrumentation test,Activity activity)throws Exception {
        byte[] data;
        try(InputStream in=test.getContext().getAssets().open("test-video.mp4");ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))>=0)out.write(buffer,0,n);data=out.toByteArray();
        }
        ServerSocket server=new ServerSocket(0,8,InetAddress.getByName("127.0.0.1"));
        Thread serving=new Thread(()->{
            while(!server.isClosed())try(Socket socket=server.accept()){
                socket.setSoTimeout(5000);
                BufferedReader reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line=reader.readLine();if(line==null)continue;String range=null;boolean authenticated=false;
                while((line=reader.readLine())!=null&&!line.isEmpty()){
                    if(line.toLowerCase(java.util.Locale.ROOT).startsWith("range:"))range=line.substring(6).trim();
                    if(line.equalsIgnoreCase("X-Cubox-Test: expected"))authenticated=true;
                }
                OutputStream output=socket.getOutputStream();
                if(!authenticated){output.write("HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes());continue;}
                int start=0,end=data.length-1;boolean partial=range!=null&&range.startsWith("bytes=");
                if(partial){String[] bounds=range.substring(6).split("-",-1);start=Integer.parseInt(bounds[0]);if(bounds.length>1&&!bounds[1].isEmpty())end=Math.min(end,Integer.parseInt(bounds[1]));}
                if(start>=data.length){output.write(("HTTP/1.1 416 Range Not Satisfiable\r\nContent-Range: bytes */"+data.length+"\r\nContent-Length: 0\r\n\r\n").getBytes());continue;}
                String headers="HTTP/1.1 "+(partial?"206 Partial Content":"200 OK")+"\r\nContent-Type: video/mp4\r\nAccept-Ranges: bytes\r\nContent-Length: "+(end-start+1)+"\r\n"+(partial?"Content-Range: bytes "+start+"-"+end+"/"+data.length+"\r\n":"")+"Connection: close\r\n\r\n";
                output.write(headers.getBytes());output.write(data,start,end-start+1);output.flush();
            }catch(Exception ignored){}
        },"cubox-test-http");serving.setDaemon(true);serving.start();
        JSONObject source=Engine.json("url","http://127.0.0.1:"+server.getLocalPort()+"/fixture.mp4","headers",new JSONArray().put(new JSONArray().put("X-Cubox-Test").put("expected")),"max_height",1080);
        String id="cubox-test-"+System.currentTimeMillis();
        try {
            play(test,activity,source,null,false);
            CountDownLatch complete=new CountDownLatch(1);AtomicReference<String> error=new AtomicReference<>();
            DownloadManager.Listener listener=new DownloadManager.Listener(){@Override public void onDownloadChanged(DownloadManager manager,Download download,Exception finalException){if(!id.equals(download.request.id))return;if(download.state==Download.STATE_COMPLETED)complete.countDown();if(download.state==Download.STATE_FAILED){error.set("Download failed: "+finalException);complete.countDown();}}};
            test.runOnMainSync(()->{
                // The fixture is on loopback; it must not depend on Android's public-internet validation.
                Offline.manager.setRequirements(new androidx.media3.exoplayer.scheduler.Requirements(0));
                Offline.manager.addListener(listener);
                Offline.enqueue(activity,source,"cubox test fixture",id,e->{if(e!=null){error.set(e);complete.countDown();}});
            });
            boolean finished=complete.await(45,TimeUnit.SECONDS);
            test.runOnMainSync(()->Offline.manager.removeListener(listener));
            if(!finished){Download stalled=Offline.manager.getDownloadIndex().getDownload(id);throw new AssertionError("Offline download timed out; state="+(stalled==null?"missing":stalled.state));}
            if(error.get()!=null)throw new AssertionError(error.get());
            Download download=Offline.manager.getDownloadIndex().getDownload(id);
            if(download==null||download.state!=Download.STATE_COMPLETED||download.getBytesDownloaded()!=data.length)throw new AssertionError("Offline bytes do not match fixture");
            server.close(); // Offline playback below cannot use the source server.
            play(test,activity,source,download.request.toMediaItem(),true);
        } finally {
            server.close();
            test.runOnMainSync(()->{Offline.manager.removeDownload(id);Offline.manager.setRequirements(new androidx.media3.exoplayer.scheduler.Requirements(androidx.media3.exoplayer.scheduler.Requirements.NETWORK));});
        }
    }
    private static void play(Instrumentation test,Activity activity,JSONObject source,MediaItem item,boolean offline)throws Exception{
        CountDownLatch ready=new CountDownLatch(1);AtomicReference<String> error=new AtomicReference<>();ExoPlayer[] holder=new ExoPlayer[1];
        test.runOnMainSync(()->{
            PlayerView view=new PlayerView(activity);activity.setContentView(view);
            ExoPlayer player=new ExoPlayer.Builder(activity).setMediaSourceFactory(new DefaultMediaSourceFactory(Offline.playbackFactory(source,offline))).build();holder[0]=player;view.setPlayer(player);
            player.addListener(new Player.Listener(){@Override public void onRenderedFirstFrame(){ready.countDown();}@Override public void onPlayerError(PlaybackException e){error.set(e.getErrorCodeName());ready.countDown();}});
            player.setMediaItem(item==null?Offline.item(source,"Fixture"):item);player.prepare();player.play();
        });
        boolean done=ready.await(30,TimeUnit.SECONDS);
        test.runOnMainSync(()->holder[0].release());
        if(!done)throw new AssertionError((offline?"Offline":"Streaming")+" first frame timed out");
        if(error.get()!=null)throw new AssertionError(error.get());
    }
}
