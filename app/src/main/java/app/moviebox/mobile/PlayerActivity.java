package app.moviebox.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import org.json.*;

@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public class PlayerActivity extends Activity {
    ExoPlayer player; PlayerView view; JSONObject source; String key; long position; boolean playWhenReady=true;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        try {source=new JSONObject(getIntent().getStringExtra("source"));}catch(Exception e){finish();return;}
        key=getIntent().getStringExtra("key"); if(key==null)key=source.optString("url");
        position=state==null?getSharedPreferences("resume",0).getLong(key,0):state.getLong("position");
        if(state!=null)playWhenReady=state.getBoolean("playing",true);
        view=new PlayerView(this);view.setShowSubtitleButton(true);setContentView(view);
    }
    @Override protected void onStart(){super.onStart();
        boolean offline=getIntent().getBooleanExtra("offline",false);
        player=new ExoPlayer.Builder(this).setMediaSourceFactory(new DefaultMediaSourceFactory(Offline.playbackFactory(source,offline))).build();
        view.setPlayer(player);
        int height=source.optInt("max_height",0);
        if(height>0)player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setMaxVideoSize(Integer.MAX_VALUE,height).build());
        MediaItem item=Offline.item(source,getIntent().getStringExtra("title"));
        String downloadId=getIntent().getStringExtra("downloadId");
        if(downloadId!=null)try{androidx.media3.exoplayer.offline.Download d=Offline.manager.getDownloadIndex().getDownload(downloadId);if(d!=null)item=d.request.toMediaItem();}catch(Exception ignored){}
        player.setMediaItem(item);player.seekTo(position);player.prepare();player.setPlayWhenReady(playWhenReady);
        player.addListener(new Player.Listener(){public void onPlayerError(PlaybackException e){
            new AlertDialog.Builder(PlayerActivity.this).setTitle("Playback unavailable").setMessage(e.getErrorCodeName()+"\nTry another server or quality from the title page.").setPositiveButton("Retry",(d,w)->{player.prepare();player.play();}).setNegativeButton("Back",(d,w)->finish()).show();
        }});
    }
    @Override protected void onStop(){if(player!=null){position=player.getCurrentPosition();playWhenReady=player.getPlayWhenReady();getSharedPreferences("resume",0).edit().putLong(key,position).apply();view.setPlayer(null);player.release();player=null;}super.onStop();}
    @Override protected void onSaveInstanceState(Bundle b){b.putLong("position",player==null?position:player.getCurrentPosition());b.putBoolean("playing",player==null?playWhenReady:player.getPlayWhenReady());super.onSaveInstanceState(b);}
}
