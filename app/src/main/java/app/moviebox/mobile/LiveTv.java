package app.moviebox.mobile;

import android.app.AlertDialog;
import android.content.*;
import android.net.Uri;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.*;

/** Live TV screens share the app shell and the existing remote focus behavior. */
final class LiveTv {
    static final int PICK_FILE=701;
    final MainActivity a;final LiveTvStore store;
    String category="BD",query="",pendingName="Imported playlist",pendingCategory="BD";
    LiveTvStore.Source source;LiveTvStore.Listing listing;
    TextView status;LinearLayout rows;int requestToken,limit=50;boolean onlyFavorites;
    LiveTv(MainActivity activity){a=activity;store=new LiveTvStore(a);category=a.getSharedPreferences("live-tv",0).getString("category","BD");if(!LiveTvStore.CATEGORIES.contains(category))category="BD";}
    boolean visible(int token){return token==requestToken&&a.screen.equals("live")&&!a.isFinishing()&&!a.isDestroyed();}
    void show(){
        requestToken++;a.shell("live");a.body.addView(a.heading("Live TV",30));
        HorizontalScrollView strip=new HorizontalScrollView(a);strip.setHorizontalScrollBarEnabled(true);LinearLayout tabs=new LinearLayout(a);
        List<String> groups=new ArrayList<>(LiveTvStore.CATEGORIES);groups.add("+ Add new");
        for(String group:groups){
            Button tab=a.secondary(group,()->{if(group.startsWith("+")){addDialog();return;}category=group;source=null;listing=null;query="";onlyFavorites=false;a.getSharedPreferences("live-tv",0).edit().putString("category",category).apply();show();});
            tab.setBackground(group.equals(category)?GlassTheme.accent(a,24):GlassTheme.panel(a,24));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,a.dp(44));p.setMargins(0,0,a.dp(8),0);tabs.addView(tab,p);
            if(group.equals(category))strip.post(()->strip.scrollTo(Math.max(0,tab.getLeft()-a.dp(8)),0));
        }
        strip.addView(tabs);a.body.addView(strip);
        List<LiveTvStore.Source> sources=store.sources(category);
        if(source==null||!source.category.equals(category)){
            String selected=a.getSharedPreferences("live-tv",0).getString("source-"+category,"");source=sources.get(0);for(LiveTvStore.Source s:sources)if(s.id.equals(selected))source=s;
        }
        a.body.addView(a.secondary(source.name+"  ▾",this::chooseSource));
        a.body.addView(a.text(source.id.startsWith("iptv-")?"Community playlist · IPTV-org on GitHub. Channel availability and regional restrictions vary.":source.id.startsWith("official-")?"Selected broadcaster feeds and official viewing pages.":"Your playlist · URLs are saved on this device.",12,a.MUTED));
        EditText search=new EditText(a);search.setSingleLine();search.setTextColor(a.INK);search.setHintTextColor(a.MUTED);search.setHint("Search channels");search.setText(query);search.setTextSize(16);search.setPadding(a.dp(16),a.dp(8),a.dp(16),a.dp(8));search.setBackground(GlassTheme.panel(a,22));a.body.addView(search,new LinearLayout.LayoutParams(-1,a.dp(48)));
        LinearLayout controls=new LinearLayout(a);
        controls.addView(a.secondary(onlyFavorites?"★ Favorites":"☆ All channels",()->{onlyFavorites=!onlyFavorites;show();}),new LinearLayout.LayoutParams(0,a.dp(48),1));
        controls.addView(a.secondary("Refresh",()->load(true)),new LinearLayout.LayoutParams(0,a.dp(48),1));a.body.addView(controls);
        status=a.text("Loading playlist…",12,a.MUTED);a.body.addView(status);rows=a.column();a.body.addView(rows);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int after){}public void onTextChanged(CharSequence s,int st,int before,int c){query=s.toString();limit=50;render();}public void afterTextChanged(Editable e){}});
        if(listing!=null){render();status.setText(listing.channels.size()+" channels · "+source.name);}else load(false);
        if(a.tvUi)TvSupport.focus(a.root);
    }
    void chooseSource(){
        List<LiveTvStore.Source> sources=store.sources(category);String[] names=new String[sources.size()];int selected=0;
        for(int i=0;i<sources.size();i++){names[i]=sources.get(i).name;if(sources.get(i).id.equals(source.id))selected=i;}
        AlertDialog dialog=new FrostedDialog(a).setTitle("Choose playlist").setSingleChoiceItems(names,selected,(d,n)->{source=sources.get(n);listing=null;limit=50;a.getSharedPreferences("live-tv",0).edit().putString("source-"+category,source.id).apply();d.dismiss();show();}).setNegativeButton("Close",null)
            .setNeutralButton(source.builtIn?"Source website":"Remove playlist",(d,n)->{
                if(source.builtIn){a.openUrl(source.id.startsWith("iptv-")?"https://github.com/iptv-org/iptv":"https://github.com/SayemXx/cubox-chill/blob/main/LIVE_TV_SOURCES.md");}
                else new FrostedDialog(a).setTitle("Remove playlist?").setMessage(source.name).setPositiveButton("Remove",(x,y)->{try{store.remove(source);source=null;listing=null;show();}catch(Exception e){a.error("Could not remove playlist");}}).setNegativeButton("Keep",null).show();
            }).show();
    }
    void load(boolean force){
        final int token=++requestToken;final LiveTvStore.Source selected=source;limit=50;
        status.setText(force?"Refreshing playlist…":"Loading playlist…");
        a.io.execute(()->{
            LiveTvStore.Listing cached=null;try{cached=store.cached(selected);}catch(Exception ignored){}
            final LiveTvStore.Listing saved=cached;
            if(saved!=null)a.main.post(()->{if(visible(token)){listing=saved;render();status.setText(saved.channels.size()+" channels · Saved playlist");}});
            if(!force&&saved!=null&&(selected.url.isEmpty()||System.currentTimeMillis()-saved.updated<LiveTvStore.FRESH_MS))return;
            try{LiveTvStore.Listing fresh=store.refresh(selected);a.main.post(()->{if(visible(token)){listing=fresh;render();status.setText(fresh.channels.size()+" channels · Updated just now");}});}
            catch(Exception e){a.main.post(()->{if(visible(token)){status.setText(saved==null?"Could not load playlist. Check your connection or choose another source.":"Using saved playlist · Refresh unavailable right now");if(saved==null){listing=null;rows.removeAllViews();}}});}
        });
    }
    void render(){
        if(rows==null||listing==null)return;rows.removeAllViews();Set<String> favorites=store.favorites();List<M3uPlaylist.Channel> filtered=new ArrayList<>();
        String needle=query.trim().toLowerCase(Locale.ROOT);
        for(M3uPlaylist.Channel c:listing.channels)if((!onlyFavorites||favorites.contains(LiveTvStore.hash(c.url)))&&(c.name+" "+c.group).toLowerCase(Locale.ROOT).contains(needle))filtered.add(c);
        if(filtered.isEmpty()){rows.addView(a.text(onlyFavorites?"No favorites in this playlist yet. Use ☆ beside a channel.":"No matching channels.",15,a.MUTED));return;}
        if(limit>50)rows.addView(a.secondary("Previous channels",()->{limit-=50;render();}));
        for(int i=Math.max(0,limit-50);i<Math.min(limit,filtered.size());i++){
            M3uPlaylist.Channel c=filtered.get(i);LinearLayout card=new LinearLayout(a);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(a.dp(8),a.dp(6),a.dp(8),a.dp(6));card.setBackground(GlassTheme.panel(a,18));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,a.dp(6),0,a.dp(4));rows.addView(card,cp);
            ImageView logo=new ImageView(a);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);logo.setImageResource(R.drawable.ic_live_tv);card.addView(logo,new LinearLayout.LayoutParams(a.dp(44),a.dp(44)));if(!c.logo.isEmpty())Posters.load(logo,c.logo);
            Button play=a.secondary(c.name+"\n"+(c.external?"Official website ↗":c.group.isEmpty()?"Watch live":c.group),()->play(c));
            // Keep the channel label on the outer card; only show feedback while pressed or TV-focused.
            play.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x18ffffff),null,new android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE)));
            play.setStateListAnimator(null);play.setElevation(0);
            play.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);play.setTextSize(14);play.setPadding(a.dp(12),a.dp(6),a.dp(6),a.dp(6));play.setMaxLines(3);play.setMinHeight(a.dp(64));card.addView(play,new LinearLayout.LayoutParams(0,-2,1));
            Button favorite=a.secondary(favorites.contains(LiveTvStore.hash(c.url))?"★":"☆",()->{store.favorite(c);render();});favorite.setTextSize(22);favorite.setTextColor(favorites.contains(LiveTvStore.hash(c.url))?0xff6D061E:a.INK);favorite.setPadding(0,0,0,0);favorite.setIncludeFontPadding(false);favorite.setGravity(Gravity.CENTER);favorite.setStateListAnimator(null);favorite.setElevation(0);favorite.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x18ffffff),null,new android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE)));favorite.setContentDescription((favorites.contains(LiveTvStore.hash(c.url))?"Remove favorite ":"Favorite ")+c.name);card.addView(favorite,new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));
        }
        if(filtered.size()>limit)rows.addView(a.secondary("Next channels ("+Math.min(limit,filtered.size())+" / "+filtered.size()+")",()->{limit+=50;render();}));
        if(a.tvUi)TvSupport.focus(rows);
    }
    void play(M3uPlaylist.Channel c){
        if(c.external){a.openUrl(c.url);return;}
        try{JSONArray headers=new JSONArray();for(Map.Entry<String,String> h:c.headers.entrySet())headers.put(new JSONArray().put(h.getKey()).put(h.getValue()));
            JSONObject source=new JSONObject().put("url",c.url).put("headers",headers).put("live",true);
            a.startActivity(new Intent(a,PlayerActivity.class).putExtra("source",source.toString()).putExtra("title",c.name+" · LIVE").putExtra("key","live:"+LiveTvStore.hash(c.url)).putExtra("live",true));
        }catch(JSONException e){a.error("Could not open this channel");}
    }
    void addDialog(){
        LinearLayout form=a.column();form.setPadding(a.dp(20),a.dp(8),a.dp(20),0);
        EditText name=new EditText(a);name.setSingleLine();name.setHint("Playlist name");name.setTextColor(a.INK);name.setHintTextColor(a.MUTED);form.addView(name);
        EditText url=new EditText(a);url.setSingleLine();url.setHint("https://…/playlist.m3u");url.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);url.setTextColor(a.INK);url.setHintTextColor(a.MUTED);form.addView(url);
        TextView hint=a.text("Add to "+category+". Paste an M3U playlist URL or import a file. Use sources you are entitled to watch.",13,a.MUTED);form.addView(hint);
        AlertDialog dialog=new FrostedDialog(a).setTitle("Add playlist").setView(form).setPositiveButton("Add URL",null).setNegativeButton("Cancel",null).setNeutralButton("Import file",(d,n)->{
            pendingName=name.getText().toString().trim();if(pendingName.isEmpty())pendingName="Imported playlist";pendingCategory=category;
            Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
            try{a.startActivityForResult(pick,PICK_FILE);}catch(ActivityNotFoundException e){a.toast("No file picker on this device. Add a playlist URL instead.");}
        }).show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String link=LiveTvStore.normalize(url.getText().toString());if(!M3uPlaylist.web(link)){url.setError("Enter an HTTP or HTTPS playlist URL");return;}
            String title=name.getText().toString().trim();if(title.isEmpty())title="My playlist";
            LiveTvStore.Source added=new LiveTvStore.Source(UUID.randomUUID().toString(),title,link,category,false);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);hint.setText("Checking playlist…");
            a.io.execute(()->{try{LiveTvStore.Fetched raw=LiveTvStore.fetch(link);store.add(added,raw.text,raw.base);a.main.post(()->{if(!a.isFinishing()&&!a.isDestroyed()){dialog.dismiss();if(a.screen.equals("live")){category=added.category;source=added;listing=null;show();}}});}catch(Exception e){a.main.post(()->{if(dialog.isShowing()){hint.setText("Could not import. Check the URL and use a valid M3U channel list (up to 5 MB).");dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);}});}});
        });
        if(a.tvUi)TvSupport.focus(form);
    }
    void importFile(Uri uri){
        LiveTvStore.Source added=new LiveTvStore.Source(UUID.randomUUID().toString(),pendingName,"",pendingCategory,false);a.toast("Importing playlist…");
        a.io.execute(()->{try(InputStream in=a.getContentResolver().openInputStream(uri)){if(in==null)throw new IOException();String raw=LiveTvStore.read(in);store.add(added,raw,"");a.main.post(()->{if(!a.isFinishing()&&!a.isDestroyed()){category=added.category;source=added;listing=null;show();}});}catch(Exception e){a.main.post(()->{if(!a.isFinishing()&&!a.isDestroyed())a.error("Could not import this file. Choose a valid M3U channel list up to 5 MB.");});}});
    }
}
