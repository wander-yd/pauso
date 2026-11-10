package com.andrew.hourpause;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.media.*;
import android.os.*;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;
import android.text.*;
import android.text.style.*;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Date;
import java.text.SimpleDateFormat;

public class MainActivity extends Activity {
 private final int BG=0xff09120d,CARD=0xff223a30,INK=0xfff4efe4,MUTED=0xffa7ac98,ACCENT=0xffdfd3b3;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private TextView file,error,planPreview;
 private ImageButton play,stop;
 private ImageButton sleepCancel;private TextView sleepStatus;private SleepStore sleep;
 private ImageButton previousTrack,nextTrack;
 private MusicStore musicStore;private MusicDialog musicDialog;
 private PlanStore plans;
 private SharedPreferences prefs;
 private SoundStore sounds;
 private UpdateClient updates;
 private AudioOutput audition;
 private AudioFocusRequest auditionFocus;
 private int auditionToken;
 private final Runnable endPreview=()->stopPreview();
 private final Runnable refresh=new Runnable(){public void run(){render();handler.postDelayed(this,250);}};
 private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
 private LinearLayout shell,playerPage,topBar,transportRow,sleepRow,musicRow,bottomNav;
 private FrameLayout pageArea,musicContent;private View musicPage,settingsPage,soundsPage,browserPage;
 private TurntableView scene;private TextView clockText;private Button rhythmControl;
 private ImageButton sleepTrigger,shuffleControl,repeatControl;private final ImageButton[] navButtons=new ImageButton[3];
 private final Button[] musicTabs=new Button[3];private final TextView[] categoryLabels=new TextView[2];
 private int page=0,musicTab=0;private boolean immersive,resumed;
 @Override public void onCreate(Bundle b){
  super.onCreate(b);sleep=new SleepStore(this);if(!PlaybackService.running)sleep.endSession();prefs=getSharedPreferences("player",0);plans=new PlanStore(this);sounds=new SoundStore(this);sounds.initialize();musicStore=new MusicStore(this);updates=new UpdateClient(this);
  getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
  shell=new LinearLayout(this);shell.setOrientation(1);shell.setBackgroundColor(BG);
  shell.setOnApplyWindowInsetsListener((v,insets)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);}else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
  pageArea=new FrameLayout(this);shell.addView(pageArea,new LinearLayout.LayoutParams(-1,0,1));
  boolean small=getResources().getDisplayMetrics().heightPixels/getResources().getDisplayMetrics().density<640;
  playerPage=new LinearLayout(this);playerPage.setOrientation(1);playerPage.setPadding(dp(16),dp(small?6:12),dp(16),dp(4));playerPage.setOnClickListener(v->setQuietMode(!immersive));pageArea.addView(playerPage,new FrameLayout.LayoutParams(-1,-1));
  topBar=new LinearLayout(this);topBar.setGravity(Gravity.CENTER_VERTICAL);playerPage.addView(topBar,new LinearLayout.LayoutParams(-1,dp(44)));
  clockText=text("",13,MUTED);topBar.addView(clockText,new LinearLayout.LayoutParams(0,-2,1));
  ImageButton volume=iconButton(R.drawable.ui_volume,BG,MUTED,44,11,"Player volume");topBar.addView(volume,new LinearLayout.LayoutParams(dp(44),dp(44)));volume.setOnClickListener(v->volumeDialog());
  rhythmControl=button("14 | 1",false);rhythmControl.setTextColor(ACCENT);rhythmControl.setTextSize(18);topBar.addView(rhythmControl,new LinearLayout.LayoutParams(dp(104),dp(44)));rhythmControl.setOnClickListener(v->rhythmDialog());
  scene=new TurntableView(this);playerPage.addView(scene,new LinearLayout.LayoutParams(-1,0,1));scene.setOnClickListener(v->{if(immersive)setQuietMode(false);else setPage(1);});
  planPreview=text("",12,MUTED);planPreview.setGravity(Gravity.CENTER);playerPage.addView(planPreview,new LinearLayout.LayoutParams(-1,dp(22)));
  transportRow=new LinearLayout(this);transportRow.setGravity(Gravity.CENTER);playerPage.addView(transportRow,new LinearLayout.LayoutParams(-1,dp(small?52:66)));
  previousTrack=iconButton(R.drawable.ic_control_previous,BG,INK,44,12,"Previous track");transportRow.addView(previousTrack,new LinearLayout.LayoutParams(dp(44),dp(44)));previousTrack.setOnClickListener(v->skipTrack(true));
  play=iconButton(R.drawable.ic_control_play,ACCENT,BG,small?52:62,small?15:18,"Start playback");transportRow.addView(play,new LinearLayout.LayoutParams(dp(small?52:62),dp(small?52:62)));play.setOnClickListener(v->playOrPause());
  nextTrack=iconButton(R.drawable.ic_control_next,BG,INK,44,12,"Next track");transportRow.addView(nextTrack,new LinearLayout.LayoutParams(dp(44),dp(44)));nextTrack.setOnClickListener(v->skipTrack(false));
  stop=iconButton(R.drawable.ic_control_stop,CARD,INK,44,13,"Stop session");LinearLayout.LayoutParams stopLP=new LinearLayout.LayoutParams(dp(44),dp(44));stopLP.leftMargin=dp(14);transportRow.addView(stop,stopLP);stop.setOnClickListener(v->{if(PlaybackService.running)startService(new Intent(this,PlaybackService.class).setAction(PlaybackService.STOP));});
  sleepRow=new LinearLayout(this);sleepRow.setGravity(Gravity.CENTER);playerPage.addView(sleepRow,new LinearLayout.LayoutParams(-1,dp(44)));
  sleepTrigger=iconButton(R.drawable.ic_sleep,BG,MUTED,44,13,"Sleep timer");sleepRow.addView(sleepTrigger,new LinearLayout.LayoutParams(dp(44),dp(44)));sleepTrigger.setOnClickListener(v->sleepDialog());
  sleepStatus=text("",12,MUTED);sleepStatus.setSingleLine();sleepStatus.setEllipsize(TextUtils.TruncateAt.END);sleepRow.addView(sleepStatus,new LinearLayout.LayoutParams(-2,-2));sleepStatus.setOnClickListener(v->sleepDialog());
  sleepCancel=iconButton(R.drawable.ic_close,BG,MUTED,44,13,"Cancel Sleep timer");sleepRow.addView(sleepCancel,new LinearLayout.LayoutParams(dp(44),dp(44)));sleepCancel.setOnClickListener(v->{sleep.cancel();sleepChanged();});
  musicRow=new LinearLayout(this);musicRow.setGravity(Gravity.CENTER_VERTICAL);playerPage.addView(musicRow,new LinearLayout.LayoutParams(-1,dp(52)));musicRow.setOnClickListener(v->setPage(1));
  ImageView musicIcon=new ImageView(this);musicIcon.setImageResource(R.drawable.ic_music);musicIcon.setImageTintList(ColorStateList.valueOf(ACCENT));musicIcon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);musicRow.addView(musicIcon,new LinearLayout.LayoutParams(dp(22),dp(22)));
  file=text("",13,INK);file.setSingleLine();file.setEllipsize(TextUtils.TruncateAt.END);LinearLayout.LayoutParams fileLP=new LinearLayout.LayoutParams(0,-2,1);fileLP.leftMargin=dp(10);musicRow.addView(file,fileLP);
  shuffleControl=iconButton(R.drawable.ui_shuffle,BG,MUTED,40,9,"Shuffle");musicRow.addView(shuffleControl,new LinearLayout.LayoutParams(dp(40),dp(44)));shuffleControl.setOnClickListener(v->{PlaybackQueue q=musicStore.state().queue;q.shuffle(!q.shuffle,new java.util.Random());musicStore.save();modeChanged();});
  repeatControl=iconButton(R.drawable.ui_repeat,BG,ACCENT,40,9,"Repeat all");musicRow.addView(repeatControl,new LinearLayout.LayoutParams(dp(40),dp(44)));repeatControl.setOnClickListener(v->{PlaybackQueue q=musicStore.state().queue;q.repeat=(q.repeat+1)%3;musicStore.save();modeChanged();});
  error=text("",12,0xffffc0a7);error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);playerPage.addView(error,new LinearLayout.LayoutParams(-1,-2));
  buildMusicPage();buildSettingsPage();
  bottomNav=new LinearLayout(this);bottomNav.setPadding(dp(20),dp(3),dp(20),dp(3));shell.addView(bottomNav,new LinearLayout.LayoutParams(-1,dp(54)));
  int[] navIcons={R.drawable.ui_disc,R.drawable.ic_music,R.drawable.ui_settings};String[] navLabels={"Player","Music","Settings"};
  for(int i=0;i<3;i++){final int n=i;ImageButton button=iconButton(navIcons[i],BG,MUTED,44,12,navLabels[i]);navButtons[i]=button;bottomNav.addView(button,new LinearLayout.LayoutParams(0,-1,1));button.setOnClickListener(v->setPage(n));}
  setContentView(shell);shell.requestApplyInsets();int restored=b==null?0:b.getInt("page",0);setPage(restored);if(b!=null&&b.getBoolean("immersive",false)&&page==0)setQuietMode(true);render();
 }
 private void buildMusicPage(){
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(16),dp(14),dp(16),0);musicPage=root;pageArea.addView(root,new FrameLayout.LayoutParams(-1,-1));
  LinearLayout tabs=new LinearLayout(this);String[] labels={"Sounds","Songs","Playlists"};for(int i=0;i<3;i++){final int n=i;Button tab=button(labels[i],false);tab.setTextSize(13);musicTabs[i]=tab;LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(44),1);if(i>0)lp.leftMargin=dp(4);tabs.addView(tab,lp);tab.setOnClickListener(v->setMusicTab(n));}root.addView(tabs);
  musicContent=new FrameLayout(this);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,0,1);cp.topMargin=dp(12);root.addView(musicContent,cp);
  ScrollView scroll=new ScrollView(this);LinearLayout categories=new LinearLayout(this);categories.setOrientation(1);scroll.addView(categories);soundsPage=scroll;musicContent.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
  LinearLayout cards=new LinearLayout(this);categories.addView(cards);String[] names={"Noise","Nature"};int[] symbols={R.drawable.ui_noise,R.drawable.ui_nature};
  for(int i=0;i<2;i++){final int index=i;LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setBackground(shape(CARD,18));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);if(i>0)lp.leftMargin=dp(10);cards.addView(card,lp);
   LinearLayout main=new LinearLayout(this);main.setOrientation(1);main.setPadding(dp(16),dp(18),dp(12),dp(12));main.setGravity(Gravity.BOTTOM);main.setClickable(true);main.setFocusable(true);main.setContentDescription("Use last "+names[i]+" sound");card.addView(main,new LinearLayout.LayoutParams(-1,dp(142)));main.setOnClickListener(v->{stopPreview();sounds.select(categoryLast(index));soundChanged();});
   ImageView symbol=new ImageView(this);symbol.setImageResource(symbols[i]);symbol.setImageTintList(ColorStateList.valueOf(ACCENT));symbol.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);main.addView(symbol,new LinearLayout.LayoutParams(dp(28),dp(28)));TextView name=text(names[i],19,INK);add(main,name,14);categoryLabels[i]=text(categoryLast(i),11,MUTED);categoryLabels[i].setMaxLines(2);add(main,categoryLabels[i],4);
   Button change=button("Change ›",false);change.setTextColor(ACCENT);change.setTextSize(12);change.setContentDescription("Change "+names[i]+" sound");card.addView(change,new LinearLayout.LayoutParams(-1,dp(44)));change.setOnClickListener(v->categoryDialog(index));
  }
  musicDialog=new MusicDialog(this,new MusicDialog.Host(){public void permission(){requestPermissions(new String[]{MediaLibrary.permission()},18);}public void files(){importFiles();}public void selected(){soundChanged();}public void play(){if(!PlaybackService.running||"Paused".equals(PlaybackService.status))playOrPause();setPage(0);}},true);
  browserPage=musicDialog.embeddedView();musicContent.addView(browserPage,new FrameLayout.LayoutParams(-1,-1));browserPage.setVisibility(View.GONE);
 }
 private String categoryLast(int category){return sounds.lastInCategory(category==0);}
 private void setMusicTab(int tab){musicTab=tab;boolean browser=tab>0;soundsPage.setVisibility(browser?View.GONE:View.VISIBLE);browserPage.setVisibility(browser?View.VISIBLE:View.GONE);if(browser)musicDialog.activate(tab==2);render();}
 private void buildSettingsPage(){ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(20),dp(16),dp(20),dp(16));scroll.addView(root);settingsPage=scroll;pageArea.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
  Button immersiveEntry=button("Immersive view",false);add(root,immersiveEntry,4);immersiveEntry.setOnClickListener(v->{setPage(0);setQuietMode(true);});
  Switch motion=new Switch(this);motion.setText("Reduce motion");motion.setTextColor(INK);motion.setPadding(dp(6),dp(8),dp(6),dp(8));motion.setChecked(prefs.getBoolean("reduce_motion",false));add(root,motion,12);motion.setOnCheckedChangeListener((v,checked)->{prefs.edit().putBoolean("reduce_motion",checked).apply();render();});
  Button update=button("v"+updates.currentVersion()+" · Check for updates",false);update.setTextSize(13);add(root,update,20);update.setOnClickListener(v->updates.check(true));
  Button credits=button("Sound credits",false);credits.setTextSize(13);add(root,credits,8);credits.setOnClickListener(v->soundCredits());
 }
 private void setPage(int selected){if(selected<0||selected>2)selected=0;if(immersive)setQuietMode(false);page=selected;playerPage.setVisibility(page==0?View.VISIBLE:View.GONE);musicPage.setVisibility(page==1?View.VISIBLE:View.GONE);settingsPage.setVisibility(page==2?View.VISIBLE:View.GONE);if(page==1)setMusicTab(musicTab);for(int i=0;i<3;i++)if(navButtons[i]!=null){navButtons[i].setImageTintList(ColorStateList.valueOf(page==i?ACCENT:MUTED));navButtons[i].setSelected(page==i);}render();}
 private void setQuietMode(boolean value){immersive=value;for(View v:new View[]{topBar,planPreview,transportRow,sleepRow,musicRow,bottomNav})v.setVisibility(value?View.GONE:View.VISIBLE);error.setVisibility(!value&&!PlaybackService.error.isEmpty()?View.VISIBLE:View.GONE);if(value)scene.requestFocus();render();}
 @Override public void onBackPressed(){if(immersive){setQuietMode(false);return;}if(page==1&&musicTab>0&&musicDialog.navigateBack())return;if(page!=0){setPage(0);return;}super.onBackPressed();}
 @Override protected void onSaveInstanceState(Bundle b){b.putInt("page",page);b.putBoolean("immersive",immersive);super.onSaveInstanceState(b);}
 private void volumeDialog(){LinearLayout box=new LinearLayout(this);box.setPadding(dp(20),dp(8),dp(20),dp(12));SeekBar bar=new SeekBar(this);bar.setMax(100);bar.setProgress(sounds.volume());bar.setContentDescription("Player volume");bar.setProgressTintList(ColorStateList.valueOf(ACCENT));bar.setThumbTintList(ColorStateList.valueOf(ACCENT));box.addView(bar,new LinearLayout.LayoutParams(-1,dp(48)));bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar v,int n,boolean user){if(user){prefs.edit().putInt("volume",n).apply();if(PlaybackService.running)startService(new Intent(MainActivity.this,PlaybackService.class).setAction(PlaybackService.VOLUME));}}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});new AlertDialog.Builder(this).setTitle("Player volume").setView(box).setNegativeButton("Close",null).show();}
 private void rhythmDialog(){LinearLayout box=new LinearLayout(this);box.setPadding(dp(20),dp(8),dp(20),dp(12));box.setOrientation(1);box.addView(text("Play / Break · minutes",12,MUTED));AlertDialog d=new AlertDialog.Builder(this).setTitle("Rhythm").setView(box).setNegativeButton("Close",null).create();for(int row=0;row<2;row++){LinearLayout line=new LinearLayout(this);add(box,line,8);for(int col=0;col<2;col++){final int index=row*2+col;int[] p=PlanStore.PRESETS[index];Button tile=button(p[0]+"\n"+p[1],false);tile.setTextSize(22);tile.setContentDescription(p[0]+" minutes Play, "+p[1]+" minutes Break");tile.setTextColor(plans.selected()==index?ACCENT:INK);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(78),1);if(col>0)lp.leftMargin=dp(8);line.addView(tile,lp);tile.setOnClickListener(v->{plans.preset(index);d.dismiss();planChanged();});}}Button custom=button("Custom rhythm ›",false);add(box,custom,10);custom.setOnClickListener(v->{d.dismiss();customDialog();});d.show();}
 private void categoryDialog(int category){stopPreview();LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(18),dp(4),dp(18),dp(8));TextView message=text("",12,ACCENT);add(box,message,0);String[] ids=category==0?new String[]{"white","pink","brown"}:new String[]{"rain","forest"};AlertDialog dialog=new AlertDialog.Builder(this).setTitle(category==0?"Noise":"Nature").setView(box).setNegativeButton("Close",null).create();for(String id:ids){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView title=text(sounds.title(id),15,INK);row.addView(title,new LinearLayout.LayoutParams(0,-2,1));ImageButton preview=iconButton(R.drawable.ic_control_play,CARD,INK,44,13,"Preview "+sounds.title(id));row.addView(preview,new LinearLayout.LayoutParams(dp(44),dp(44)));preview.setOnClickListener(v->previewSound(id,message));Button use=button(sounds.id().equals(id)?"Selected":"Use",false);use.setTextSize(12);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(80),dp(44));lp.leftMargin=dp(6);row.addView(use,lp);use.setOnClickListener(v->{stopPreview();sounds.select(id);soundChanged();dialog.dismiss();});add(box,row,8);}dialog.setOnDismissListener(d->stopPreview());dialog.show();}
 private void soundCredits(){new AlertDialog.Builder(this).setTitle("Sound credits").setMessage("Forest Rain: stevenparry · CC0\nhttps://freesound.org/s/760955/\n\nForest Birds: Magnesus · CC0\nhttps://freesound.org/s/723913/\n\nNoise is generated locally.").setPositiveButton("Close",null).show();}
 private void importFiles(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("audio/*").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);try{startActivityForResult(i,17);}catch(ActivityNotFoundException e){Toast.makeText(this,"No file picker found.",Toast.LENGTH_LONG).show();}}
 private void playOrPause(){
  stopPreview();
  if(PlaybackService.running){String action="Paused".equals(PlaybackService.status)?PlaybackService.RESUME:PlaybackService.PAUSE;startService(new Intent(this,PlaybackService.class).setAction(action));}
  else{
   if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},8);
   PlaybackService.error="";
   try{startForegroundService(new Intent(this,PlaybackService.class).setAction(PlaybackService.START));}catch(Exception e){PlaybackService.error="Unable to start playback. Reopen Pauso and try again.";}
  }
 }
 private void soundChanged(){
  PlaybackService.error="";
  if(PlaybackService.running)startService(new Intent(this,PlaybackService.class).setAction(PlaybackService.SOUND));render();
 }
 private void stopPreview(){
  auditionToken++;handler.removeCallbacks(endPreview);if(audition!=null){audition.release();audition=null;}
  if(auditionFocus!=null){((AudioManager)getSystemService(AUDIO_SERVICE)).abandonAudioFocusRequest(auditionFocus);auditionFocus=null;}
 }
 private void previewSound(String id,TextView message){
  if(PlaybackService.running&&!"Paused".equals(PlaybackService.status)){message.setText("Pause playback before previewing a sound.");return;}
  stopPreview();int token=auditionToken;
  AudioManager manager=(AudioManager)getSystemService(AUDIO_SERVICE);
  auditionFocus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setOnAudioFocusChangeListener(change->{if(change<0){stopPreview();message.setText("Preview ended");}}).build();
  if(manager.requestAudioFocus(auditionFocus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){stopPreview();message.setText("Other audio is busy. Try again later.");return;}
  final SleepPlan previewSleep=sleep.load();
  message.setText("Previewing "+sounds.title(id)+" · 5 seconds");
  try{
   audition=new AudioOutput(this,id,sounds.uri(),0,()->{
    if(token!=auditionToken||audition==null)return;SleepPlan sleepPreview=previewSleep;long previewLimit=sleepPreview.active()?Math.min(5000,sleepPreview.remaining(SystemClock.elapsedRealtime(),System.currentTimeMillis())):5000;if(previewLimit<=0){stopPreview();message.setText("Sleep finished");return;}audition.setVolume(Math.min(.35f,sounds.volume()/100f));audition.setPlaying(true);
    handler.postDelayed(endPreview,previewLimit);handler.postDelayed(()->{if(token+1==auditionToken)message.setText("Preview finished · tap Use to select");},5100);
   },()->{if(token==auditionToken){stopPreview();message.setText("Could not preview this sound.");}});
  }catch(Exception e){stopPreview();message.setText("Could not preview this sound.");}
 }
 private void modeChanged(){if(PlaybackService.running)startService(new Intent(this,PlaybackService.class).setAction(PlaybackService.MODE));render();}
 private void skipTrack(boolean previous){
  if(!sounds.id().equals("local"))return;
  if(PlaybackService.running)startService(new Intent(this,PlaybackService.class).setAction(previous?PlaybackService.PREVIOUS:PlaybackService.NEXT));
  else{PlaybackQueue q=musicStore.state().queue;boolean changed=previous?q.previous():q.advance(false,new java.util.Random());if(changed){musicStore.save();musicStore.mirror();}render();}
 }
 private void openMusic(){stopPreview();setPage(1);setMusicTab(1);}
 @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);if(code==18&&musicDialog!=null)musicDialog.scan();}
 @Override protected void onDestroy(){if(musicDialog!=null)musicDialog.dismiss();stopPreview();super.onDestroy();}
 private void sleepChanged(){if(PlaybackService.running)startService(new Intent(this,PlaybackService.class).setAction(PlaybackService.SLEEP));render();}
 private void sleepDialog(){
  stopPreview();
  if(!PlaybackService.running){new AlertDialog.Builder(this).setTitle("Sleep timer").setMessage("Start playback to set a sleep timer.").setPositiveButton("OK",null).show();return;}
  LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(20),dp(8),dp(20),dp(8));
  ScrollView scroll=new ScrollView(this);scroll.addView(box);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Sleep timer").setView(scroll).setNegativeButton("Close",null).create();
  int[] minutes={15,30,60,90};for(int row=0;row<2;row++){LinearLayout line=new LinearLayout(this);add(box,line,8);for(int col=0;col<2;col++){int n=minutes[row*2+col];Button b=button(n+" minutes",false);b.setTextSize(14);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),1);if(col>0)lp.leftMargin=dp(8);line.addView(b,lp);b.setOnClickListener(v->{if(PlaybackService.running){sleep.duration(n,SystemClock.elapsedRealtime());sleepChanged();}dialog.dismiss();});}}
  Button custom=button("Custom duration…",false);add(box,custom,8);custom.setOnClickListener(v->{dialog.dismiss();customSleepDialog();});
  Button period=button("Finish this play period",false);add(box,period,10);
  Runnable updatePeriod=()->{Schedule.Snapshot phase=plans.now();period.setText(phase.quiet?"Already on break · stop now":"This play period ends · "+time(System.currentTimeMillis()+phase.remaining));};
  final Runnable live=new Runnable(){public void run(){updatePeriod.run();if(dialog.isShowing())handler.postDelayed(this,1000);}};
  period.setOnClickListener(v->{if(PlaybackService.running){sleep.period(System.currentTimeMillis(),plans.now());sleepChanged();}dialog.dismiss();});
  if(sleep.load().active()){Button cancel=button("Cancel Sleep",false);add(box,cancel,8);cancel.setOnClickListener(v->{sleep.cancel();sleepChanged();dialog.dismiss();});}
  dialog.setOnShowListener(d->live.run());dialog.setOnDismissListener(d->handler.removeCallbacks(live));dialog.show();if(dialog.getWindow()!=null)dialog.getWindow().setBackgroundDrawable(shape(BG,24));
 }
 private void customSleepDialog(){
  EditText input=new EditText(this);input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);input.setSingleLine();input.setText(String.valueOf(sleep.lastMinutes()));styleInput(input);
  LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(24),dp(12),dp(24),dp(8));box.addView(text("Stop after 1–720 minutes",14,MUTED));box.addView(input);
  AlertDialog d=new AlertDialog.Builder(this).setTitle("Sleep duration").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Enable",null).create();d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{Integer minutes=validDuration(input);if(minutes==null)return;if(PlaybackService.running){sleep.duration(minutes,SystemClock.elapsedRealtime());sleepChanged();}d.dismiss();}));d.show();if(d.getWindow()!=null)d.getWindow().setBackgroundDrawable(shape(BG,24));
 }
 private String time(long epoch){
  SimpleDateFormat clock=new SimpleDateFormat("HH:mm",Locale.getDefault()),date=new SimpleDateFormat("yyyyMMdd",Locale.US);
  return clock.format(new Date(epoch))+(date.format(new Date(epoch)).equals(date.format(new Date()))?"":" (next day)");
 }
 private String preview(Schedule.Rhythm rhythm,long now){Schedule.Snapshot t=rhythm.at(now,TimeZone.getDefault());return (t.quiet?"Break ends at ":"Next break at ")+time(now+t.remaining);}
 private void planChanged(){if(sleep.load().active())stopPreview();if(PlaybackService.running)startService(new Intent(this,PlaybackService.class).setAction(PlaybackService.PLAN));render();}
 private Integer validDuration(EditText input){
  try{int value=Integer.parseInt(input.getText().toString());if(value<1||value>720)throw new IllegalArgumentException();input.setError(null);return value;}
  catch(Exception e){if(input.getError()==null)input.setError("Use 1–720 minutes");return null;}
 }
 private void styleInput(EditText input){input.setTextColor(INK);input.setHintTextColor(MUTED);input.setBackgroundTintList(ColorStateList.valueOf(ACCENT));input.setSelectAllOnFocus(true);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});input.setMinHeight(dp(48));}
 private void customDialog(){
  LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(24),dp(12),dp(24),dp(4));
  box.addView(text("Play (minutes)",14,MUTED));
  EditText work=new EditText(this);work.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);work.setSingleLine();work.setText(String.valueOf(prefs.getInt("custom_work",25)));styleInput(work);box.addView(work);
  box.addView(text("Break (minutes)",14,MUTED));
  EditText rest=new EditText(this);rest.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);rest.setSingleLine();rest.setText(String.valueOf(prefs.getInt("custom_rest",5)));styleInput(rest);box.addView(rest);
  RadioGroup mode=new RadioGroup(this);RadioButton aligned=new RadioButton(this);aligned.setId(View.generateViewId());aligned.setText("Align to clock");aligned.setTextColor(INK);RadioButton fromNow=new RadioButton(this);fromNow.setId(View.generateViewId());fromNow.setText("Start now");fromNow.setTextColor(INK);mode.addView(aligned);mode.addView(fromNow);mode.check(prefs.getBoolean("custom_aligned",true)?aligned.getId():fromNow.getId());box.addView(mode);
  TextView explanation=text("Clock-aligned: cycles start at today's 00:00 and continue across days.\nStart now: begins a full Play interval when you tap Apply.",12,MUTED);box.addView(explanation);
  TextView result=text("",14,ACCENT);result.setPadding(0,dp(12),0,dp(8));box.addView(result);
  Runnable update=()->{
   try{int w=Integer.parseInt(work.getText().toString()),r=Integer.parseInt(rest.getText().toString());long now=System.currentTimeMillis();boolean clock=mode.getCheckedRadioButtonId()==aligned.getId();long local=now+TimeZone.getDefault().getOffset(now);long anchor=clock?Math.floorDiv(local,Schedule.DAY)*Schedule.DAY:now;result.setText(preview(new Schedule.Rhythm(w,r,clock,anchor),now));}
   catch(Exception e){result.setText("Enter 1–720 minutes for both durations.");}
  };
  TextWatcher watch=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int after){}public void onTextChanged(CharSequence s,int st,int before,int c){update.run();}public void afterTextChanged(Editable e){}};
  work.addTextChangedListener(watch);rest.addTextChangedListener(watch);mode.setOnCheckedChangeListener((g,id)->update.run());update.run();
  ScrollView scroll=new ScrollView(this);scroll.addView(box);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Custom Rhythm").setView(scroll).setNegativeButton("Cancel",null).setPositiveButton("Apply",null).create();
  final Runnable livePreview=new Runnable(){public void run(){update.run();if(dialog.isShowing())handler.postDelayed(this,1000);}};
  dialog.setOnDismissListener(d->handler.removeCallbacks(livePreview));
  dialog.setOnShowListener(d->{livePreview.run();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   try{Integer w=validDuration(work),r=validDuration(rest);if(w==null||r==null)return;plans.custom(w,r,mode.getCheckedRadioButtonId()==aligned.getId(),System.currentTimeMillis());planChanged();dialog.dismiss();}
   catch(Exception ex){result.setText("Enter 1–720 minutes for both durations.");}
  });});dialog.show();
  if(dialog.getWindow()!=null)dialog.getWindow().setBackgroundDrawable(shape(BG,24));
 }
 private TextView text(String value,int sp,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(sp);t.setTextColor(color);return t;}
 private GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
 private GradientDrawable selectedShape(){GradientDrawable g=shape(CARD,14);g.setStroke(dp(2),ACCENT);return g;}
 private Drawable ripple(Drawable background){return new RippleDrawable(ColorStateList.valueOf(0x22E9D8AE),background,shape(Color.WHITE,36));}
 private Button button(String label,boolean primary){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(primary?BG:INK);b.setBackground(ripple(shape(primary?ACCENT:CARD,14)));b.setMinHeight(dp(48));b.setStateListAnimator(null);b.setElevation(0);b.setPadding(dp(8),dp(4),dp(8),dp(4));return b;}
 private ImageButton iconButton(int icon,int background,int tint,int size,int padding,String description){ImageButton b=new ImageButton(this);b.setImageResource(icon);b.setImageTintList(ColorStateList.valueOf(tint));b.setBackground(ripple(shape(background,size)));b.setPadding(dp(padding),dp(padding),dp(padding),dp(padding));b.setStateListAnimator(null);b.setElevation(0);b.setScaleType(ImageView.ScaleType.FIT_CENTER);b.setContentDescription(description);return b;}
 private void add(LinearLayout root,View view,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);root.addView(view,p);}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
  if(request==17){if(result!=RESULT_OK||data==null)return;java.util.ArrayList<Uri> uris=new java.util.ArrayList<>();
   if(data.getClipData()!=null){for(int i=0;i<data.getClipData().getItemCount();i++)uris.add(data.getClipData().getItemAt(i).getUri());}else if(data.getData()!=null)uris.add(data.getData());
   if(uris.size()>200){Toast.makeText(this,"Please add at most 200 files at a time.",Toast.LENGTH_LONG).show();return;}
   android.content.Context app=getApplicationContext();new Thread(()->{java.util.ArrayList<Track> tracks=new java.util.ArrayList<>();for(Uri uri:uris)try{app.getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);tracks.add(MediaLibrary.document(app,uri));}catch(Exception ignored){}
    runOnUiThread(()->{if(isFinishing()||isDestroyed())return;openMusic();musicDialog.imported(tracks);if(tracks.size()!=uris.size())Toast.makeText(this,"Some files could not retain access. Choose downloaded audio on your phone.",Toast.LENGTH_LONG).show();});},"Pauso file import").start();return;
  }
  if(request!=7||result!=RESULT_OK||data==null||data.getData()==null)return;
  Uri uri=data.getData();try{
   getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);Track track=MediaLibrary.document(this,uri);
   int position=uri.toString().equals(sounds.uri())?prefs.getInt("position",0):0;
   musicStore.select(java.util.Collections.singletonList(track),0);musicStore.state().queue.position=position;musicStore.save();musicStore.mirror();soundChanged();
  }catch(Exception e){PlaybackService.error="Could not retain access. Choose a downloaded audio file from your phone.";}render();
 }
 @Override protected void onResume(){super.onResume();resumed=true;handler.removeCallbacks(refresh);refresh.run();if(updates!=null)updates.check(false);}
 @Override protected void onPause(){resumed=false;stopPreview();handler.removeCallbacks(refresh);if(scene!=null)scene.setPresentation(false,true,immersive);super.onPause();}
 private String sleepCountdown(long millis){long seconds=(millis+999)/1000;return seconds>=3600?String.format(Locale.getDefault(),"%d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60):String.format(Locale.getDefault(),"%02d:%02d",seconds/60,seconds%60);}
 private void render(){
  if(scene==null)return;if(!PlaybackService.running)sleep.endSession();
  boolean active=PlaybackService.running,paused=active&&"Paused".equals(PlaybackService.status);Schedule.Snapshot timing=plans.now();
  String label=!active?"Ready":paused?"Paused":"Loading music".equals(PlaybackService.status)?"Loading":"Interrupted".equals(PlaybackService.status)?"Interrupted":timing.quiet?"Break":"Playing";
  int icon=paused||label.equals("Interrupted")?R.drawable.ic_status_pause:label.equals("Loading")?R.drawable.ic_status_loading:timing.quiet&&active?R.drawable.ic_status_rest:R.drawable.ic_status_music;
  boolean reduce=prefs.getBoolean("reduce_motion",false);try{reduce|=android.provider.Settings.Global.getFloat(getContentResolver(),android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0;}catch(Exception ignored){}
  scene.setPresentation(resumed&&page==0,reduce,immersive);scene.setState(label,timing.remaining,timing.quiet?timing.cycle-timing.play:timing.play,active&&"Playing".equals(PlaybackService.status),timing.quiet,icon);
  clockText.setText(time(System.currentTimeMillis()));Schedule.Rhythm rhythm=plans.load();rhythmControl.setText(rhythm.work+" | "+rhythm.rest);rhythmControl.setContentDescription("Change rhythm: "+rhythm.work+" minutes Play, "+rhythm.rest+" minutes Break");
  planPreview.setText((timing.quiet?"Break ends at ":"Next break at ")+time(System.currentTimeMillis()+timing.remaining));
  play.setImageResource(active&&!paused?R.drawable.ic_control_pause:R.drawable.ic_control_play);play.setContentDescription(!active?"Start playback":paused?"Resume playback":"Pause playback");stop.setEnabled(active);stop.setAlpha(active?1f:.35f);
  file.setText(sounds.title());musicRow.setContentDescription("Choose music. "+sounds.title());boolean local=sounds.id().equals("local")&&musicStore.state().queue.current()!=null;previousTrack.setVisibility(local?View.VISIBLE:View.GONE);nextTrack.setVisibility(local?View.VISIBLE:View.GONE);shuffleControl.setVisibility(local?View.VISIBLE:View.GONE);repeatControl.setVisibility(local?View.VISIBLE:View.GONE);
  PlaybackQueue queue=musicStore.state().queue;shuffleControl.setSelected(queue.shuffle);shuffleControl.setImageTintList(ColorStateList.valueOf(queue.shuffle&&queue.repeat!=PlaybackQueue.ONE?ACCENT:MUTED));shuffleControl.setContentDescription(queue.shuffle?"Shuffle on":"Shuffle off");repeatControl.setImageResource(queue.repeat==PlaybackQueue.ONE?R.drawable.ui_repeat_one:R.drawable.ui_repeat);repeatControl.setImageTintList(ColorStateList.valueOf(queue.repeat==PlaybackQueue.OFF?MUTED:ACCENT));repeatControl.setContentDescription(queue.repeat==PlaybackQueue.OFF?"Repeat off":queue.repeat==PlaybackQueue.ALL?"Repeat playlist":"Repeat one");
  SleepPlan sp=sleep.load();sleepStatus.setText(sp.active()?sleepCountdown(sp.remaining(SystemClock.elapsedRealtime(),System.currentTimeMillis()))+" · "+time(sp.endTime(SystemClock.elapsedRealtime(),System.currentTimeMillis())):sleep.message());sleepStatus.setVisibility(sp.active()||!sleep.message().isEmpty()?View.VISIBLE:View.GONE);sleepTrigger.setContentDescription(sp.active()?"Change Sleep timer":"Sleep timer");sleepCancel.setVisibility(sp.active()?View.VISIBLE:View.GONE);
  error.setText(PlaybackService.error);error.setVisibility(!immersive&&!PlaybackService.error.isEmpty()?View.VISIBLE:View.GONE);
  for(int i=0;i<2;i++)categoryLabels[i].setText(sounds.title(categoryLast(i)));if(page==1&&musicTab>0)musicTab=musicDialog.isPlaylists()?2:1;for(int i=0;i<3;i++){musicTabs[i].setTextColor(musicTab==i?ACCENT:MUTED);musicTabs[i].setSelected(musicTab==i);}
 }
}
