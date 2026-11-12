package app.moviebox.mobile;
final class VerticalGestureZone {
    static int at(float startX,float width){
        if(width<=0||startX<0||startX>width)return 0;
        if(startX<width*.25f)return -1;
        if(startX>width*.75f)return 1;
        return 0;
    }
}
