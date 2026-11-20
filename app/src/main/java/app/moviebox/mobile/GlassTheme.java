package app.moviebox.mobile;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.Drawable;

/** Frosted app surfaces; dialog windows additionally use system blur on supported Android 12+ devices. */
final class GlassTheme {
    static GradientDrawable panel(Context context,int radius){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xc0444752,0xc8242730,0xd01b1d24});
        d.setCornerRadius(radius*context.getResources().getDisplayMetrics().density);
        d.setStroke(Math.max(1,Math.round(context.getResources().getDisplayMetrics().density)),0x66ffffff);
        return d;
    }
    static Drawable action(Context context,int radius){
        return new RippleDrawable(ColorStateList.valueOf(0x28ffffff),panel(context,radius),null);
    }
    static GradientDrawable backdrop(){
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xff141414,0xff101010,0xff080808,0xff080808});
    }
    private GlassTheme(){}
}
