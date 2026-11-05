package app.moviebox.mobile;
final class Versions {
    static boolean newer(String candidate,String current){
        String a=candidate.replaceFirst("^[vV]", ""),b=current.replaceFirst("^[vV]", "");
        String[] aa=a.split("[.+-]"),bb=b.split("[.+-]");
        try {for(int i=0;i<3;i++){int x=i<aa.length?Integer.parseInt(aa[i]):0,y=i<bb.length?Integer.parseInt(bb[i]):0;if(x!=y)return x>y;}}
        catch(NumberFormatException e){return false;}
        return b.contains("-")&&!a.contains("-");
    }
    static boolean validRepository(String s){return s.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+");}
}
