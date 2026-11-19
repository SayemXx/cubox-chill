package app.moviebox.mobile;
import android.app.*;
import android.os.Bundle;
import android.widget.*;
/** Exercises TV focus setup directly, including on a non-TV emulator. */
public class TvFocusInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();Throwable[] failure={null}; MainActivity activity=(MainActivity)startActivitySync(new android.content.Intent(getTargetContext(),MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)); runOnMainSync(()->{
        try{
            LinearLayout root=new LinearLayout(getTargetContext());TextView button=new TextView(getTargetContext());button.setText("TV test");button.setOnClickListener(v->{});root.addView(button);
            TvSupport.focus(root);TvSupport.focus(root);
            int[] clicks={0};button.setOnClickListener(v->clicks[0]++);
            for(int key:new int[]{23,66,160}){
                TvSupport.activate(button,key,new android.view.KeyEvent(0,key));
                TvSupport.activate(button,key,new android.view.KeyEvent(1,key));
            }
            if(clicks[0]!=3)throw new AssertionError("Remote activation count: "+clicks[0]);
                        activity.tvShell("browse");
            LinearLayout tile=new LinearLayout(activity);TextView child=new TextView(activity);child.setText("Movie fixture");child.setFocusableInTouchMode(true);tile.addView(child);activity.body.addView(tile);
            int[] opened={0};tile.setOnClickListener(v->opened[0]++);
            child.requestFocus();
            for(int code:new int[]{23,66,160,96,109}){
                activity.dispatchKeyEvent(new android.view.KeyEvent(0,code));
                tile.setPressed(false);
                activity.dispatchKeyEvent(new android.view.KeyEvent(1,code));
            }
            if(opened[0]!=5)throw new AssertionError("Activity did not activate focused poster parent: "+opened[0]);
            android.view.View updates=activity.nav.getChildAt(3);updates.setFocusableInTouchMode(true);updates.requestFocus();
            activity.dispatchKeyEvent(new android.view.KeyEvent(0,23));activity.dispatchKeyEvent(new android.view.KeyEvent(1,23));
            if(!activity.screen.equals("updates"))throw new AssertionError("Remote did not open Updates");
            activity.finish();
            button.setEnabled(false);
            if(TvSupport.activate(button,23,new android.view.KeyEvent(0,23)))throw new AssertionError("Disabled control consumed OK");
            button.setEnabled(true);
            if(!button.isFocusable()||button.getTag(R.id.tv_focus_initialized)==null)throw new AssertionError("Focus not initialized");
            button.getOnFocusChangeListener().onFocusChange(button,true);
            if(button.getForeground()==null)throw new AssertionError("Focus highlight missing");
            button.getOnFocusChangeListener().onFocusChange(button,false);
            if(button.getForeground()!=null)throw new AssertionError("Highlight not cleared");
        }catch(Throwable e){failure[0]=e;}
    });result.putString("stream",failure[0]==null?"PASS: TV focus initialization, repeated setup, highlight and blur":"FAIL: "+failure[0]);finish(failure[0]==null?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);}
}
