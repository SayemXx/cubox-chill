package app.moviebox.mobile;

import android.app.Application;

public class MovieBoxApp extends Application {
    @Override public void onCreate() { super.onCreate(); Offline.initialize(this); }
}
