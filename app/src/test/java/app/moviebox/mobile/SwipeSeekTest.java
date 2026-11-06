package app.moviebox.mobile;
import org.junit.Test;
import static org.junit.Assert.*;
public class SwipeSeekTest {
    @Test public void swipesSeekRelativeToStartingPosition(){assertEquals(90000,SwipeSeek.target(60000,300000,250,1000));assertEquals(30000,SwipeSeek.target(60000,300000,-250,1000));}
    @Test public void clampsToMediaBounds(){assertEquals(0,SwipeSeek.target(10000,300000,-500,1000));assertEquals(300000,SwipeSeek.target(290000,300000,500,1000));assertEquals(120000,SwipeSeek.target(0,-1,1000,1000));}
}
