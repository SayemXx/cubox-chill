package app.moviebox.mobile;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.*;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.Drawable;

/** Frosted app surfaces; dialog windows additionally use system blur on supported Android 12+ devices. */
final class GlassTheme {
    static GradientDrawable panel(Context context,int radius){
        float density=context.getResources().getDisplayMetrics().density;
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0x603f424a,0x3020242b,0x70101218}){
            private final Paint rim=new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override public void draw(Canvas canvas){
                super.draw(canvas);
                Rect bounds=getBounds();float inset=density*.75f;
                rim.setStyle(Paint.Style.STROKE);rim.setStrokeWidth(density);
                rim.setShader(new LinearGradient(bounds.left,bounds.top,bounds.right,bounds.bottom,
                    new int[]{0x99ffffff,0x18ffffff,0x08ffffff,0x60dce5f5},
                    new float[]{0f,.35f,.65f,1f},Shader.TileMode.CLAMP));
                canvas.drawRoundRect(new RectF(bounds.left+inset,bounds.top+inset,bounds.right-inset,bounds.bottom-inset),
                    radius*density,radius*density,rim);
            }
        };
        d.setCornerRadius(radius*density);
        return d;
    }
    static void brand(android.widget.TextView label){
        label.setTextColor(Color.WHITE);
        // Older Android GPUs cannot compose two linear gradients; only this small label
        // needs a software layer on Android 8/8.1, not the surrounding app or player.
        if(android.os.Build.VERSION.SDK_INT<28)label.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE,null);
        label.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            float width=label.getPaint().measureText(label.getText().toString());
            Shader base=new LinearGradient(0,0,Math.max(1,width),0,
                new int[]{label.getContext().getColor(R.color.brand_rose),
                    label.getContext().getColor(R.color.brand_burgundy),
                    label.getContext().getColor(R.color.brand_wine)},null,Shader.TileMode.CLAMP);
            Paint.FontMetrics fm=label.getPaint().getFontMetrics();
            float top=label.getBaseline()-label.getTotalPaddingTop()+fm.ascent;
            Shader sheen=new LinearGradient(0,top,0,top+Math.max(1,fm.descent-fm.ascent),
                new int[]{0x60ffe5ec,0x18ffe5ec,0x00ffe5ec,0x20ffe5ec},
                new float[]{0f,.38f,.55f,1f},Shader.TileMode.CLAMP);
            label.getPaint().setShader(new ComposeShader(base,sheen,PorterDuff.Mode.SRC_OVER));
            label.invalidate();
        });
    }
    static GradientDrawable accent(Context context,int radius){
        GradientDrawable d=panel(context,radius);
        d.setColors(new int[]{0xb8b93246,0x90500e1b,0xc0250b12});
        return d;
    }
    static Drawable primary(Context context,int radius){
        return new RippleDrawable(ColorStateList.valueOf(0x38ffffff),accent(context,radius),null);
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
