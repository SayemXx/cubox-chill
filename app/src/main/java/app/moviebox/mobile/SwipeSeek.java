package app.moviebox.mobile;
final class SwipeSeek {
    static long target(long start,long duration,float distance,float width){
        long target=Math.max(0,start+Math.round(distance/Math.max(1f,width)*120000d));
        return duration>0?Math.min(duration,target):target;
    }
}
