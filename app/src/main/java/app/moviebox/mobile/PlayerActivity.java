package app.moviebox.mobile;

import android.app.*;
import android.content.pm.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Rational;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import org.json.*;
import java.util.*;

@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public class PlayerActivity extends Activity {
    ExoPlayer player; PlayerView view; JSONObject source; String key; long position;
    boolean playWhenReady=true,locked=false; float speed=1f,subtitleSize=.0533f;
    int fit=AspectRatioFrameLayout.RESIZE_MODE_FIT,repeat=Player.REPEAT_MODE_OFF;
    int orientation=ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;
    TrackSelectionParameters selection; FrameLayout root; LinearLayout toolbar; TextView unlock;
    final android.os.Handler gestures=new android.os.Handler(android.os.Looper.getMainLooper());
    final TapSeek tapSeek=new TapSeek(); TextView gestureHud; android.media.AudioManager audio;
    float downX,downY,startingBrightness;int startingVolume;boolean moved,swiping;
    final Runnable singleTap=()->{if(view!=null&&!locked)view.performClick();};
    final Runnable hideGesture=()->{if(gestureHud!=null)gestureHud.animate().alpha(0f).setDuration(220).withEndAction(()->gestureHud.setVisibility(View.GONE)).start();};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);orientation=getSharedPreferences("player",0).getInt("orientation",ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);setRequestedOrientation(orientation);getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);immersive();
        try{source=new JSONObject(getIntent().getStringExtra("source"));}catch(Exception e){finish();return;}
        key=getIntent().getStringExtra("key");if(key==null)key=source.optString("url");
        position=state==null?getSharedPreferences("resume",0).getLong(key,0):state.getLong("position");
        speed=getSharedPreferences("player",0).getFloat("speed",1f);fit=getSharedPreferences("player",0).getInt("fit",AspectRatioFrameLayout.RESIZE_MODE_FIT);subtitleSize=getSharedPreferences("player",0).getFloat("subtitleSize",.0533f);
        if(state!=null){playWhenReady=state.getBoolean("playing",true);locked=state.getBoolean("locked",false);}
        root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);
        view=new PlayerView(this);view.setBackgroundColor(Color.BLACK);view.setShutterBackgroundColor(Color.BLACK);
        view.setUseArtwork(false);view.setKeepContentOnPlayerReset(false);view.setResizeMode(fit);
        view.setShowSubtitleButton(true);view.setShowNextButton(false);view.setShowPreviousButton(false);
        view.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);view.setControllerShowTimeoutMs(4000);
        // All unused video space and the loading/reset shutter are explicitly pure black.
        View content=view.findViewById(androidx.media3.ui.R.id.exo_content_frame);if(content!=null)content.setBackgroundColor(Color.BLACK);
        if(view.getSubtitleView()!=null)view.getSubtitleView().setFractionalTextSize(subtitleSize);
        root.addView(view,new FrameLayout.LayoutParams(-1,-1));
        toolbar=new LinearLayout(this);toolbar.setGravity(Gravity.CENTER_VERTICAL);toolbar.setPadding(dp(8),dp(6),dp(8),dp(6));toolbar.setBackgroundColor(0xb8000000);
        toolbar.addView(control("‹","Back to title",this::finish),new LinearLayout.LayoutParams(dp(44),dp(44)));
        TextView title=new TextView(this);title.setText(getIntent().getStringExtra("title"));title.setTextSize(14);title.setTextColor(Color.WHITE);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setMaxLines(1);title.setEllipsize(android.text.TextUtils.TruncateAt.END);toolbar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        toolbar.addView(control("Fit","Change video size",this::chooseFit),new LinearLayout.LayoutParams(dp(48),dp(44)));
        toolbar.addView(control("⋮","Player settings",this::settings),new LinearLayout.LayoutParams(dp(44),dp(44)));
        root.addView(toolbar,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
        unlock=control("Unlock","Unlock player controls",()->setLocked(false));unlock.setBackgroundColor(0xcc202020);FrameLayout.LayoutParams up=new FrameLayout.LayoutParams(dp(82),dp(44),Gravity.TOP|Gravity.END);up.setMargins(0,dp(8),dp(12),0);root.addView(unlock,up);
        view.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener)visibility->{if(!locked&&!isInPictureInPictureMode())toolbar.setVisibility(visibility);});
        gestureHud=new TextView(this);gestureHud.setGravity(Gravity.CENTER);gestureHud.setTextColor(Color.WHITE);gestureHud.setTextSize(21);gestureHud.setTypeface(Typeface.DEFAULT,Typeface.BOLD);gestureHud.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);android.graphics.drawable.GradientDrawable bubble=new android.graphics.drawable.GradientDrawable();bubble.setColor(0xb8202020);bubble.setCornerRadius(dp(80));gestureHud.setBackground(bubble);gestureHud.setVisibility(View.GONE);root.addView(gestureHud,new FrameLayout.LayoutParams(dp(150),dp(150),Gravity.CENTER));
        audio=(android.media.AudioManager)getSystemService(AUDIO_SERVICE);installGestures();setContentView(root);setLocked(locked);
        root.setOnApplyWindowInsetsListener((v,insets)->{toolbar.setPadding(Math.max(dp(8),insets.getSystemWindowInsetLeft()),Math.max(dp(6),insets.getSystemWindowInsetTop()),Math.max(dp(8),insets.getSystemWindowInsetRight()),dp(6));return insets;});
    }
    int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    TextView control(String label,String description,Runnable action){TextView b=new TextView(this);b.setText(label);b.setTextSize(16);b.setTextColor(Color.WHITE);b.setGravity(Gravity.CENTER);b.setContentDescription(description);b.setOnClickListener(v->action.run());return b;}
    void immersive(){getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)immersive();}
    @Override protected void onStart(){super.onStart();
        boolean offline=getIntent().getBooleanExtra("offline",false);
        player=new ExoPlayer.Builder(this).setSeekBackIncrementMs(10000).setSeekForwardIncrementMs(10000).setHandleAudioBecomingNoisy(true).setMediaSourceFactory(new DefaultMediaSourceFactory(Offline.playbackFactory(source,offline))).build();
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),true);
        view.setPlayer(player);player.setPlaybackSpeed(speed);player.setRepeatMode(repeat);
        int height=source.optInt("max_height",0);if(selection!=null)player.setTrackSelectionParameters(selection);else if(height>0)player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setMaxVideoSize(Integer.MAX_VALUE,height).build());
        MediaItem item=Offline.item(source,getIntent().getStringExtra("title"));String downloadId=getIntent().getStringExtra("downloadId");
        if(downloadId!=null)try{androidx.media3.exoplayer.offline.Download d=Offline.manager.getDownloadIndex().getDownload(downloadId);if(d!=null)item=d.request.toMediaItem();}catch(Exception ignored){}
        player.addListener(new Player.Listener(){
            @Override public void onPlaybackStateChanged(int state){if(state==Player.STATE_ENDED){position=0;getSharedPreferences("resume",0).edit().remove(key).apply();}}
            @Override public void onPlayerError(PlaybackException e){if(isFinishing())return;new AlertDialog.Builder(PlayerActivity.this).setTitle("Playback unavailable").setMessage(e.getErrorCodeName()+"\nTry another server or quality from the title page.").setPositiveButton("Retry",(d,w)->{if(player!=null){player.prepare();player.play();}}).setNegativeButton("Back",(d,w)->finish()).show();}
        });player.setMediaItem(item);player.seekTo(position);player.prepare();player.setPlayWhenReady(playWhenReady);
    }
    void settings(){if(player==null)return;String[] options={"Playback speed · "+speed+"×","Audio track","Subtitles","Subtitle size","Video size / aspect ratio","Screen rotation","Lock controls","Picture-in-picture",repeat==Player.REPEAT_MODE_ONE?"Repeat · On":"Repeat · Off","Gesture guide"};new AlertDialog.Builder(this).setTitle("Player controls").setItems(options,(d,n)->{switch(n){case 0->chooseSpeed();case 1->chooseTrack(C.TRACK_TYPE_AUDIO);case 2->chooseTrack(C.TRACK_TYPE_TEXT);case 3->chooseSubtitleSize();case 4->chooseFit();case 5->chooseOrientation();case 6->setLocked(true);case 7->miniPlayer();case 8->{repeat=repeat==Player.REPEAT_MODE_ONE?Player.REPEAT_MODE_OFF:Player.REPEAT_MODE_ONE;player.setRepeatMode(repeat);}case 9->new AlertDialog.Builder(this).setTitle("Playback gestures").setMessage("Tap the left/right side twice for 10 seconds, three times for 20, four times for 30, and keep tapping for more.\n\nSwipe up/down on the left for brightness or on the right for volume.\n\nTap once to show controls. Lock also freezes rotation; tap Unlock or press Back to unlock.").setPositiveButton("Got it",null).show();}}).show();}
    void chooseSpeed(){if(player==null)return;float[] speeds={.25f,.5f,.75f,1f,1.25f,1.5f,1.75f,2f};String[] labels=new String[speeds.length];int selected=3;for(int i=0;i<speeds.length;i++){labels[i]=speeds[i]+"×";if(speeds[i]==speed)selected=i;}new AlertDialog.Builder(this).setTitle("Playback speed").setSingleChoiceItems(labels,selected,(d,n)->{speed=speeds[n];if(player!=null)player.setPlaybackSpeed(speed);getSharedPreferences("player",0).edit().putFloat("speed",speed).apply();d.dismiss();}).show();}
    void chooseFit(){String[] labels={"Fit · black bars, no cropping","Fill · stretch to screen","Zoom · crop to fill"};int[] modes={AspectRatioFrameLayout.RESIZE_MODE_FIT,AspectRatioFrameLayout.RESIZE_MODE_FILL,AspectRatioFrameLayout.RESIZE_MODE_ZOOM};int selected=fit==modes[1]?1:fit==modes[2]?2:0;new AlertDialog.Builder(this).setTitle("Video size").setSingleChoiceItems(labels,selected,(d,n)->{fit=modes[n];view.setResizeMode(fit);getSharedPreferences("player",0).edit().putInt("fit",fit).apply();d.dismiss();}).show();}
    void chooseSubtitleSize(){String[] labels={"Small","Medium","Large"};float[] sizes={.04f,.0533f,.07f};int selected=subtitleSize==sizes[0]?0:subtitleSize==sizes[2]?2:1;new AlertDialog.Builder(this).setTitle("Subtitle size").setSingleChoiceItems(labels,selected,(d,n)->{subtitleSize=sizes[n];if(view.getSubtitleView()!=null)view.getSubtitleView().setFractionalTextSize(subtitleSize);getSharedPreferences("player",0).edit().putFloat("subtitleSize",subtitleSize).apply();d.dismiss();}).show();}
    void chooseTrack(int type){if(player==null)return;List<String> labels=new ArrayList<>();List<TrackSelectionOverride> overrides=new ArrayList<>();labels.add(type==C.TRACK_TYPE_TEXT?"Off":"Automatic");overrides.add(null);int selected=0;
        for(Tracks.Group group:player.getCurrentTracks().getGroups()){if(group.getType()!=type)continue;for(int i=0;i<group.length;i++){if(!group.isTrackSupported(i))continue;Format format=group.getTrackFormat(i);String language=format.language==null?"":Locale.forLanguageTag(format.language).getDisplayLanguage();String label=format.label!=null?format.label:!language.isEmpty()?language:(type==C.TRACK_TYPE_AUDIO?"Audio":"Subtitle")+" "+labels.size();if(type==C.TRACK_TYPE_AUDIO&&format.channelCount>0)label+=" · "+format.channelCount+" ch";labels.add(label);overrides.add(new TrackSelectionOverride(group.getMediaTrackGroup(),i));if(group.isTrackSelected(i))selected=labels.size()-1;}}
        if(labels.size()==1){Toast.makeText(this,type==C.TRACK_TYPE_AUDIO?"No selectable audio tracks yet":"This stream has no available subtitle tracks",Toast.LENGTH_LONG).show();return;}
        new AlertDialog.Builder(this).setTitle(type==C.TRACK_TYPE_AUDIO?"Audio track":"Subtitles").setSingleChoiceItems(labels.toArray(new String[0]),selected,(d,n)->{if(player!=null){TrackSelectionParameters.Builder b=player.getTrackSelectionParameters().buildUpon().clearOverridesOfType(type).setTrackTypeDisabled(type,type==C.TRACK_TYPE_TEXT&&n==0);if(overrides.get(n)!=null)b.setOverrideForType(overrides.get(n));player.setTrackSelectionParameters(b.build());}d.dismiss();}).show();
    }
    void chooseOrientation(){String[] labels={"Auto · follow phone rotation","Portrait","Landscape"};int[] modes={ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR,ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT,ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE};int selected=orientation==modes[1]?1:orientation==modes[2]?2:0;new AlertDialog.Builder(this).setTitle("Screen rotation").setSingleChoiceItems(labels,selected,(d,n)->{setOrientationMode(modes[n]);d.dismiss();}).show();}
    void setOrientationMode(int mode){orientation=mode;getSharedPreferences("player",0).edit().putInt("orientation",mode).apply();if(!locked)setRequestedOrientation(mode);}
    void setLocked(boolean value){gestures.removeCallbacks(singleTap);tapSeek.reset();locked=value;setRequestedOrientation(value?ActivityInfo.SCREEN_ORIENTATION_LOCKED:orientation);unlock.setVisibility(value?View.VISIBLE:View.GONE);view.setUseController(!value);toolbar.setVisibility(value?View.GONE:View.VISIBLE);if(!value)view.showController();}
    void miniPlayer(){if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)){Toast.makeText(this,"Picture-in-picture is not supported on this device",Toast.LENGTH_LONG).show();return;}VideoSize video=player==null?VideoSize.UNKNOWN:player.getVideoSize();int w=video.width>0?video.width:16,h=video.height>0?video.height:9;double ratio=(double)w/h;if(ratio<.42||ratio>2.39){w=16;h=9;}try{if(!enterPictureInPictureMode(new PictureInPictureParams.Builder().setAspectRatio(new Rational(w,h)).build()))Toast.makeText(this,"Enable picture-in-picture for cubox & chill in Android settings",Toast.LENGTH_LONG).show();}catch(IllegalStateException|IllegalArgumentException e){Toast.makeText(this,"Picture-in-picture is unavailable right now",Toast.LENGTH_LONG).show();}}
    @Override public void onPictureInPictureModeChanged(boolean pip,Configuration config){super.onPictureInPictureModeChanged(pip,config);view.setUseController(!pip&&!locked);toolbar.setVisibility(pip||locked?View.GONE:View.VISIBLE);unlock.setVisibility(!pip&&locked?View.VISIBLE:View.GONE);}
    float brightness(){float b=getWindow().getAttributes().screenBrightness;if(b>=0)return b;try{return android.provider.Settings.System.getInt(getContentResolver(),android.provider.Settings.System.SCREEN_BRIGHTNESS)/255f;}catch(Exception e){return .5f;}}
    void setBrightness(float value){WindowManager.LayoutParams p=getWindow().getAttributes();p.screenBrightness=Math.max(.02f,Math.min(1f,value));getWindow().setAttributes(p);showGesture("☀\n"+Math.round(p.screenBrightness*100)+"%",0);}
    void setVolume(int value){int max=audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);value=Math.max(0,Math.min(max,value));try{audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC,value,0);}catch(SecurityException ignored){}showGesture("Volume\n"+Math.round(100f*audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)/Math.max(1,max))+"%",0);}
    void showGesture(String label,int side){gestures.removeCallbacks(hideGesture);gestureHud.animate().cancel();FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)gestureHud.getLayoutParams();p.gravity=Gravity.CENTER_VERTICAL|(side<0?Gravity.LEFT:side>0?Gravity.RIGHT:Gravity.CENTER_HORIZONTAL);p.setMargins(dp(24),0,dp(24),0);gestureHud.setLayoutParams(p);gestureHud.setText(label);gestureHud.setVisibility(View.VISIBLE);gestureHud.setAlpha(1f);gestureHud.setScaleX(.88f);gestureHud.setScaleY(.88f);gestureHud.animate().scaleX(1f).scaleY(1f).setDuration(160).start();gestures.postDelayed(hideGesture,750);}
    void tapVideo(boolean forward,long time){gestures.removeCallbacks(singleTap);int seconds=tapSeek.tap(forward,time);if(seconds==0){gestures.postDelayed(singleTap,TapSeek.WINDOW_MS);return;}if(player!=null){if(forward)player.seekForward();else player.seekBack();view.hideController();showGesture((forward?"❯❯":"❮❮")+"\n"+(forward?"+":"−")+seconds+" seconds",forward?1:-1);}}
    void installGestures(){view.setClickable(true);view.setOnTouchListener((v,e)->{
        if(locked)return true;
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN->{downX=e.getX();downY=e.getY();startingBrightness=brightness();startingVolume=audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);moved=false;swiping=false;return true;}
            case MotionEvent.ACTION_MOVE->{float dx=e.getX()-downX,dy=downY-e.getY();if(!moved&&Math.max(Math.abs(dx),Math.abs(dy))>dp(12)){moved=true;swiping=Math.abs(dy)>Math.abs(dx);gestures.removeCallbacks(singleTap);tapSeek.reset();}if(swiping){float change=dy/Math.max(1,view.getHeight()*.65f);if(downX<view.getWidth()/2f)setBrightness(startingBrightness+change);else setVolume(startingVolume+Math.round(change*audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)));}return true;}
            case MotionEvent.ACTION_UP->{if(!moved&&e.getEventTime()-e.getDownTime()<500)tapVideo(e.getX()>view.getWidth()/2f,e.getEventTime());return true;}
            case MotionEvent.ACTION_CANCEL->{gestures.removeCallbacks(singleTap);tapSeek.reset();return true;}
            default->{return true;}
        }
    });}
    @Override protected void onDestroy(){gestures.removeCallbacksAndMessages(null);super.onDestroy();}
    @Override public void onBackPressed(){if(locked)setLocked(false);else super.onBackPressed();}
    @Override protected void onStop(){if(player!=null){position=player.getPlaybackState()==Player.STATE_ENDED?0:player.getCurrentPosition();playWhenReady=player.getPlayWhenReady();selection=player.getTrackSelectionParameters();getSharedPreferences("resume",0).edit().putLong(key,position).apply();view.setPlayer(null);player.release();player=null;}super.onStop();}
    @Override protected void onSaveInstanceState(Bundle b){b.putLong("position",player==null?position:player.getCurrentPosition());b.putBoolean("playing",player==null?playWhenReady:player.getPlayWhenReady());b.putBoolean("locked",locked);super.onSaveInstanceState(b);}
}
