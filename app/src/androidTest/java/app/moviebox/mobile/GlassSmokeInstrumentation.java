package app.moviebox.mobile;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import java.io.*;
import org.json.*;

/** Repeatable visual checks, using a local video fixture instead of a provider. */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public class GlassSmokeInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    void capture(String name)throws Exception{
        waitForIdleSync();Thread.sleep(700);
        Bitmap bitmap=getUiAutomation().takeScreenshot();
        if(bitmap==null)throw new AssertionError("Screenshot unavailable");
        try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}
        bitmap.recycle();
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        MainActivity main=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("downloads",true));
        runOnMainSync(main::showAbout);capture("glass-phone");
        final android.app.AlertDialog[] dialog=new android.app.AlertDialog[1];
        runOnMainSync(()->dialog[0]=new FrostedDialog(main).setTitle("Choose a server / quality").setItems(new String[]{"1080p · Original audio", "720p · Smaller download"},(d,n)->{}).setNegativeButton("Cancel",null).show());capture("glass-dialog");runOnMainSync(()->dialog[0].dismiss());
        runOnMainSync(()->{main.tvShell("downloads");main.renderDownloads();TvSupport.focus(main.root);});capture("glass-tv");
        File video=new File(getTargetContext().getFilesDir(),"glass-fixture.mp4");
        try(InputStream in=getContext().getAssets().open("test-video.mp4");FileOutputStream out=new FileOutputStream(video)){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);}
        JSONObject source=new JSONObject().put("url",video.toURI().toString()).put("headers",new JSONArray());
        PlayerActivity player=(PlayerActivity)startActivitySync(new Intent(getTargetContext(),PlayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("source",source.toString()).putExtra("title","Cubox cinema · Player preview").putExtra("key","glass-test"));
        Thread.sleep(1800);
        runOnMainSync(()->{player.player.pause();player.view.showController();if(player.toolbar.getBackground()==null)throw new AssertionError("Missing glass toolbar");});capture("glass-player");
        runOnMainSync(()->{player.view.hideController();});capture("glass-player-hidden");
        runOnMainSync(()->{player.finish();main.finish();});video.delete();
        result.putString("stream","PASS: phone and TV glass screens render; local player opens; controls show/hide. Screenshots saved in app external files.");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
