package app.moviebox.mobile;
final class TvLayout {
    static float scale(int width,int height){return Math.max(.8f,Math.min(1.6f,Math.min(width/960f,height/540f)));}
    static int icon(int width,int height){return Math.round(28*scale(width,height));}
    static int poster(int width,int height){return Math.round(132*scale(width,height));}
    static int hero(int width,int height){return Math.round(280*scale(width,height));}
}
