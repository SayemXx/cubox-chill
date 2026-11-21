package app.moviebox.mobile;

import android.net.Uri;
import androidx.media3.common.C;
import androidx.media3.datasource.*;
import androidx.media3.datasource.cache.*;
import androidx.media3.exoplayer.offline.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.*;

/** Four bounded cache writers; only validated byte-range responses may enter the cache. */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
final class FastDownloader implements Downloader {
    static final long MIN_SIZE=8L*1024*1024;
    static final String IDENTITY="cubox.download.identity";
    private final DownloadRequest request;
    private final Cache cache;
    private final HttpDataSource.Factory network;
    private final Downloader fallback;
    private final String key;
    private final List<CacheWriter> writers=new CopyOnWriteArrayList<>();
    private volatile boolean canceled;
    private volatile ExecutorService pool;
    private final AtomicBoolean rejected=new AtomicBoolean();
    private boolean sequential;

    FastDownloader(DownloadRequest request,Cache cache,HttpDataSource.Factory network,Downloader fallback){
        this.request=request;this.cache=cache;this.network=network;this.fallback=fallback;
        key=request.customCacheKey!=null?request.customCacheKey:request.uri.toString();
    }
    static String header(Map<String,List<String>> headers,String name){
        for(Map.Entry<String,List<String>> h:headers.entrySet())
            if(name.equalsIgnoreCase(h.getKey())&&!h.getValue().isEmpty())return h.getValue().get(0);
        return "";
    }
    static long[] range(String value){
        Matcher m=Pattern.compile("bytes (\\d+)-(\\d+)/(\\d+)").matcher(value);
        if(!m.matches())return null;
        try {long a=Long.parseLong(m.group(1)),b=Long.parseLong(m.group(2)),n=Long.parseLong(m.group(3));
            return a>=0&&b>=a&&n>b?new long[]{a,b,n}:null;
        }catch(NumberFormatException e){return null;}
    }
    private static String validator(Map<String,List<String>> h){
        String etag=header(h,"ETag");
        return etag.startsWith("\"")&&etag.endsWith("\"")?etag:"";
    }
    private void checkCanceled() throws InterruptedException {
        if(canceled||Thread.currentThread().isInterrupted())throw new InterruptedException("Download paused");
    }
    @Override public void download(ProgressListener listener)throws IOException,InterruptedException{
        checkCanceled();
        if(sequential){fallback.download(listener);return;}
        HttpDataSource probe=network.createDataSource();
        long length=-1;String tag="";
        try{
            probe.open(new DataSpec.Builder().setUri(request.uri).setLength(1).build());
            long[] r=range(header(probe.getResponseHeaders(),"Content-Range"));
            tag=validator(probe.getResponseHeaders());
            if(probe.getResponseCode()==206&&r!=null&&r[0]==0&&r[1]==0&&
                (header(probe.getResponseHeaders(),"Content-Encoding").isEmpty()||header(probe.getResponseHeaders(),"Content-Encoding").equalsIgnoreCase("identity")))length=r[2];
        }catch(HttpDataSource.InvalidResponseCodeException e){
            if(e.responseCode!=400&&e.responseCode!=405&&e.responseCode!=416&&e.responseCode!=501)throw e;
        }finally{probe.close();}
        checkCanceled();
        // A stable validator prevents stitching together bytes from different file revisions.
        if(length<MIN_SIZE||tag.isEmpty()){
            if(!cache.getContentMetadata(key).get(IDENTITY,"").isEmpty())clearPartial();
            sequential=true;fallback.download(listener);return;
        }
        String identity=tag+":"+length;
        if(!identity.equals(cache.getContentMetadata(key).get(IDENTITY,"")))clearPartial();
        ContentMetadataMutations metadata=new ContentMetadataMutations().set(IDENTITY,identity);
        ContentMetadataMutations.setContentLength(metadata,length);cache.applyContentMetadataMutations(key,metadata);
        final long total=length;final String etag=tag;
        rejected.set(false);writers.clear();

        pool=Executors.newFixedThreadPool(4);
        List<Future<?>> tasks=new ArrayList<>();
        Object progressLock=new Object();
        long[] partBytes=new long[4];
        for(int i=0;i<4;i++)partBytes[i]=cache.getCachedBytes(key,total*i/4,total*(i+1)/4-total*i/4);
        IOException failure=null;
        try{
            for(int i=0;i<4;i++){
                final int part=i;long start=total*i/4,end=total*(i+1)/4;
                CacheDataSource.Factory factory=new CacheDataSource.Factory().setCache(cache)
                    .setUpstreamDataSourceFactory(()->new CheckedSource(total,etag,end));
                CacheWriter writer=new CacheWriter(factory.createDataSourceForDownloading(),
                    new DataSpec.Builder().setUri(request.uri).setKey(key).setPosition(start).setLength(end-start)
                        .setFlags(DataSpec.FLAG_ALLOW_CACHE_FRAGMENTATION).build(),null,(n,b,delta)->{
                        synchronized(progressLock){partBytes[part]=b;if(listener!=null){long bytes=0;for(long count:partBytes)bytes+=count;
                            listener.onProgress(total,bytes,100f*bytes/total);}}
                    });
                writers.add(writer);
                tasks.add(pool.submit(()->{writer.cache();return null;}));
            }
            for(Future<?> task:tasks){
                try{task.get();}catch(ExecutionException e){
                    Throwable cause=e.getCause();failure=cause instanceof IOException?(IOException)cause:new IOException(cause);break;
                }
            }
        }finally{
            for(CacheWriter w:writers)w.cancel();
            pool.shutdownNow();
            // Never clear or reuse the cache while a worker can still write to it.
            boolean interrupted=Thread.interrupted();
            while(!pool.isTerminated())try{pool.awaitTermination(1,TimeUnit.SECONDS);}catch(InterruptedException e){interrupted=true;}
            writers.clear();pool=null;
            if(interrupted)Thread.currentThread().interrupt();
        }
        checkCanceled();
        if(rejected.get()){
            clearPartial();sequential=true;fallback.download(listener);return;
        }
        if(failure!=null)throw failure;
        if(cache.getCachedBytes(key,0,total)!=total)throw new EOFException("Download incomplete; retrying missing bytes");
        if(listener!=null)listener.onProgress(total,total,100f);
    }
    private void clearPartial()throws IOException{
        cache.removeResource(key);
        cache.applyContentMetadataMutations(key,new ContentMetadataMutations().remove(IDENTITY).remove(ContentMetadata.KEY_CONTENT_LENGTH));
    }
    @Override public void cancel(){canceled=true;fallback.cancel();for(CacheWriter w:writers)w.cancel();}
    @Override public void remove(){fallback.remove();}

    private final class CheckedSource implements DataSource {
        private final HttpDataSource delegate=network.createDataSource();
        private final long total,partEnd;private final String tag;
        CheckedSource(long total,String tag,long partEnd){this.total=total;this.tag=tag;this.partEnd=partEnd;}
        public long open(DataSpec spec)throws IOException{
            long end=spec.length==C.LENGTH_UNSET?total-1:spec.position+spec.length-1;
            if(end>=partEnd)throw new IOException("Retry the bounded part instead of expanding its range");
            Map<String,String> h=new HashMap<>(spec.httpRequestHeaders);
            h.put("Range","bytes="+spec.position+"-"+end);h.put("If-Range",tag);h.put("Accept-Encoding","identity");
            // Position zero avoids DefaultHttpDataSource silently skipping a full 200 response.
            delegate.open(spec.buildUpon().setPosition(0).setLength(C.LENGTH_UNSET).setHttpRequestHeaders(h).build());
            long[] r=range(header(delegate.getResponseHeaders(),"Content-Range"));
            String encoding=header(delegate.getResponseHeaders(),"Content-Encoding");
            if(delegate.getResponseCode()!=206||r==null||r[0]!=spec.position||r[1]!=end||r[2]!=total||
                !tag.equals(validator(delegate.getResponseHeaders()))||(!encoding.isEmpty()&&!encoding.equalsIgnoreCase("identity"))){
                rejected.set(true);throw new IOException("Server changed or rejected ranged download");
            }
            return end-spec.position+1;
        }
        public int read(byte[] b,int o,int n)throws IOException{return delegate.read(b,o,n);}
        public Uri getUri(){return delegate.getUri();}
        public Map<String,List<String>> getResponseHeaders(){return delegate.getResponseHeaders();}
        public void addTransferListener(TransferListener l){delegate.addTransferListener(l);}
        public void close()throws IOException{delegate.close();}
    }
}
