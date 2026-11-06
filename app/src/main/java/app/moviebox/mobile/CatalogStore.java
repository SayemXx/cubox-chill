package app.moviebox.mobile;
import java.io.File;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** Metadata only: playback URLs are always resolved fresh. Uses bounded disk storage. */
final class CatalogStore {
    static final long FRESH_MS=6L*60*60*1000;
    private final ArtworkCache storage;
    CatalogStore(File root){storage=new ArtworkCache(new File(root,"catalog-v1"),4L*1024*1024);}
    static String key(String provider,String query,int page){return provider+"\n"+query+"\n"+page;}
    static final class Entry {
        final JSONArray items;final long savedAt;
        Entry(JSONArray items,long savedAt){this.items=items;this.savedAt=savedAt;}
        boolean fresh(long now){return now>=savedAt&&now-savedAt<FRESH_MS;}
    }
    Entry load(String key,long now){try{byte[] bytes=storage.get(key,now);if(bytes==null)return null;JSONObject data=new JSONObject(new String(bytes,StandardCharsets.UTF_8));return new Entry(data.getJSONArray("items"),data.getLong("savedAt"));}catch(Exception e){return null;}}
    void save(String key,JSONArray items,long now){try{if(items.length()==0)return;JSONObject data=new JSONObject().put("items",items).put("savedAt",now);storage.put(key,data.toString().getBytes(StandardCharsets.UTF_8),now);}catch(Exception ignored){}}
}
