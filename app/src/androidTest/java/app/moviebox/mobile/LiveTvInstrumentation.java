package app.moviebox.mobile;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public class LiveTvInstrumentation extends Instrumentation {
    public void onCreate(Bundle b){super.onCreate(b);start();}
    void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    TextView find(View view,String text){if(view instanceof TextView&&((TextView)view).getText().toString().equals(text))return (TextView)view;if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++){TextView match=find(g.getChildAt(i),text);if(match!=null)return match;}}return null;}
    public void onStart(){Bundle result=new Bundle();LiveTvStore store=new LiveTvStore(getTargetContext());LiveTvStore.Source custom=null;MainActivity activity=null;PlayerActivity player=null;
        SharedPreferences prefs=getTargetContext().getSharedPreferences("live-tv",0);Map<String,?> prior=prefs.getAll();
        try{
            String[] paths={"countries/bd","countries/in","categories/sports","categories/kids","categories/movies","categories/music","categories/news"};
            for(int i=0;i<paths.length;i++){
                String category=LiveTvStore.CATEGORIES.get(i);
                List<LiveTvStore.Source> sources=store.sources(category);
                check(sources.get(0).url.endsWith(paths[i]+".m3u"),"Wrong playlist for "+category);
                for(LiveTvStore.Source source:sources)if(source.id.startsWith("official-"))check(!store.cached(source).channels.isEmpty(),"Empty official source: "+category);
            }
            // Exercise a redirect plus relative media paths with real HTTP, then read only from disk.
            try(ServerSocket server=new ServerSocket(0)){
                Thread fixture=new Thread(()->{for(int i=0;i<2;i++)try(Socket s=server.accept()){
                    BufferedReader in=new BufferedReader(new InputStreamReader(s.getInputStream(),StandardCharsets.US_ASCII));String first=in.readLine(),line;while((line=in.readLine())!=null&&!line.isEmpty()){}
                    String body="#EXTM3U\n#EXTINF:-1 group-title=\"Test\",Fixture TV\nlive.m3u8\n";
                    String response=first.contains("/start ")?"HTTP/1.1 302 Found\r\nLocation: /folder/list.m3u\r\nContent-Length: 0\r\nConnection: close\r\n\r\n":"HTTP/1.1 200 OK\r\nContent-Length: "+body.getBytes(StandardCharsets.UTF_8).length+"\r\nConnection: close\r\n\r\n"+body;
                    s.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
                }catch(Exception e){throw new RuntimeException(e);}});fixture.setDaemon(true);fixture.start();
                String origin="http://127.0.0.1:"+server.getLocalPort();custom=new LiveTvStore.Source(UUID.randomUUID().toString(),"Test playlist",origin+"/start","BD",false);
                LiveTvStore.Fetched fetched=LiveTvStore.fetch(custom.url);store.add(custom,fetched.text,fetched.base);
                check(store.cached(custom).channels.get(0).url.equals(origin+"/folder/live.m3u8"),"Relative URL after redirect");fixture.join(5000);
            }
            LiveTvStore.Listing cached=store.cached(custom);check(cached.channels.size()==1,"Cache reload");
            store.favorite(cached.channels.get(0));check(store.favorites().contains(LiveTvStore.hash(cached.channels.get(0).url)),"Favorite persistence");
            store.remove(custom);check(store.cached(custom)==null,"Remove cached playlist");custom=null;
            prefs.edit().putString("category","BD").putString("source-BD","official-BD").putString("source-India","official-India").putString("source-Sports","official-Sports").commit();
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("live",true));
            final MainActivity a=activity;
            waitForIdleSync();Thread.sleep(1500);
            runOnMainSync(()->{check(find(a.nav,"Live TV")!=null,"Live tab missing");for(String category:LiveTvStore.CATEGORIES)check(find(a.body,category)!=null,"Missing category: "+category);check(find(a.body,"+ Add new")!=null,"Add new missing");find(a.body,"News").performClick();check(a.liveTv.source.id.equals("iptv-News"),"News playlist selection");check(new LiveTv(a).category.equals("News"),"New category did not persist");find(a.body,"India").performClick();});
            Thread.sleep(1000);waitForIdleSync();check(a.liveTv.source.id.equals("official-India"),"India selection");
            runOnMainSync(()->{a.tvShell("live");TvSupport.focus(a.root);TextView live=find(a.nav,"Live TV");check(live!=null&&live.isFocusable(),"TV navigation focus");check(TvSupport.activate(live,KeyEvent.KEYCODE_DPAD_CENTER,new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER)),"Remote OK not handled");});
            check(a.screen.equals("live"),"Remote failed to open Live TV");
            // Live mode must not restore the movie resume timestamp or the saved playback speed.
            File video=new File(getTargetContext().getCacheDir(),"live-fixture.mp4");try(InputStream in=getContext().getAssets().open("test-video.mp4");FileOutputStream out=new FileOutputStream(video)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
            getTargetContext().getSharedPreferences("resume",0).edit().putLong("live-test",90000).commit();
            player=(PlayerActivity)startActivitySync(new Intent(getTargetContext(),PlayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("source",new JSONObject().put("url",video.toURI().toString()).toString()).putExtra("key","live-test").putExtra("title","Live fixture").putExtra("live",true));
            final PlayerActivity p=player;Thread.sleep(1500);runOnMainSync(()->{check(p.live&&p.speed==1f&&find(p.toolbar,"LIVE")!=null,"Live controls missing");check(p.player!=null&&p.player.getCurrentPosition()<15000,"Stale movie resume applied to live mode");p.finish();a.finish();});
            getTargetContext().getSharedPreferences("resume",0).edit().remove("live-test").commit();video.delete();
            result.putString("stream","PASS: seven category mappings and controls, nonempty official sources, category persistence, redirected playlist import, cache, favorites/removal, TV remote activation and live-mode player controls/resume behavior.");finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}
        finally{try{if(custom!=null)store.remove(custom);}catch(Exception ignored){}
            SharedPreferences.Editor edit=prefs.edit().clear();for(Map.Entry<String,?> entry:prior.entrySet()){Object value=entry.getValue();if(value instanceof String)edit.putString(entry.getKey(),(String)value);else if(value instanceof Set)edit.putStringSet(entry.getKey(),(Set<String>)value);}edit.commit();}
    }
}
