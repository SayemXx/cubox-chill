package app.moviebox.mobile;
import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
final class TvSupport {
    static boolean enabled(Context c){UiModeManager m=(UiModeManager)c.getSystemService(Context.UI_MODE_SERVICE);return m!=null&&m.getCurrentModeType()==Configuration.UI_MODE_TYPE_TELEVISION;}
    static void focus(View view){
        if(view.isClickable()){
            view.setFocusable(true);
            if(view.getTag(android.R.id.custom)==null){view.setTag(android.R.id.custom,Boolean.TRUE);view.setOnFocusChangeListener((v,has)->{
                if(has){GradientDrawable outline=new GradientDrawable();outline.setColor(0x22ffffff);outline.setStroke(3,Color.WHITE);outline.setCornerRadius(8);v.setForeground(outline);v.requestRectangleOnScreen(new android.graphics.Rect(0,0,v.getWidth(),v.getHeight()),false);}else v.setForeground(null);
            });}
        }
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)focus(g.getChildAt(i));}
    }
}
