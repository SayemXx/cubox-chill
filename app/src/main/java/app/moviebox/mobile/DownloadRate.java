package app.moviebox.mobile;

/** Smoothed transfer rate; samples use a monotonic clock and exclude cached bytes on resume. */
final class DownloadRate {
    private long time=-1,bytes;
    private double speed;
    double sample(long now,long downloaded){
        if(time<0||downloaded<bytes||now<=time){time=now;bytes=downloaded;speed=0;return 0;}
        long elapsed=now-time;
        if(elapsed<500)return speed;
        double current=(downloaded-bytes)*1000.0/elapsed;
        speed=speed==0?current:speed*.5+current*.5;
        time=now;bytes=downloaded;return speed;
    }
}
