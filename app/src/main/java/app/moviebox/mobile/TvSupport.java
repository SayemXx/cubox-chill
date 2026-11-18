package app.moviebox.mobile;
import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
final class TvSupport {
    static boolean activate(View view,int code,KeyEvent event){
        if(!view.isEnabled())return false;
        if(view instanceof android.widget.EditText){
            if(code!=KeyEvent.KEYCODE_DPAD_CENTER)return false;
            if(event.getAction()==KeyEvent.ACTION_UP&&!event.isCanceled()){
                view.requestFocus();
                ((android.view.inputmethod.InputMethodManager)view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(view,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
            return true;
        }
        if(!view.isClickable())return false;
        if(code!=KeyEvent.KEYCODE_DPAD_CENTER&&code!=KeyEvent.KEYCODE_ENTER&&code!=KeyEvent.KEYCODE_NUMPAD_ENTER)return false;
        if(event.getAction()==KeyEvent.ACTION_DOWN){if(event.getRepeatCount()==0)view.setPressed(true);return true;}
        if(event.getAction()==KeyEvent.ACTION_UP){boolean pressed=view.isPressed();view.setPressed(false);if(pressed&&!event.isCanceled())view.performClick();return true;}
        return false;
    }
    static boolean enabled(Context c){UiModeManager m=(UiModeManager)c.getSystemService(Context.UI_MODE_SERVICE);return m!=null&&m.getCurrentModeType()==Configuration.UI_MODE_TYPE_TELEVISION;}
    static void focus(View view){
        if(view.isClickable()){
            view.setFocusable(true);
            if(view.getTag(R.id.tv_focus_initialized)==null){view.setTag(R.id.tv_focus_initialized,Boolean.TRUE);view.setOnKeyListener(TvSupport::activate);view.setOnFocusChangeListener((v,has)->{
                if(has){GradientDrawable outline=new GradientDrawable();outline.setColor(0x22ffffff);outline.setStroke(3,Color.WHITE);outline.setCornerRadius(8);v.setForeground(outline);v.requestRectangleOnScreen(new android.graphics.Rect(0,0,v.getWidth(),v.getHeight()),false);}else {v.setForeground(null);v.setPressed(false);}
            });}
        }
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)focus(g.getChildAt(i));}
    }
}
