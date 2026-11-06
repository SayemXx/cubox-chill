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
            verifyPlayerScreen(test,activity,source);
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
    private static void verifyPlayerScreen(Instrumentation test,Activity parent,JSONObject source)throws Exception {
        PlayerActivity screen=(PlayerActivity)test.startActivitySync(new android.content.Intent(parent,PlayerActivity.class)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("source",source.toString())
                .putExtra("title","Player verification").putExtra("key","player-ui-test"));
        CountDownLatch frame=new CountDownLatch(1);AtomicReference<String> error=new AtomicReference<>();
        test.runOnMainSync(()->{
            screen.player.addListener(new Player.Listener(){@Override public void onRenderedFirstFrame(){frame.countDown();}@Override public void onPlayerError(PlaybackException e){error.set(e.getErrorCodeName());frame.countDown();}});
            screen.view.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
            screen.player.seekTo(0);screen.player.play();
        });
        try {
            if(!frame.await(30,TimeUnit.SECONDS))throw new AssertionError("Player screen did not render a frame");
            if(error.get()!=null)throw new AssertionError(error.get());
            test.runOnMainSync(()->{
                screen.view.setControllerAutoShow(false);
                ((androidx.media3.ui.PlayerControlView)screen.view.findViewById(androidx.media3.ui.R.id.exo_controller)).setAnimationEnabled(false);
                screen.player.pause();screen.player.setPlaybackSpeed(1.5f);
                if(screen.player.getPlaybackParameters().speed!=1.5f)throw new AssertionError("Playback speed was not applied");
                if(screen.getRequestedOrientation()!=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR)throw new AssertionError("Player does not default to automatic rotation");
                screen.setLocked(true);
                if(screen.view.getUseController()||screen.unlock.getVisibility()!=android.view.View.VISIBLE)throw new AssertionError("Lock controls failed");
                if(screen.getRequestedOrientation()!=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LOCKED)throw new AssertionError("Lock must freeze orientation");
                screen.setLocked(false);
                if(!screen.view.getUseController())throw new AssertionError("Unlock controls failed");
                if(screen.getRequestedOrientation()!=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR)throw new AssertionError("Unlock must restore automatic rotation");
                screen.view.hideController();
            });
            test.waitForIdleSync();Thread.sleep(700);
            test.runOnMainSync(screen.singleTap);Thread.sleep(500);
            test.runOnMainSync(()->{if(!screen.view.isControllerFullyVisible())throw new AssertionError("Single tap did not show controls");screen.view.hideController();});Thread.sleep(500);
            // Dismiss Android's first-use fullscreen education, which is outside the app's window.
            android.view.accessibility.AccessibilityNodeInfo active=test.getUiAutomation().getRootInActiveWindow();
            if(active!=null){
                for(android.view.accessibility.AccessibilityNodeInfo node:active.findAccessibilityNodeInfosByText("Got it")){
                    if("com.android.systemui".contentEquals(node.getPackageName()))node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
            Thread.sleep(700);test.runOnMainSync(()->screen.view.hideController());test.waitForIdleSync();Thread.sleep(300);
            android.graphics.Bitmap screenshot=test.getUiAutomation().takeScreenshot();
            if(screenshot==null)throw new AssertionError("Player screenshot unavailable");
            try(FileOutputStream file=new FileOutputStream(new File(parent.getExternalFilesDir(null),"player-black-bars.png"))){screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,file);}
            int pixel=screenshot.getPixel(screenshot.getWidth()/2,screenshot.getHeight()/8);
            if(android.graphics.Color.red(pixel)>5||android.graphics.Color.green(pixel)>5||android.graphics.Color.blue(pixel)>5)throw new AssertionError("Unused player area is not black: "+Integer.toHexString(pixel));
            screenshot.recycle();
            ExoPlayer original=screen.player;long[] before={0};test.runOnMainSync(()->before[0]=original.getCurrentPosition());
            try {
                // Full-sensor mode intentionally ignores the emulator's user-rotation lock.
                // Exercise the same orientation change through the player's landscape option.
                test.runOnMainSync(()->screen.setOrientationMode(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE));
                Thread.sleep(2500);test.waitForIdleSync();
                test.runOnMainSync(()->{
                    if(screen.getResources().getConfiguration().orientation!=android.content.res.Configuration.ORIENTATION_LANDSCAPE)throw new AssertionError("Player did not rotate to landscape");
                    if(screen.player!=original||Math.abs(screen.player.getCurrentPosition()-before[0])>500)throw new AssertionError("Rotation restarted playback");
                    screen.view.hideController();screen.setBrightness(.2f);
                    swipe(screen.view,.25f);
                    if(screen.brightness()<=.2f)throw new AssertionError("Left swipe did not increase brightness");
                    int previous=screen.audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);
                    int max=screen.audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
                    try{screen.setVolume(max/4);swipe(screen.view,.75f);if(screen.audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)<=max/4)throw new AssertionError("Right swipe did not increase volume");}
                    finally{screen.audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC,previous,0);}
                    screen.player.seekTo(0);horizontalSwipe(screen.view,.3f,.6f,false);
                    if(screen.player.getCurrentPosition()<=0)throw new AssertionError("Right slide did not seek forward");
                    long afterForward=screen.player.getCurrentPosition();horizontalSwipe(screen.view,.6f,.3f,true);
                    if(screen.player.getCurrentPosition()!=afterForward)throw new AssertionError("Cancelled slide changed position");
                    horizontalSwipe(screen.view,.6f,.3f,false);
                    if(screen.player.getCurrentPosition()>=afterForward)throw new AssertionError("Left slide did not seek backward");
                    long t=android.os.SystemClock.uptimeMillis();screen.tapVideo(true,t);screen.tapVideo(true,t+80);screen.tapVideo(true,t+160);screen.tapVideo(true,t+240);
                    if(!screen.gestureHud.getText().toString().contains("30 seconds"))throw new AssertionError("Repeated tap feedback incorrect");
                });
            } finally {test.runOnMainSync(()->screen.setOrientationMode(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR));}
        } finally {test.runOnMainSync(screen::finish);test.waitForIdleSync();}
    }
    private static void swipe(android.view.View view,float side){
        long time=android.os.SystemClock.uptimeMillis();float x=view.getWidth()*side;
        int[] actions={android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_MOVE,android.view.MotionEvent.ACTION_UP};
        for(int i=0;i<actions.length;i++){android.view.MotionEvent event=android.view.MotionEvent.obtain(time,time+i*40,actions[i],x,view.getHeight()*(i==0?.8f:.3f),0);view.dispatchTouchEvent(event);event.recycle();}
    }
    private static void horizontalSwipe(android.view.View view,float from,float to,boolean cancel){
        long time=android.os.SystemClock.uptimeMillis();int[] actions={android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_MOVE,cancel?android.view.MotionEvent.ACTION_CANCEL:android.view.MotionEvent.ACTION_UP};
        for(int i=0;i<actions.length;i++){android.view.MotionEvent event=android.view.MotionEvent.obtain(time,time+i*50,actions[i],view.getWidth()*(i==0?from:to),view.getHeight()*.35f,0);view.dispatchTouchEvent(event);event.recycle();}
    }
    static void play(Instrumentation test,Activity activity,JSONObject source,MediaItem item,boolean offline)throws Exception{
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
