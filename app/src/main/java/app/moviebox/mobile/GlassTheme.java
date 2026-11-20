package app.moviebox.mobile;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.Drawable;

/** Lightweight translucent surfaces: no blur pass or video-surface effects. */
final class GlassTheme {
    static GradientDrawable panel(Context context,int radius){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0x705d6272,0x80272b39,0x99202530});
        d.setCornerRadius(radius*context.getResources().getDisplayMetrics().density);
        d.setStroke(Math.max(1,Math.round(context.getResources().getDisplayMetrics().density)),0x66ffffff);
        return d;
    }
    static Drawable action(Context context,int radius){
        return new RippleDrawable(ColorStateList.valueOf(0x28ffffff),panel(context,radius),null);
    }
    static GradientDrawable backdrop(){
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xff321a2c,0xff172336,0xff151925,0xff080b13});
    }
    private GlassTheme(){}
}
