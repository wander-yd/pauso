package com.andrew.hourpause;
import android.app.*;import android.content.*;import android.content.res.ColorStateList;import android.graphics.Color;import android.graphics.drawable.GradientDrawable;import android.os.*;import android.text.*;import android.view.*;import android.widget.*;import java.util.*;import java.util.concurrent.*;
/** A single, virtualized music sheet. Library queries never run on the UI thread. */
public final class MusicDialog extends Dialog {
 public interface Host {void permission();void files();void selected();default void play(){selected();}}
 private final Activity activity;private final Host host;private final MusicStore store;private final Handler main=new Handler(Looper.getMainLooper());
 private final ExecutorService worker=Executors.newSingleThreadExecutor();private boolean closed;private int scanGeneration;
 private static final int BG=0xff09120d,CARD=0xff223a30,INK=0xfff4efe4,MUTED=0xffa7ac98,GOLD=0xffdfd3b3;
 private LinearLayout body,actions;private TextView subtitle;private EditText search;private ListView list;private Button phoneTab,playlistTab,addSelected;
 private final ArrayList<Track> device=new ArrayList<>(),shown=new ArrayList<>();private final LinkedHashSet<String> selected=new LinkedHashSet<>();
 private final boolean embedded;private boolean selectionMode;private LinearLayout contextActions;private ImageButton selectAllButton;
 private boolean playlistsTab;private String playlistId;private BaseAdapter adapter;private int dragged=-1;
 public MusicDialog(Activity a,Host h){this(a,h,false);}
 public MusicDialog(Activity a,Host h,boolean e){super(a);activity=a;host=h;embedded=e;store=new MusicStore(a);}
 public View embeddedView(){if(body==null)buildContent();return body;}
 public boolean isPlaylists(){return playlistsTab;}
 public void activate(boolean playlists){if(body==null)buildContent();if(playlistsTab!=playlists){playlistId=null;selected.clear();selectionMode=false;search.setText("");}playlistsTab=playlists;render();if(!playlists&&device.isEmpty())scan();}
 public boolean navigateBack(){if(selectionMode){selectionMode=false;selected.clear();render();return true;}if(playlistId!=null){playlistId=null;search.setText("");render();return true;}return false;}
 private int dp(int n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
 private TextView text(String s,int size,int color){TextView v=new TextView(activity);v.setText(s);v.setTextSize(size);v.setTextColor(color);return v;}
 private Button button(String s,Runnable action){Button b=new Button(activity);b.setText(s);b.setAllCaps(false);b.setTextColor(INK);b.setTextSize(13);b.setMinHeight(dp(48));GradientDrawable bg=new GradientDrawable();bg.setColor(CARD);bg.setCornerRadius(dp(12));b.setBackground(bg);b.setOnClickListener(v->action.run());return b;}
 private void rowButton(LinearLayout row,Button b){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),1);lp.setMargins(dp(3),dp(3),dp(3),dp(3));row.addView(b,lp);}
 @Override protected void onCreate(Bundle state){super.onCreate(state);buildContent();}
 private void buildContent(){if(!embedded)requestWindowFeature(Window.FEATURE_NO_TITLE);
  body=new LinearLayout(activity);body.setOrientation(1);body.setPadding(dp(embedded?0:16),dp(embedded?0:12),dp(embedded?0:16),dp(8));body.setBackgroundColor(BG);
  LinearLayout top=new LinearLayout(activity);top.setGravity(Gravity.CENTER_VERTICAL);TextView title=text("My Audio",22,INK);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));top.addView(button("Close",()->dismiss()),new LinearLayout.LayoutParams(dp(72),dp(48)));if(!embedded)body.addView(top);
  LinearLayout tabs=new LinearLayout(activity);phoneTab=button("Phone music",()->{playlistId=null;playlistsTab=false;selected.clear();selectionMode=false;render();if(device.isEmpty())scan();});playlistTab=button("Playlists",()->{playlistId=null;playlistsTab=true;selected.clear();selectionMode=false;render();});rowButton(tabs,phoneTab);rowButton(tabs,playlistTab);if(!embedded)body.addView(tabs);
  LinearLayout context=new LinearLayout(activity);context.setGravity(Gravity.CENTER_VERTICAL);subtitle=text("",13,MUTED);subtitle.setSingleLine();subtitle.setEllipsize(TextUtils.TruncateAt.END);context.addView(subtitle,new LinearLayout.LayoutParams(0,dp(48),1));contextActions=new LinearLayout(activity);context.addView(contextActions);body.addView(context);
  search=new EditText(activity);search.setSingleLine();search.setHint("Search title or artist");search.setTextColor(INK);search.setHintTextColor(MUTED);search.setTextSize(15);search.setBackgroundTintList(ColorStateList.valueOf(GOLD));body.addView(search,new LinearLayout.LayoutParams(-1,dp(48)));
  search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int n,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){filter();}public void afterTextChanged(Editable e){}});
  list=new ListView(activity);list.setDividerHeight(dp(1));list.setDivider(new android.graphics.drawable.ColorDrawable(CARD));body.addView(list,new LinearLayout.LayoutParams(-1,0,1));
  adapter=new BaseAdapter(){public int getCount(){return playlistsTab&&playlistId==null?store.state().playlists.size():shown.size();}public Object getItem(int i){return playlistsTab&&playlistId==null?store.state().playlists.get(i):shown.get(i);}public long getItemId(int i){return i;}
   public View getView(int i,View reused,android.view.ViewGroup parent){
    LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(4),dp(6),dp(4),dp(6));
    LinearLayout words=new LinearLayout(activity);words.setOrientation(1);row.addView(words,new LinearLayout.LayoutParams(0,-2,1));
    if(playlistsTab&&playlistId==null){LibraryState.Playlist p=store.state().playlists.get(i);words.addView(text(p.name,16,INK));words.addView(text(p.tracks.size()+" tracks",12,MUTED));Button more=button("⋮",()->playlistOptions(p));more.setFocusable(false);more.setContentDescription("Manage "+p.name);row.addView(more,new LinearLayout.LayoutParams(dp(48),dp(48)));}
    else{Track t=shown.get(i);TextView title=text((t.missing?"Unavailable · ":"")+t.title,15,t.missing?MUTED:INK);title.setMaxLines(2);title.setEllipsize(TextUtils.TruncateAt.END);title.setContentDescription((selected.contains(t.uri)?"Selected, ":"")+(t.missing?"Unavailable, ":"")+t.title);words.addView(title);
     words.addView(text((t.artist.isEmpty()?"Audio":t.artist)+(t.duration>0?" · "+String.format(Locale.getDefault(),"%d:%02d",t.duration/60000,t.duration/1000%60):""),12,MUTED));
     if(playlistId!=null&&!selectionMode){Button handle=button("≡",()->trackOptions(t));handle.setFocusable(false);handle.setContentDescription("Reorder or remove "+t.title+"; hold to drag");handle.setOnLongClickListener(v->{LibraryState.Playlist p=store.state().find(playlistId);dragged=p==null?-1:p.tracks.indexOf(t);if(dragged<0||search.length()>0){toast("Clear search before reordering.");return true;}v.startDragAndDrop(ClipData.newPlainText("Pauso track","reorder"),new View.DragShadowBuilder(v),MusicDialog.this,0);return true;});row.addView(handle,new LinearLayout.LayoutParams(dp(48),dp(48)));}
     else if(selectionMode){TextView check=text(selected.contains(t.uri)?"✓":"○",20,GOLD);check.setPadding(dp(8),0,dp(8),0);row.addView(check);}
    }return row;
   }};list.setAdapter(adapter);
  list.setOnItemClickListener((p,v,i,id)->{if(playlistsTab&&playlistId==null){playlistId=store.state().playlists.get(i).id;search.setText("");render();return;}Track t=shown.get(i);if(selectionMode){toggle(t);return;}ArrayList<Track> queue=new ArrayList<>(shown);store.select(queue,i);host.selected();if(!embedded)dismiss();});
  list.setOnItemLongClickListener((p,v,i,id)->{if(playlistsTab&&playlistId==null)playlistOptions(store.state().playlists.get(i));else{selectionMode=true;toggle(shown.get(i));}return true;});
  list.setOnDragListener((v,event)->{
   if(event.getLocalState()!=MusicDialog.this)return false;
   if(event.getAction()==DragEvent.ACTION_DRAG_STARTED)return playlistId!=null&&search.length()==0;
   if(event.getAction()==DragEvent.ACTION_DRAG_LOCATION){if(event.getY()<dp(48))list.smoothScrollBy(-dp(32),100);else if(event.getY()>list.getHeight()-dp(48))list.smoothScrollBy(dp(32),100);return true;}
   if(event.getAction()==DragEvent.ACTION_DROP){LibraryState.Playlist p=store.state().find(playlistId);int target=list.pointToPosition((int)event.getX(),(int)event.getY());if(p!=null&&dragged>=0&&dragged<p.tracks.size()&&target>=0&&target<p.tracks.size()){Track t=p.tracks.remove(dragged);p.tracks.add(target,t);store.save();filter();}return true;}
   if(event.getAction()==DragEvent.ACTION_DRAG_ENDED)dragged=-1;return true;
  });
  actions=new LinearLayout(activity);actions.setOrientation(1);body.addView(actions);if(!embedded){setContentView(body);getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(BG));}render();if(!embedded)scan();
 }
 @Override public void show(){super.show();getWindow().setLayout(-1,(int)(activity.getResources().getDisplayMetrics().heightPixels*.88));}
 @Override public void dismiss(){closed=true;scanGeneration++;worker.shutdownNow();main.removeCallbacksAndMessages(null);super.dismiss();}
 @Override public void onBackPressed(){if(!navigateBack())super.onBackPressed();}
 private void toast(String s){Toast.makeText(activity,s,Toast.LENGTH_SHORT).show();}
 private void toggle(Track t){if(!selected.add(t.uri))selected.remove(t.uri);render();}
 private void filter(){shown.clear();List<Track> source=playlistId==null?device:store.state().find(playlistId)==null?Collections.emptyList():store.state().find(playlistId).tracks;String needle=search.getText().toString().trim().toLowerCase(Locale.ROOT);for(Track t:source)if((t.title+" "+t.artist).toLowerCase(Locale.ROOT).contains(needle))shown.add(t);if(adapter!=null)adapter.notifyDataSetChanged();updateSelectionIcon();}
 private ImageButton icon(int resource,String label,Runnable action){ImageButton b=new ImageButton(activity);b.setImageResource(resource);b.setImageTintList(ColorStateList.valueOf(GOLD));b.setBackgroundColor(BG);b.setPadding(dp(10),dp(10),dp(10),dp(10));b.setContentDescription(label);b.setOnClickListener(v->action.run());return b;}
 private void tool(int resource,String label,Runnable action){contextActions.addView(icon(resource,label,action),new LinearLayout.LayoutParams(dp(40),dp(48)));}
 private void selectionTools(){
  contextActions.removeAllViews();selectAllButton=null;
  if(playlistsTab&&playlistId==null)return;
  if(selectionMode){tool(R.drawable.ic_close,"Cancel selection",()->{selected.clear();selectionMode=false;render();});tool(playlistId==null?R.drawable.ui_add:R.drawable.ui_trash,playlistId==null?"Add to playlist":"Remove from playlist",()->{if(selected.isEmpty())return;if(playlistId==null)addSelectedTracks();else removeSelectedTracks();});tool(R.drawable.ui_more,"More selection options",()->selectionOptions());}
  selectAllButton=icon(R.drawable.ui_check_empty,"Select all",()->{boolean all=!shown.isEmpty();for(Track t:shown)all&=selected.contains(t.uri);selectionMode=true;for(Track t:shown)if(all)selected.remove(t.uri);else selected.add(t.uri);render();});contextActions.addView(selectAllButton,new LinearLayout.LayoutParams(dp(40),dp(48)));updateSelectionIcon();
 }
 private void updateSelectionIcon(){if(selectAllButton==null)return;int n=0;for(Track t:shown)if(selected.contains(t.uri))n++;boolean all=!shown.isEmpty()&&n==shown.size();selectAllButton.setImageResource(all?R.drawable.ui_check_all:n>0?R.drawable.ui_check_partial:R.drawable.ui_check_empty);selectAllButton.setContentDescription(all?"Deselect all":"Select all");selectAllButton.setSelected(all);selectAllButton.setEnabled(!shown.isEmpty());}
 private void addSelectedTracks(){ArrayList<Track> tracks=new ArrayList<>();List<Track> source=playlistId==null?device:store.state().find(playlistId).tracks;for(Track t:source)if(selected.contains(t.uri))tracks.add(t);chooseDestination(tracks);}
 private void removeSelectedTracks(){LibraryState.Playlist p=store.state().find(playlistId);if(p==null)return;for(int i=p.tracks.size()-1;i>=0;i--)if(selected.contains(p.tracks.get(i).uri))p.tracks.remove(i);store.save();selected.clear();selectionMode=false;render();}
 private void selectionOptions(){new AlertDialog.Builder(activity).setTitle(selected.size()+" selected").setItems(new String[]{"Add to playlist","Cancel selection"},(d,i)->{if(i==0)addSelectedTracks();else{selected.clear();selectionMode=false;render();}}).show();}
 private void playPlaylist(LibraryState.Playlist p,boolean shuffled){if(p.tracks.isEmpty())return;if(shuffled)store.selectShuffled(p.tracks);else store.select(p.tracks,0);host.selected();host.play();if(!embedded)dismiss();}
 private void render(){
  phoneTab.setTextColor(!playlistsTab?GOLD:INK);playlistTab.setTextColor(playlistsTab?GOLD:INK);actions.removeAllViews();search.setVisibility(playlistsTab&&playlistId==null?View.GONE:View.VISIBLE);filter();
  if(playlistId!=null){LibraryState.Playlist p=store.state().find(playlistId);if(p==null){playlistId=null;render();return;}subtitle.setText(selectionMode?selected.size()+" selected":p.name);if(!selectionMode){LinearLayout row=new LinearLayout(activity);rowButton(row,button("‹",()->{playlistId=null;search.setText("");render();}));rowButton(row,button("Play",()->playPlaylist(p,false)));rowButton(row,button("Shuffle",()->playPlaylist(p,true)));actions.addView(row);}}
  else if(playlistsTab){subtitle.setText(store.state().playlists.isEmpty()?"No playlists":store.state().playlists.size()+" playlists");actions.addView(button("New playlist",()->newPlaylist(null)));}
  else subtitle.setText(selectionMode?selected.size()+" selected":device.size()+" songs");
  selectionTools();LinearLayout bottom=new LinearLayout(activity);rowButton(bottom,button("Add files",()->host.files()));rowButton(bottom,button("Refresh",()->scan()));actions.addView(bottom);
 }
 public void scan(){
  if(closed)return;
  if(activity.checkSelfPermission(MediaLibrary.permission())!=android.content.pm.PackageManager.PERMISSION_GRANTED){render();if(!playlistsTab)subtitle.setText("Audio access is off. Tap Allow access, or Add files.");if(!playlistsTab){Button allow=button("Allow audio access",()->host.permission());actions.addView(allow,0);}return;}
  int generation=++scanGeneration;if(!playlistsTab)subtitle.setText("Reading phone music…");Context context=activity.getApplicationContext();
  worker.execute(()->{try{ArrayList<Track> found=MediaLibrary.scan(context);main.post(()->{if(closed||generation!=scanGeneration)return;device.clear();device.addAll(found);store.state().restored(uris(found));store.save();selected.retainAll(uris(found));render();if(found.isEmpty()&&!playlistsTab)subtitle.setText("No indexed audio found. Try Add files or Refresh phone after downloading music.");});}catch(Exception e){main.post(()->{if(closed||generation!=scanGeneration)return;subtitle.setText("Cannot read phone music. Check audio permission, or use Add files.");});}});
 }
 private Set<String> uris(List<Track> tracks){HashSet<String> s=new HashSet<>();for(Track t:tracks)s.add(t.uri);return s;}
 public void imported(List<Track> tracks){if(!closed)chooseDestination(tracks);}
 private void chooseDestination(List<Track> tracks){if(tracks.isEmpty()){toast("No readable files selected.");return;}ArrayList<String> choices=new ArrayList<>();choices.add("New playlist…");for(LibraryState.Playlist p:store.state().playlists)choices.add(p.name);new AlertDialog.Builder(activity).setTitle("Add "+tracks.size()+" tracks to…").setItems(choices.toArray(new String[0]),(d,i)->{if(i==0)newPlaylist(tracks);else{LibraryState.Playlist p=store.state().playlists.get(i-1);int added=LibraryState.add(p,tracks);store.save();selected.clear();selectionMode=false;playlistsTab=true;playlistId=p.id;search.setText("");render();toast(added+" added · duplicates ignored");}}).setNegativeButton("Cancel",null).show();}
 private void newPlaylist(List<Track> tracks){if(store.state().playlists.size()>=LibraryState.MAX_LISTS){toast("Up to 100 playlists are supported.");return;}nameDialog("New playlist","",name->{LibraryState.Playlist p=new LibraryState.Playlist(name);if(tracks!=null)LibraryState.add(p,tracks);store.state().playlists.add(p);store.save();selected.clear();selectionMode=false;playlistsTab=true;playlistId=p.id;search.setText("");render();});}
 private interface Named {void apply(String name);}
 private void nameDialog(String title,String value,Named result){EditText input=new EditText(activity);input.setSingleLine();input.setText(value);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)});input.setSelectAllOnFocus(true);AlertDialog d=new AlertDialog.Builder(activity).setTitle(title).setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{String name=input.getText().toString().trim();if(name.isEmpty()){input.setError("Enter a name");return;}result.apply(name);d.dismiss();}));d.show();}
 private void playlistOptions(LibraryState.Playlist p){new AlertDialog.Builder(activity).setTitle(p.name).setItems(new String[]{"Rename","Delete playlist"},(d,i)->{if(i==0)nameDialog("Rename playlist",p.name,n->{p.name=n;store.save();render();});else new AlertDialog.Builder(activity).setTitle("Delete "+p.name+"?").setMessage("Your audio files and current playback queue stay on the phone.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(x,w)->{store.state().playlists.remove(p);store.save();render();}).show();}).show();}
 private void trackOptions(Track track){LibraryState.Playlist p=store.state().find(playlistId);if(p==null)return;new AlertDialog.Builder(activity).setTitle(track.title).setItems(new String[]{"Move up","Move down","Remove from playlist"},(d,i)->{int n=p.tracks.indexOf(track);if(n<0)return;if(i==2)p.tracks.remove(n);else{int target=n+(i==0?-1:1);if(target>=0&&target<p.tracks.size())Collections.swap(p.tracks,n,target);}store.save();render();}).show();}
}
