package app.moviebox.mobile;
import android.app.*;
import android.view.View;
import java.io.File;
import org.json.*;

final class CatalogUpdateSmoke {
    static void run(Instrumentation test,MainActivity activity)throws Exception {
        File root=new File(activity.getCacheDir(),"catalog-smoke");long now=System.currentTimeMillis();String key=CatalogStore.key("moviebox","",1);
        CatalogStore store=new CatalogStore(root);
        JSONArray items=new JSONArray().put(Engine.json("title","Cached test series","media_type","series","id",Engine.json("provider","moviebox","value","test-series")));
        store.save(key,items,now);CatalogStore reopened=new CatalogStore(root);CatalogStore.Entry saved=reopened.load(key,now+1);
        if(saved==null||!saved.fresh(now+1)||saved.fresh(now+CatalogStore.FRESH_MS+1))throw new AssertionError("Catalog persistence/freshness failed");
        if(reopened.load(CatalogStore.key("moviebox","different search",1),now)!=null)throw new AssertionError("Catalog keys mixed queries");
        CatalogStore original=activity.catalogStore;
        try {
            test.runOnMainSync(()->{activity.catalogStore=reopened;activity.catalog=new JSONArray();activity.showBrowse();});
            boolean[] rendered={false};for(int i=0;i<100;i++){test.runOnMainSync(()->rendered[0]=activity.catalog.length()==1&&"Cached test series".equals(activity.catalog.optJSONObject(0).optString("title"))&&activity.progress.getVisibility()==View.GONE);if(rendered[0])break;Thread.sleep(100);}
            if(!rendered[0])throw new AssertionError("Home did not render the saved catalog");
            JSONObject release=Engine.json("tag_name","v"+BuildConfig.VERSION_NAME,"body","Test release","html_url","https://github.com/SayemXx/cubox-chill/releases/latest","assets",new JSONArray().put(Engine.json("name","app.apk","browser_download_url","https://github.com/SayemXx/cubox-chill/releases/download/test/app.apk")));
            test.runOnMainSync(()->{AlertDialog dialog=activity.showRelease(release,false);if(!"Download APK again".contentEquals(dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText()))throw new AssertionError("Same-version download button missing");dialog.dismiss();});
            release.put("tag_name","v99.0.0");
            test.runOnMainSync(()->{AlertDialog dialog=activity.showRelease(release,false);if(!"Download update".contentEquals(dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText()))throw new AssertionError("New-version download button missing");dialog.dismiss();});
        } finally {test.runOnMainSync(()->{activity.catalogStore=original;activity.catalog=new JSONArray();activity.showDownloads();});}
    }
}
