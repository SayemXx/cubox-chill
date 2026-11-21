package app.moviebox.mobile;

import android.content.Context;
import android.net.Uri;
import androidx.media3.common.*;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.*;
import androidx.media3.datasource.cache.*;
import androidx.media3.exoplayer.offline.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.nio.charset.StandardCharsets;

@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
final class Offline {
    static DownloadManager manager;
    static SimpleCache cache;
    static Context context;
    static synchronized void initialize(Context c) {
        if (manager != null) return;
        context=c.getApplicationContext();
        StandaloneDatabaseProvider db=new StandaloneDatabaseProvider(context);
        cache=new SimpleCache(new File(context.getFilesDir(),"offline-media"),new NoOpCacheEvictor(),db);
        java.util.concurrent.Executor executor=Executors.newFixedThreadPool(4);
        DownloaderFactory factory=request -> {
            JSONObject source;
            try { source=new JSONObject(new String(request.data,StandardCharsets.UTF_8)).getJSONObject("source"); }
            catch(JSONException e) { source=new JSONObject(); }
            HttpDataSource.Factory network=new DefaultHttpDataSource.Factory().setUserAgent("cubox-chill/0.1.0")
                    .setDefaultRequestProperties(headers(source)).setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(10000).setReadTimeoutMs(10000);
            Downloader standard=new DefaultDownloaderFactory(new CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(network),executor).createDownloader(request);
            boolean direct=androidx.media3.common.util.Util.inferContentTypeForUriAndMimeType(request.uri,request.mimeType)==C.CONTENT_TYPE_OTHER;
            return direct?new FastDownloader(request,cache,network,standard):standard;
        };
        manager=new DownloadManager(context,new DefaultDownloadIndex(db),factory);
        manager.setMaxParallelDownloads(2);
        manager.setMinRetryCount(5);
        manager.setRequirements(new androidx.media3.exoplayer.scheduler.Requirements(androidx.media3.exoplayer.scheduler.Requirements.NETWORK));
        manager.resumeDownloads();
    }
    static Map<String,String> headers(JSONObject source) {
        Map<String,String> result=new HashMap<>();
        JSONArray a=source.optJSONArray("headers");
        if(a!=null) for(int i=0;i<a.length();i++) { JSONArray p=a.optJSONArray(i); if(p!=null && p.length()==2) result.put(p.optString(0),p.optString(1)); }
        return result;
    }
    static MediaItem item(JSONObject source, String title) {
        MediaItem.Builder b=new MediaItem.Builder().setUri(source.optString("url")).setMediaMetadata(new MediaMetadata.Builder().setTitle(title).build());
        String sub=Engine.str(source,"subtitle");
        if(!sub.isEmpty()) {
            String path=Uri.parse(sub).getPath();
            String mime=path!=null && path.toLowerCase(Locale.ROOT).endsWith(".vtt")?MimeTypes.TEXT_VTT:MimeTypes.APPLICATION_SUBRIP;
            b.setSubtitleConfigurations(Collections.singletonList(new MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub)).setMimeType(mime).setLabel("Selected subtitle").setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build()));
        }
        return b.build();
    }
    static DataSource.Factory playbackFactory(JSONObject source, boolean offlineOnly) {
        HttpDataSource.Factory network=new DefaultHttpDataSource.Factory().setUserAgent("cubox-chill/0.1.0").setDefaultRequestProperties(headers(source)).setAllowCrossProtocolRedirects(true);
        return new CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(offlineOnly?null:new DefaultDataSource.Factory(context,network)).setCacheWriteDataSinkFactory(null);
    }
    interface Result { void complete(String error); }
    static void enqueue(Context c, JSONObject source, String title, String id, Result result) {
        MediaItem item=item(source,title);
        // Empty stream keys download every rendition, so DownloadHelper selects tracks first.
        DownloadHelper helper=DownloadHelper.forMediaItem(c,item,null,new DefaultHttpDataSource.Factory().setDefaultRequestProperties(headers(source)).setAllowCrossProtocolRedirects(true));
        helper.prepare(new DownloadHelper.Callback() {
            public void onPrepared(DownloadHelper h, boolean tracksInfoAvailable) {
                try {
                    int height=source.optInt("max_height",1080);
                    TrackSelectionParameters params=new TrackSelectionParameters.Builder(c)
                            .setMaxVideoSize(Integer.MAX_VALUE,height>0?height:1080).build();
                    if(tracksInfoAvailable) {
                        for(int period=0;period<h.getPeriodCount();period++) {
                            h.clearTrackSelections(period);
                            h.addTrackSelection(period,params);
                        }
                    }
                    JSONObject metadata=Engine.json("title",title,"source",source);
                    DownloadRequest req=h.getDownloadRequest(id,metadata.toString().getBytes(StandardCharsets.UTF_8));
                    DownloadService.sendAddDownload(c,OfflineService.class,req,false);
                    result.complete(null);
                } catch(Exception e) { result.complete(e.getMessage()!=null?e.getMessage():e.getClass().getSimpleName()); }
                finally { h.release(); }
            }
            public void onPrepareError(DownloadHelper h, IOException e) { h.release();result.complete("Cannot prepare download: "+e.getMessage()); }
        });
    }
    static final Map<String,DownloadRate> rates=new HashMap<>();
    static String transferStatus(Download d){
        if(d.state!=Download.STATE_DOWNLOADING){rates.remove(d.request.id);return "";}
        DownloadRate rate=rates.get(d.request.id);
        if(rate==null){rate=new DownloadRate();rates.put(d.request.id,rate);}
        double speed=rate.sample(android.os.SystemClock.elapsedRealtime(),d.getBytesDownloaded());
        if(speed<1)return " · Connecting…";
        String value=speed>=1048576?String.format(Locale.ROOT,"%.1f MB/s",speed/1048576):String.format(Locale.ROOT,"%.0f KB/s",speed/1024);
        if(d.contentLength>d.getBytesDownloaded()){
            long seconds=(long)((d.contentLength-d.getBytesDownloaded())/speed);
            value+=" · "+(seconds>=3600?seconds/3600+"h "+(seconds%3600)/60+"m":seconds>=60?seconds/60+"m":seconds+"s")+" remaining";
        }
        return " · "+value;
    }
    static List<Download> downloads() {
        List<Download> list=new ArrayList<>();
        try(DownloadCursor cursor=manager.getDownloadIndex().getDownloads()) { while(cursor.moveToNext())list.add(cursor.getDownload()); }
        catch(IOException ignored) {}
        Map<String,Download> active=new HashMap<>();
        for(Download d:manager.getCurrentDownloads())active.put(d.request.id,d);
        for(int i=0;i<list.size();i++){Download current=active.get(list.get(i).request.id);if(current!=null)list.set(i,current);}
        Set<String> ids=new HashSet<>();for(Download d:list)ids.add(d.request.id);rates.keySet().retainAll(ids);
        return list;
    }
    static JSONObject metadata(Download d) {
        try {return new JSONObject(new String(d.request.data,StandardCharsets.UTF_8));}
        catch(JSONException e){return Engine.json("title",d.request.id);}
    }
}
