package app.moviebox.mobile;

import android.app.Activity;
import android.app.Instrumentation;
import org.json.*;

/** Bounded live-series probe: resolves one episode and renders a first frame, without a full download. */
final class SeriesSmoke {
    static String run(Instrumentation test,Activity activity,String query)throws Exception {
        JSONArray items=(JSONArray)Engine.request(activity,Engine.json("op","search","provider","moviebox","query",query,"page",1));
        JSONObject series=null;
        for(int i=0;i<items.length();i++)if("series".equals(items.getJSONObject(i).optString("media_type"))){series=items.getJSONObject(i);break;}
        if(series==null)throw new Exception("No series found for "+query);
        String id=series.getJSONObject("id").getString("value");
        JSONObject details=(JSONObject)Engine.request(activity,Engine.json("op","details","provider","moviebox","id",id));
        JSONArray dubs=details.optJSONArray("dubs");
        if(dubs!=null)for(int i=0;i<dubs.length();i++)if(dubs.getJSONObject(i).optString("label").toLowerCase(java.util.Locale.ROOT).contains("hindi")){id=dubs.getJSONObject(i).getString("subject_id");break;}
        JSONObject season=details.getJSONArray("seasons").getJSONObject(0);
        int sn=season.getInt("number"),en=season.getJSONArray("episodes").getJSONObject(0).getInt("number");
        JSONArray releases=(JSONArray)Engine.request(activity,Engine.json("op","releases","provider","moviebox","id",id,"season",sn,"episode",en));
        if(releases.length()==0)throw new Exception("No episode streams returned");
        JSONObject source=(JSONObject)Engine.request(activity,Engine.json("op","resolve","provider","moviebox","release",releases.getJSONObject(0),"mirror",0));
        MediaSmoke.play(test,activity,source,null,false);
        return "PASS: "+series.getString("title")+" S"+sn+"E"+en+" resolved "+releases.length()+" qualities and rendered first frame";
    }
}
