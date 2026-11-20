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
                new int[]{0xe6383942,0xd91a1b24,0xe6111219});
        d.setCornerRadius(radius*context.getResources().getDisplayMetrics().density);
        d.setStroke(Math.max(1,Math.round(context.getResources().getDisplayMetrics().density)),0x38ffffff);
        return d;
    }
    static Drawable action(Context context,int radius){
        return new RippleDrawable(ColorStateList.valueOf(0x28ffffff),panel(context,radius),null);
    }
    static GradientDrawable backdrop(){
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xff261018,0xff101118,0xff08090d,0xff08090d});
    }
    private GlassTheme(){}
}
