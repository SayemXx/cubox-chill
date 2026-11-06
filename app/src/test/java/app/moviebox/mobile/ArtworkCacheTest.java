package app.moviebox.mobile;
import java.io.File;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
public class ArtworkCacheTest {
    @Rule public TemporaryFolder folder=new TemporaryFolder();
    @Test public void persistsAcrossInstancesAndExpires()throws Exception {
        File dir=folder.newFolder();byte[] data={1,2,3};long now=System.currentTimeMillis();
        new ArtworkCache(dir,100).put("https://a/poster.jpg",data,now);
        ArtworkCache reopened=new ArtworkCache(dir,100);
        assertArrayEquals(data,reopened.get("https://a/poster.jpg",now+100));
        assertNull(reopened.get("https://a/poster.jpg",now+ArtworkCache.MAX_AGE+1));
    }
    @Test public void evictsLeastRecentlyUsedAndBoundsStorage()throws Exception {
        File dir=folder.newFolder();ArtworkCache cache=new ArtworkCache(dir,40);long now=System.currentTimeMillis();
        cache.put("one",new byte[10],now);cache.put("two",new byte[10],now+1000);
        assertNotNull(cache.get("one",now+2000));cache.put("three",new byte[10],now+3000);
        assertNull(cache.get("two",now+4000));assertNotNull(cache.get("one",now+4000));assertNotNull(cache.get("three",now+4000));
        long bytes=0;for(File f:dir.listFiles())bytes+=f.length();assertTrue(bytes<=40);
    }
}
