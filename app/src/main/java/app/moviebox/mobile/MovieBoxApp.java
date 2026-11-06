package app.moviebox.mobile;

import android.app.Application;

public class MovieBoxApp extends Application {
    @Override public void onCreate() { super.onCreate(); Offline.initialize(this); }
    @Override public void onTrimMemory(int level){super.onTrimMemory(level);if(level>=TRIM_MEMORY_UI_HIDDEN)Posters.trimMemory();}
}
