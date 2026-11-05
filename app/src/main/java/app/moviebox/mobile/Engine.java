package app.moviebox.mobile;

import android.content.Context;
import org.json.*;

final class Engine {
    static { System.loadLibrary("moviebox_mobile"); }
    private static native String call(String input, String path);
    static Object request(Context c, JSONObject input) throws Exception {
        String raw = call(input.toString(), c.getFilesDir().getAbsolutePath());
        if (raw == null) throw new Exception("Engine did not return a response");
        JSONObject result = new JSONObject(raw);
        if (!result.optBoolean("ok")) throw new Exception(result.optString("error", "Provider unavailable"));
        return result.get("data");
    }
    static JSONObject json(Object... pairs) {
        JSONObject j = new JSONObject();
        try { for (int i=0;i<pairs.length;i+=2) j.put((String)pairs[i],pairs[i+1]); }
        catch (JSONException e) { throw new IllegalArgumentException(e); }
        return j;
    }
    static String str(JSONObject j, String key) { return j.isNull(key) ? "" : j.optString(key, ""); }
}
