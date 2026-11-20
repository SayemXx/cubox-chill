package app.moviebox.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Bounded snapshot blur: works even when Android disables cross-window blur. */
final class FrostedDialog extends AlertDialog.Builder {
    private final Activity activity;
    FrostedDialog(Activity activity){super(activity);this.activity=activity;}
    @Override public AlertDialog show(){
        View behind=activity.getWindow().getDecorView();
        Bitmap snapshot=null;int[] origin=new int[2];behind.getLocationOnScreen(origin);
        int width=behind.getWidth(),height=behind.getHeight();
        if(width>0&&height>0){
            float scale=Math.min(1f,240f/Math.max(width,height));
            snapshot=Bitmap.createBitmap(Math.max(1,Math.round(width*scale)),Math.max(1,Math.round(height*scale)),Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(snapshot);canvas.drawColor(Color.BLACK);canvas.scale(scale,scale);behind.draw(canvas);
            soften(snapshot);
        }
        AlertDialog dialog=super.create();dialog.show();
        if(snapshot!=null&&dialog.getWindow()!=null){
            View decor=dialog.getWindow().getDecorView();
            Drawable overlay=activity.getDrawable(R.drawable.glass_dialog);
            dialog.getWindow().setBackgroundDrawable(new Surface(snapshot,overlay,decor,origin,width,height,activity.getResources().getDisplayMetrics().density));
        }
        return dialog;
    }
    private static void soften(Bitmap bitmap){
        int w=bitmap.getWidth(),h=bitmap.getHeight();int[] pixels=new int[w*h],temp=new int[w*h];
        bitmap.getPixels(pixels,0,w,0,0,w,h);
        // Two small separable box passes on a <=240px snapshot keep work bounded.
        for(int pass=0;pass<2;pass++){
            for(int y=0;y<h;y++)for(int x=0;x<w;x++){
                int r=0,g=0,b=0;
                for(int k=-3;k<=3;k++){int c=pixels[y*w+Math.max(0,Math.min(w-1,x+k))];r+=(c>>16)&255;g+=(c>>8)&255;b+=c&255;}
                temp[y*w+x]=Color.rgb(r/7,g/7,b/7);
            }
            for(int y=0;y<h;y++)for(int x=0;x<w;x++){
                int r=0,g=0,b=0;
                for(int k=-3;k<=3;k++){int c=temp[Math.max(0,Math.min(h-1,y+k))*w+x];r+=(c>>16)&255;g+=(c>>8)&255;b+=c&255;}
                pixels[y*w+x]=Color.rgb(r/7,g/7,b/7);
            }
        }
        bitmap.setPixels(pixels,0,w,0,0,w,h);
    }
    private static final class Surface extends Drawable {
        final Bitmap image;final Drawable overlay;final View decor;final int[] origin;final int width,height;final float density;
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        Surface(Bitmap image,Drawable overlay,View decor,int[] origin,int width,int height,float density){this.image=image;this.overlay=overlay;this.decor=decor;this.origin=origin;this.width=width;this.height=height;this.density=density;}
        @Override public void draw(Canvas canvas){
            Rect bounds=getBounds();int[] location=new int[2];decor.getLocationOnScreen(location);
            float inset=8*density;RectF panel=new RectF(bounds.left+inset,bounds.top+inset,bounds.right-inset,bounds.bottom-inset);
            Path clip=new Path();clip.addRoundRect(panel,32*density,32*density,Path.Direction.CW);
            int saved=canvas.save();canvas.clipPath(clip);
            float left=origin[0]-location[0],top=origin[1]-location[1];
            canvas.drawBitmap(image,null,new RectF(left,top,left+width,top+height),paint);
            canvas.restoreToCount(saved);overlay.setBounds(bounds);overlay.draw(canvas);
        }
        @Override public boolean getPadding(Rect padding){return overlay.getPadding(padding);}
        @Override public void getOutline(Outline outline){overlay.setBounds(getBounds());overlay.getOutline(outline);}
        @Override public void setAlpha(int alpha){paint.setAlpha(alpha);overlay.setAlpha(alpha);invalidateSelf();}
        @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
