package app.moviebox.mobile;
import org.junit.Test;
import static org.junit.Assert.*;
public class VersionsTest {
    @Test public void comparesNumericVersions(){assertTrue(Versions.newer("v0.1.29","0.1.28"));assertTrue(Versions.newer("0.10.0","0.9.9"));assertFalse(Versions.newer("v0.1.27","0.1.28"));assertFalse(Versions.newer("v0.1.28","0.1.28"));}
    @Test public void rejectsMalformedReleaseTags(){assertFalse(Versions.newer("latest","0.1.0"));assertFalse(Versions.newer("v1.x.0","1.0.0"));}
    @Test public void restrictsRepositoryNames(){assertTrue(Versions.validRepository("Sayem/cubox-chill"));assertFalse(Versions.validRepository("https://example.com/a"));assertFalse(Versions.validRepository("a/b/c"));}
}
