package app.moviebox.mobile;
import org.junit.Test;
import static org.junit.Assert.*;
public class TapSeekTest {
    @Test public void repeatedTapsAccumulateInTenSecondSteps(){TapSeek t=new TapSeek();assertEquals(0,t.tap(true,0));assertEquals(10,t.tap(true,100));assertEquals(20,t.tap(true,200));assertEquals(30,t.tap(true,300));assertEquals(40,t.tap(true,400));}
    @Test public void pauseOrSideChangeStartsANewBurst(){TapSeek t=new TapSeek();t.tap(true,0);assertEquals(10,t.tap(true,100));assertEquals(0,t.tap(false,200));assertEquals(10,t.tap(false,300));assertEquals(0,t.tap(false,1000));t.reset();assertEquals(0,t.tap(false,1100));}
}
