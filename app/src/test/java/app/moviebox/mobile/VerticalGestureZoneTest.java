package app.moviebox.mobile;
import org.junit.Test;
import static org.junit.Assert.*;
public class VerticalGestureZoneTest {
    @Test public void onlyOuterQuartersRespond(){for(float width:new float[]{360,1080,1920}){assertEquals(-1,VerticalGestureZone.at(width*.1f,width));assertEquals(1,VerticalGestureZone.at(width*.9f,width));for(float fraction:new float[]{.25f,.3f,.5f,.7f,.75f})assertEquals(0,VerticalGestureZone.at(width*fraction,width));}}
    @Test public void invalidDimensionsAreIgnored(){assertEquals(0,VerticalGestureZone.at(0,0));assertEquals(0,VerticalGestureZone.at(-1,100));assertEquals(0,VerticalGestureZone.at(101,100));}
}
