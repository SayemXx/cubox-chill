package app.moviebox.mobile;
/** A tap burst is local to one screen side; the first tap never seeks. */
final class TapSeek {
    static final long WINDOW_MS=350;
    long last=-1; boolean forward; int taps;
    int tap(boolean nextForward,long time){
        if(last<0||time-last>WINDOW_MS||time<last||nextForward!=forward)taps=0;
        last=time;forward=nextForward;taps++;
        return Math.max(0,taps-1)*10;
    }
    void reset(){last=-1;taps=0;}
}
