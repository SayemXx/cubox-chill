package app.moviebox.mobile;
import android.app.*;
import android.os.Bundle;
import android.widget.*;
/** Exercises TV focus setup directly, including on a non-TV emulator. */
public class TvFocusInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();Throwable[] failure={null};runOnMainSync(()->{
        try{
            LinearLayout root=new LinearLayout(getTargetContext());TextView button=new TextView(getTargetContext());button.setText("TV test");button.setOnClickListener(v->{});root.addView(button);
            TvSupport.focus(root);TvSupport.focus(root);
            if(!button.isFocusable()||button.getTag(R.id.tv_focus_initialized)==null)throw new AssertionError("Focus not initialized");
            button.getOnFocusChangeListener().onFocusChange(button,true);
            if(button.getForeground()==null)throw new AssertionError("Focus highlight missing");
            button.getOnFocusChangeListener().onFocusChange(button,false);
            if(button.getForeground()!=null)throw new AssertionError("Highlight not cleared");
        }catch(Throwable e){failure[0]=e;}
    });result.putString("stream",failure[0]==null?"PASS: TV focus initialization, repeated setup, highlight and blur":"FAIL: "+failure[0]);finish(failure[0]==null?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);}
}
