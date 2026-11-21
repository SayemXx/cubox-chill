package app.moviebox.mobile;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class M3uPlaylistTest {
    @Test public void parsesNamesQuotedCommasLogosAndGroups(){
        List<M3uPlaylist.Channel> c=M3uPlaylist.parse("\ufeff#EXTM3U\n#EXTINF:-1 tvg-logo=\"logos/a.png\" group-title=\"News, Live\",Channel, One\nstream.m3u8", "https://example.com/list/index.m3u");
        assertEquals(1,c.size());assertEquals("Channel, One",c.get(0).name);assertEquals("News, Live",c.get(0).group);assertEquals("https://example.com/list/logos/a.png",c.get(0).logo);assertEquals("https://example.com/list/stream.m3u8",c.get(0).url);
    }
    @Test public void preservesAllowedHeadersAndResetsBetweenChannels(){
        List<M3uPlaylist.Channel> c=M3uPlaylist.parse("#EXTM3U\n#EXTINF:-1,A\n#EXTVLCOPT:http-user-agent=Player\n#EXTVLCOPT:http-referrer=https://example.com/\nhttps://example.com/a.m3u8|Origin=https%3A%2F%2Fexample.com\n#EXTINF:-1,B\nhttps://example.com/b.m3u8","");
        assertEquals("Player",c.get(0).headers.get("User-Agent"));assertEquals("https://example.com/",c.get(0).headers.get("Referer"));assertEquals(3,c.get(0).headers.size());assertTrue(c.get(1).headers.isEmpty());
    }
    @Test public void skipsUnsupportedSchemesDrmAndDuplicates(){
        List<M3uPlaylist.Channel> c=M3uPlaylist.parse("#EXTM3U\n#EXTINF:-1,Bad\nfile:///secret\n#EXTINF:-1,DRM\n#KODIPROP:inputstream.adaptive.license_key=secret\nhttps://example.com/drm.mpd\n#EXTINF:-1,Good\nhttps://example.com/a\n#EXTINF:-1,Duplicate\nhttps://example.com/a","");
        assertEquals(1,c.size());assertEquals("Good",c.get(0).name);
    }
    @Test public void recognizesHlsAsOneStream(){
        List<M3uPlaylist.Channel> c=M3uPlaylist.parse("#EXTM3U\n#EXT-X-TARGETDURATION:6\n#EXTINF:6,\npart1.ts\n#EXTINF:6,\npart2.ts", "https://example.com/live.m3u8");
        assertEquals(1,c.size());assertEquals("https://example.com/live.m3u8",c.get(0).url);
    }
    @Test(expected=IllegalArgumentException.class)public void rejectsHtmlErrors(){M3uPlaylist.parse("<!doctype html>\n<html>Access denied</html>","https://example.com/list");}
    @Test(expected=IllegalArgumentException.class)public void rejectsLocalSegmentLists(){M3uPlaylist.parse("#EXTM3U\n#EXT-X-TARGETDURATION:6\npart.ts","");}
    @Test(expected=IllegalArgumentException.class)public void rejectsOversizedLists(){M3uPlaylist.parse(String.join("",Collections.nCopies(5*1024*1024+1,"x")),"");}
    @Test public void rejectsHeaderInjection(){Map<String,String> h=new HashMap<>();M3uPlaylist.header(h,"User-Agent","x\r\nInjected: yes");assertTrue(h.isEmpty());}
    @Test public void supportsGroupTagAndPlainUrlLists(){List<M3uPlaylist.Channel> c=M3uPlaylist.parse("#EXTM3U\n#EXTINF:-1,Example\n#EXTGRP:Sports\nhttps://example.com/live","");assertEquals("Sports",c.get(0).group);assertEquals(1,M3uPlaylist.parse("https://example.com/live","").size());}
    @Test public void convertsGithubBlobLinks(){assertEquals("https://raw.githubusercontent.com/owner/repo/main/list.m3u",LiveTvStore.normalize("https://github.com/owner/repo/blob/main/list.m3u"));}
}
