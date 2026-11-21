package app.moviebox.mobile;

import android.app.*;
import android.os.Bundle;
import android.net.Uri;
import androidx.media3.common.C;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.*;
import androidx.media3.datasource.cache.*;
import androidx.media3.exoplayer.offline.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Real HTTP/cache integration checks, independent of third-party movie servers. */
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public class FastDownloadInstrumentation extends Instrumentation {
    static final int SIZE=9*1024*1024;
    String phase="parallel";
    public void onCreate(Bundle args){super.onCreate(args);start();}
    static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    public void onStart(){Bundle result=new Bundle();SimpleCache cache=null;
        try(Server server=new Server()){
            File dir=new File(getTargetContext().getCacheDir(),"fast-test-"+System.nanoTime());
            cache=new SimpleCache(dir,new NoOpCacheEvictor(),new StandaloneDatabaseProvider(getTargetContext()));
            String url=server.url();
            FastDownloader first=create(url,cache);
            first.download(null);verify(cache,url,0);
            require(server.peak.get()>=2,"Expected overlapping range connections");
            require(server.authErrors.get()==0,"Provider headers lost");
            phase="ignored range fallback";cache.removeResource(url);server.mode="ignore";
            create(url,cache).download(null);verify(cache,url,0);
            phase="rejected workers fallback";cache.removeResource(url);server.mode="reject-workers";
            create(url,cache).download(null);verify(cache,url,0);
            phase="pause and resume";cache.removeResource(url);server.mode="ranges";server.delay=4;
            FastDownloader paused=create(url,cache);AtomicLong progress=new AtomicLong();AtomicReference<Throwable> error=new AtomicReference<>();
            Thread worker=new Thread(()->{try{paused.download((n,b,p)->progress.set(b));}catch(Throwable t){error.set(t);}});
            worker.start();long deadline=System.currentTimeMillis()+15000;
            while(progress.get()<512*1024&&worker.isAlive()&&System.currentTimeMillis()<deadline)Thread.sleep(10);
            paused.cancel();worker.interrupt();worker.join(15000);
            require(!worker.isAlive(),"Cancellation did not finish");
            long saved=cache.getCachedBytes(url,0,SIZE);require(saved>0&&saved<SIZE,"Pause did not preserve a partial download: "+saved);
            long before=server.sent.get();server.delay=0;create(url,cache).download(null);verify(cache,url,0);
            require(server.sent.get()-before<SIZE,"Resume downloaded the whole file again");
            // A changed validator must discard the old representation, including fully cached spans.
            phase="changed file";server.revision=1;create(url,cache).download(null);verify(cache,url,1);
            // Truncated bodies must never be reported as a completed file; retry fills cached gaps.
            phase="truncated body";cache.removeResource(url);server.mode="truncate";boolean failed=false;
            FastDownloader retry=create(url,cache);try{retry.download(null);}catch(IOException expected){failed=true;}
            require(failed,"Truncated response was accepted");server.mode="ranges";
            retry.download(null);verify(cache,url,1);
            create(url,cache).remove();require(cache.getCachedBytes(url,0,SIZE)==0,"Remove left cached media");
            result.putString("stream","PASS: parallel ranges, header forwarding, ignored/rejected ranges fallback, pause/resume, changed ETag, truncated-body retry, byte-for-byte offline integrity and removal.");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable t){result.putString("stream","FAIL during "+phase+": "+android.util.Log.getStackTraceString(t));finish(Activity.RESULT_CANCELED,result);}
        finally{if(cache!=null)cache.release();}
    }
    FastDownloader create(String url,SimpleCache cache){
        DownloadRequest request=new DownloadRequest.Builder("fixture",Uri.parse(url)).build();
        HttpDataSource.Factory network=new DefaultHttpDataSource.Factory().setDefaultRequestProperties(Collections.singletonMap("X-Test","secret"))
            .setConnectTimeoutMs(15000).setReadTimeoutMs(15000);
        CacheDataSource.Factory cached=new CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(network);
        return new FastDownloader(request,cache,network,new ProgressiveDownloader(request.toMediaItem(),cached));
    }
    static byte value(long position,int revision){return (byte)(position*31+revision*17);}
    void verify(SimpleCache cache,String url,int revision)throws Exception{
        require(cache.getCachedBytes(url,0,SIZE)==SIZE,"Cached size mismatch");
        CacheDataSource reader=new CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(null).createDataSource();
        try{reader.open(new DataSpec(Uri.parse(url)));byte[] b=new byte[65536];long position=0;int n;
            while((n=reader.read(b,0,b.length))!=C.RESULT_END_OF_INPUT){for(int i=0;i<n;i++)if(b[i]!=value(position+i,revision))throw new AssertionError("Corrupt byte at "+(position+i));position+=n;}
            require(position==SIZE,"Offline EOF mismatch");
        }finally{reader.close();}
    }
    static final class Server implements AutoCloseable {
        final ServerSocket socket=new ServerSocket(0);final ExecutorService clients=Executors.newCachedThreadPool();
        final AtomicInteger active=new AtomicInteger(),peak=new AtomicInteger(),authErrors=new AtomicInteger();
        final AtomicLong sent=new AtomicLong();volatile boolean closed;volatile String mode="ranges";volatile int revision,delay=2;
        Server()throws IOException{Thread accept=new Thread(()->{while(!closed)try{Socket s=socket.accept();clients.submit(()->serve(s));}catch(IOException e){if(!closed)throw new RuntimeException(e);}});accept.setDaemon(true);accept.start();}
        String url(){return "http://127.0.0.1:"+socket.getLocalPort()+"/movie.mp4";}
        void serve(Socket s){boolean counted=false;try(Socket client=s){
            client.setSoTimeout(5000);BufferedReader in=new BufferedReader(new InputStreamReader(client.getInputStream(),StandardCharsets.US_ASCII));
            in.readLine();Map<String,String> headers=new HashMap<>();String line;
            while((line=in.readLine())!=null&&!line.isEmpty()){int colon=line.indexOf(':');if(colon>0)headers.put(line.substring(0,colon).toLowerCase(Locale.ROOT),line.substring(colon+1).trim());}
            if(!"secret".equals(headers.get("x-test")))authErrors.incrementAndGet();
            String range=headers.get("range");long start=0,end=SIZE-1;boolean partial=range!=null&&!mode.equals("ignore");
            boolean probe="bytes=0-0".equals(range);
            if(mode.equals("reject-workers")&&!probe&&headers.containsKey("if-range"))partial=false;
            if(partial){String[] bounds=range.substring(6).split("-");start=Long.parseLong(bounds[0]);if(bounds.length>1)end=Math.min(end,Long.parseLong(bounds[1]));}
            int rev=revision;long length=end-start+1;
            OutputStream out=client.getOutputStream();String response="HTTP/1.1 "+(partial?"206 Partial Content":"200 OK")+"\r\nContent-Length: "+length+"\r\nETag: \"v"+rev+"\"\r\nConnection: close\r\n";
            if(partial)response+="Content-Range: bytes "+start+"-"+end+"/"+SIZE+"\r\n";
            out.write((response+"\r\n").getBytes(StandardCharsets.US_ASCII));out.flush();
            if(!probe){counted=true;int now=active.incrementAndGet();peak.accumulateAndGet(now,Math::max);}
            if(mode.equals("truncate")&&!probe)end=Math.min(end,start+65535);
            byte[] b=new byte[32768];for(long p=start;p<=end;){int n=(int)Math.min(b.length,end-p+1);for(int i=0;i<n;i++)b[i]=value(p+i,rev);out.write(b,0,n);out.flush();sent.addAndGet(n);p+=n;if(delay>0&&!probe)Thread.sleep(delay);}
        }catch(Exception ignored){}finally{if(counted)active.decrementAndGet();}}
        public void close()throws IOException{closed=true;socket.close();clients.shutdownNow();}
    }
}

