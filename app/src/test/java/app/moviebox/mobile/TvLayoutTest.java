package app.moviebox.mobile;
import org.junit.Test;
import static org.junit.Assert.*;
public class TvLayoutTest {
    @Test public void scalesWithAvailableSpace(){assertTrue(TvLayout.icon(1280,720)>TvLayout.icon(960,540));assertTrue(TvLayout.poster(1280,720)>TvLayout.poster(960,540));}
    @Test public void boundsSmallAndLargeLayouts(){assertEquals(.8f,TvLayout.scale(320,180),.001f);assertEquals(1.6f,TvLayout.scale(3840,2160),.001f);assertTrue(TvLayout.hero(960,540)<540);}
}
