package app.moviebox.mobile;
import org.junit.Test;
import static org.junit.Assert.*;
public class DownloadRateTest {
    @Test public void ignoresPreviouslyCachedBytesAndComputesRate(){
        DownloadRate r=new DownloadRate();assertEquals(0,r.sample(1000,5000000),0);
        assertEquals(1000,r.sample(2000,5001000),0);assertEquals(1500,r.sample(3000,5003000),0);
    }
    @Test public void resetsAfterRestartOrClockReset(){
        DownloadRate r=new DownloadRate();r.sample(1000,100);r.sample(2000,200);
        assertEquals(0,r.sample(3000,0),0);assertEquals(0,r.sample(2000,100),0);
    }
}
