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
        assertNull(reopened.get("https://a/poster.jpg",now+ArtworkCache.MAX_AGE+101));
    }
    @Test public void evictsLeastRecentlyUsedAndBoundsStorage()throws Exception {
        File dir=folder.newFolder();ArtworkCache cache=new ArtworkCache(dir,40);long now=System.currentTimeMillis();
        cache.put("one",new byte[10],now);cache.put("two",new byte[10],now+1000);
        assertNotNull(cache.get("one",now+2000));cache.put("three",new byte[10],now+3000);
        assertNull(cache.get("two",now+4000));assertNotNull(cache.get("one",now+4000));assertNotNull(cache.get("three",now+4000));
        long bytes=0;for(File f:dir.listFiles())bytes+=f.length();assertTrue(bytes<=40);
    }
    @Test public void readsExtendRetentionAndPruneRemovesOnlyUnusedImages()throws Exception {
        File dir=folder.newFolder();ArtworkCache cache=new ArtworkCache(dir,1000);
        long now=System.currentTimeMillis(),day=24L*60*60*1000;
        cache.put("active",new byte[]{1},now);cache.put("unused",new byte[]{2},now);
        assertNotNull(cache.get("active",now+29*day));
        cache.prune(now+31*day);
        assertEquals(1,dir.listFiles().length);
        assertNotNull(new ArtworkCache(dir,1000).get("active",now+31*day));
        cache.touch("active",now+59*day);cache.prune(now+61*day);
        assertNotNull(cache.get("active",now+61*day));
        cache.prune(now+92*day);assertEquals(0,dir.listFiles().length);
    }
    @Test public void migratesExistingImagesWithoutDownloadingAgain()throws Exception {
        File old=folder.newFolder(),current=folder.newFolder();long now=System.currentTimeMillis();
        byte[] data={3,2,1};new ArtworkCache(old,100).put("poster",data,now);
        ArtworkCache cache=new ArtworkCache(current,100);cache.migrateFrom(old);cache.prune(now+100);
        assertArrayEquals(data,cache.get("poster",now+100));assertEquals(0,old.listFiles().length);
    }
}
