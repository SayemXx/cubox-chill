package app.moviebox.mobile;

import android.app.Instrumentation;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.offline.*;
import org.json.*;
import java.util.concurrent.*;

/** Device smoke tests with explicit results; no credentials or provider media downloads. */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public class SmokeInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle out=new Bundle();
        try{
            Context context=getTargetContext();
            boolean rejected=false;
            try{Engine.request(context,Engine.json("op","invalid_operation"));}
            catch(Exception e){rejected=e.getMessage().contains("Unknown engine request");}
            if(!rejected)throw new AssertionError("JNI engine failed invalid-operation contract");
            out.putString("native_bridge","PASS: library loads; runtime initializes; errors cross JNI");
            Activity activity=startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("downloads",true));
            waitForIdleSync();
            if(activity==null)throw new AssertionError("Main activity did not start");
            out.putString("activity","PASS: downloads screen opens");
            CountDownLatch playerDone=new CountDownLatch(1);
            runOnMainSync(()->{ExoPlayer player=new ExoPlayer.Builder(context).build();player.release();playerDone.countDown();});
            if(!playerDone.await(10,TimeUnit.SECONDS))throw new AssertionError("Player initialization timeout");
            out.putString("player","PASS: ExoPlayer initializes and releases");
            runOnMainSync(()->{if(Offline.manager==null||Offline.cache==null)throw new AssertionError("Offline manager missing");});
            out.putString("offline","PASS: persistent download database/cache initialize");
            MediaSmoke.run(this,activity);
            out.putString("media_pipeline","PASS: authenticated video renders; full download completes; cached video renders with server closed");
            try{
                Object data=Engine.request(context,Engine.json("op","search","provider","moviebox","query","Big Buck Bunny","page",1));
                if(!(data instanceof JSONArray))throw new AssertionError("Search returned wrong type");
                out.putString("live_search","PASS: provider returned "+((JSONArray)data).length()+" results");
            }catch(Exception e){out.putString("live_search","UNAVAILABLE: "+e.getMessage());}
            runOnMainSync(activity::finish);
            out.putString("stream","cubox chill smoke checks completed\n"+out);
            finish(Activity.RESULT_OK,out);
        }catch(Throwable error){out.putString("stream","FAIL: "+error);finish(Activity.RESULT_CANCELED,out);}
    }
}
