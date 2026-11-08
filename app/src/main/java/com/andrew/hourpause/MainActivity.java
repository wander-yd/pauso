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
 private final int BG=Color.rgb(32,58,50),CARD=Color.rgb(52,75,64),INK=Color.rgb(244,239,228),MUTED=Color.rgb(168,175,164),ACCENT=Color.rgb(233,216,174);
 private final Handler handler=new Handler(Looper.getMainLooper());
 private TextView state,count,detail,file,error,planPreview;
 private ImageView stateIcon;
 private ImageButton play,stop;
 private Button choose,customButton;
 private Dial dial;
 private PlanStore plans;
 private SharedPreferences prefs;
 private SoundStore sounds;
 private UpdateClient updates;
 private AudioOutput audition;
 private AudioFocusRequest auditionFocus;
 private int auditionToken;
 private final Runnable endPreview=()->stopPreview();
 private final Button[] presetButtons=new Button[4];
 private int lastSelected=-99,lastPlayIcon=-1,lastStateIcon=-1;
 private final Runnable refresh=new Runnable(){public void run(){render();handler.postDelayed(this,250);}};
 private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
 @Override public void onCreate(Bundle b){
  super.onCreate(b);prefs=getSharedPreferences("player",0);plans=new PlanStore(this);sounds=new SoundStore(this);sounds.initialize();updates=new UpdateClient(this);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);scroll.setClipToPadding(false);
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(22),dp(12),dp(22),dp(24));scroll.addView(root);
  scroll.setOnApplyWindowInsetsListener((v,insets)->{
   if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);}else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;
  });
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER);header.setOrientation(0);
  ImageView brand=new ImageView(this);brand.setImageResource(R.mipmap.ic_launcher_round);brand.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);header.addView(brand,new LinearLayout.LayoutParams(dp(32),dp(32)));
  TextView title=text("Pauso",26,INK);title.setTypeface(android.graphics.Typeface.create("sans-serif-medium",0));LinearLayout.LayoutParams titleLP=new LinearLayout.LayoutParams(-2,-2);titleLP.leftMargin=dp(10);header.addView(title,titleLP);add(root,header,0);
  int faceSize=Math.min(232,Math.max(184,(int)(getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density)-90));
  FrameLayout face=new FrameLayout(this);LinearLayout.LayoutParams faceLP=new LinearLayout.LayoutParams(dp(faceSize),dp(faceSize));faceLP.gravity=Gravity.CENTER;faceLP.topMargin=dp(14);root.addView(face,faceLP);
  dial=new Dial(this);face.addView(dial,new FrameLayout.LayoutParams(-1,-1));
  LinearLayout center=new LinearLayout(this);center.setOrientation(1);center.setGravity(Gravity.CENTER);center.setPadding(dp(18),0,dp(18),0);face.addView(center,new FrameLayout.LayoutParams(-1,-1));
  count=text("00:00",48,ACCENT);count.setTypeface(android.graphics.Typeface.create("sans-serif-medium",0));count.setGravity(Gravity.CENTER);count.setIncludeFontPadding(false);center.addView(count);
  LinearLayout stateRow=new LinearLayout(this);stateRow.setGravity(Gravity.CENTER);stateRow.setOrientation(0);LinearLayout.LayoutParams stateLP=new LinearLayout.LayoutParams(-1,dp(30));stateLP.topMargin=dp(8);center.addView(stateRow,stateLP);
  stateIcon=new ImageView(this);stateIcon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);stateRow.addView(stateIcon,new LinearLayout.LayoutParams(dp(19),dp(19)));
  state=text("Ready",16,INK);LinearLayout.LayoutParams stateTextLP=new LinearLayout.LayoutParams(-2,-2);stateTextLP.leftMargin=dp(6);stateRow.addView(state,stateTextLP);
  detail=text("Choose your music",12,MUTED);detail.setGravity(Gravity.CENTER);detail.setMaxLines(2);detail.setPadding(0,dp(5),0,0);center.addView(detail);
  LinearLayout controls=new LinearLayout(this);controls.setOrientation(0);controls.setGravity(Gravity.CENTER);add(root,controls,8);
  play=iconButton(R.drawable.ic_control_play,ACCENT,BG,72,21,"Start playback");controls.addView(play,new LinearLayout.LayoutParams(dp(72),dp(72)));
  stop=iconButton(R.drawable.ic_control_stop,CARD,INK,48,13,"Stop session");LinearLayout.LayoutParams stopLP=new LinearLayout.LayoutParams(dp(48),dp(48));stopLP.leftMargin=dp(26);controls.addView(stop,stopLP);
  play.setOnClickListener(v->playOrPause());stop.setOnClickListener(v->{if(PlaybackService.running)startService(new Intent(this,PlaybackService.class).setAction(PlaybackService.STOP));});
  buildRhythm(root);
  View divider=new View(this);divider.setBackgroundColor(CARD);LinearLayout.LayoutParams dividerLP=new LinearLayout.LayoutParams(-1,dp(1));dividerLP.topMargin=dp(18);dividerLP.bottomMargin=dp(12);root.addView(divider,dividerLP);
  LinearLayout music=new LinearLayout(this);music.setOrientation(0);music.setGravity(Gravity.CENTER_VERTICAL);root.addView(music);
  ImageView musicIcon=new ImageView(this);musicIcon.setImageResource(R.drawable.ic_music);musicIcon.setImageTintList(ColorStateList.valueOf(INK));musicIcon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);music.addView(musicIcon,new LinearLayout.LayoutParams(dp(22),dp(22)));
  file=text(sounds.title(),14,INK);file.setSingleLine();file.setEllipsize(TextUtils.TruncateAt.END);LinearLayout.LayoutParams fileLP=new LinearLayout.LayoutParams(0,-2,1);fileLP.leftMargin=dp(10);music.addView(file,fileLP);
  choose=button("Sounds",false);choose.setTextSize(13);choose.setMinHeight(dp(48));LinearLayout.LayoutParams chooseLP=new LinearLayout.LayoutParams(dp(76),dp(48));chooseLP.leftMargin=dp(8);music.addView(choose,chooseLP);choose.setContentDescription("Choose Sound");choose.setOnClickListener(v->soundDialog());
  LinearLayout volumeRow=new LinearLayout(this);volumeRow.setGravity(Gravity.CENTER_VERTICAL);volumeRow.setOrientation(0);add(root,volumeRow,4);
  TextView volumeLabel=text("Volume",12,MUTED);volumeRow.addView(volumeLabel,new LinearLayout.LayoutParams(dp(58),-2));
  SeekBar volume=new SeekBar(this);volume.setMax(100);volume.setProgress(sounds.volume());volume.setContentDescription("Player volume");volume.setProgressTintList(ColorStateList.valueOf(ACCENT));volume.setThumbTintList(ColorStateList.valueOf(ACCENT));volumeRow.addView(volume,new LinearLayout.LayoutParams(0,dp(48),1));
  volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int value,boolean user){if(user){prefs.edit().putInt("volume",value).apply();if(PlaybackService.running)startService(new Intent(MainActivity.this,PlaybackService.class).setAction(PlaybackService.VOLUME));}}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
  error=text("",13,Color.rgb(255,192,167));error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);add(root,error,4);
  Button update=button("v"+updates.currentVersion()+" · Check for updates",false);update.setTextSize(12);update.setTextColor(MUTED);update.setBackground(shape(BG,12));add(root,update,10);update.setOnClickListener(v->updates.check(true));
  setContentView(scroll);scroll.requestApplyInsets();render();
 }
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
  message.setText("Previewing "+sounds.title(id)+" · 5 seconds");
  try{
   audition=new AudioOutput(this,id,sounds.uri(),0,()->{
    if(token!=auditionToken||audition==null)return;audition.setVolume(Math.min(.35f,sounds.volume()/100f));audition.setPlaying(true);
    handler.postDelayed(endPreview,5000);handler.postDelayed(()->{if(token+1==auditionToken)message.setText("Preview finished · tap Use to select");},5100);
   },()->{if(token==auditionToken){stopPreview();message.setText("Could not preview this sound.");}});
  }catch(Exception e){stopPreview();message.setText("Could not preview this sound.");}
 }
 private void soundDialog(){
  LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(18),dp(4),dp(18),dp(8));
  TextView hint=text("Preview for 5 seconds, then tap Use.\nIf music is playing, pause it before previewing.",12,MUTED);add(box,hint,4);
  TextView previewMessage=text("All built-in sounds work offline.",12,ACCENT);add(box,previewMessage,8);
  ScrollView scroll=new ScrollView(this);scroll.addView(box);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Choose Sound").setView(scroll).setNegativeButton("Close",null).setNeutralButton("Credits",null).create();
  String[] ids={"white","pink","brown","rain","forest"};
  String[] notes={"Bright and steady · Offline","Soft and balanced · Offline","Deep and warm · Offline","Rain in the woods · Offline","Birdsong and forest air · Offline"};
  for(int i=0;i<ids.length;i++){
   if(i==0||i==3)add(box,text(i==0?"Noise":"Nature",18,INK),16);
   final String id=ids[i];LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setOrientation(0);add(box,row,4);
   LinearLayout words=new LinearLayout(this);words.setOrientation(1);row.addView(words,new LinearLayout.LayoutParams(0,-2,1));
   words.addView(text(sounds.title(id)+(id.equals(sounds.id())?" · Selected":""),15,INK));words.addView(text(notes[i],11,MUTED));
   ImageButton preview=iconButton(R.drawable.ic_control_play,CARD,INK,48,15,"Preview "+sounds.title(id));row.addView(preview,new LinearLayout.LayoutParams(dp(48),dp(48)));preview.setOnClickListener(v->previewSound(id,previewMessage));
   Button use=button("Use",false);use.setTextSize(13);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(56),dp(48));lp.leftMargin=dp(6);row.addView(use,lp);use.setContentDescription("Use "+sounds.title(id));use.setOnClickListener(v->{stopPreview();sounds.select(id);soundChanged();dialog.dismiss();});
  }
  add(box,text("My Audio",18,INK),16);
  if(!sounds.uri().isEmpty()){
   TextView local=text(sounds.title("local"),13,MUTED);local.setMaxLines(1);local.setEllipsize(TextUtils.TruncateAt.END);add(box,local,6);
   Button localUse=button("Use my selected file",false);add(box,localUse,6);localUse.setOnClickListener(v->{stopPreview();sounds.select("local");soundChanged();dialog.dismiss();});
  }
  Button chooseLocal=button("Choose a local file…",false);add(box,chooseLocal,6);chooseLocal.setOnClickListener(v->{stopPreview();dialog.dismiss();chooseFile();});
  dialog.setOnDismissListener(d->stopPreview());dialog.show();
  dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{
   new AlertDialog.Builder(this).setTitle("Sound credits").setMessage("Forest Rain: Rain in the forest — stevenparry\nhttps://freesound.org/s/760955/\n\nForest Birds: Forest birds - ambient seamless loop — Magnesus\nhttps://freesound.org/s/723913/\n\nBoth recordings: CC0 1.0. Pauso uses edited, level-adjusted loops. Noise is generated locally.\nhttps://creativecommons.org/publicdomain/zero/1.0/").setPositiveButton("OK",null).show();
  });
  if(dialog.getWindow()!=null)dialog.getWindow().setBackgroundDrawable(shape(BG,24));
 }
 private void chooseFile(){
  Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("audio/*").addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
  try{startActivityForResult(i,7);}catch(ActivityNotFoundException e){PlaybackService.error="No file picker found on this device.";render();}
 }
 private void buildRhythm(LinearLayout root){
  TextView heading=text("Rhythms",20,INK);heading.setTypeface(android.graphics.Typeface.create("sans-serif-medium",0));add(root,heading,22);
  for(int row=0;row<2;row++){
   LinearLayout line=new LinearLayout(this);line.setOrientation(0);add(root,line,8);
   for(int col=0;col<2;col++){
    final int index=row*2+col;int[] preset=PlanStore.PRESETS[index];
    String label=preset[0]+" min Play\n"+preset[1]+" min Break";
    SpannableString styled=new SpannableString(label);int newline=label.indexOf('\n');styled.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),0,newline,0);styled.setSpan(new RelativeSizeSpan(.82f),newline+1,label.length(),0);
    Button b=button("",false);b.setText(styled);b.setTextSize(16);b.setLineSpacing(dp(3),1);b.setContentDescription(preset[0]+" minutes Play, "+preset[1]+" minutes Break");presetButtons[index]=b;
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(64),1);if(col==1)lp.leftMargin=dp(8);line.addView(b,lp);
    b.setOnClickListener(v->{plans.preset(index);planChanged();});
   }
  }
  customButton=button("Custom Rhythm",false);customButton.setTextSize(14);customButton.setBackground(shape(BG,12));customButton.setTextColor(ACCENT);LinearLayout.LayoutParams customLP=new LinearLayout.LayoutParams(-1,dp(44));customLP.topMargin=dp(3);root.addView(customButton,customLP);customButton.setOnClickListener(v->customDialog());
  planPreview=text("",14,INK);planPreview.setGravity(Gravity.CENTER);add(root,planPreview,4);
 }
 private String time(long epoch){
  SimpleDateFormat clock=new SimpleDateFormat("HH:mm",Locale.getDefault()),date=new SimpleDateFormat("yyyyMMdd",Locale.US);
  return clock.format(new Date(epoch))+(date.format(new Date(epoch)).equals(date.format(new Date()))?"":" (next day)");
 }
 private String preview(Schedule.Rhythm rhythm,long now){
  Schedule.Snapshot t=rhythm.at(now,TimeZone.getDefault());
  return rhythm.work+" min Play · "+rhythm.rest+" min Break\n"+(t.quiet?"Break ends at ":"Next break at ")+time(now+t.remaining);
 }
 private void planChanged(){if(PlaybackService.running)startService(new Intent(this,PlaybackService.class).setAction(PlaybackService.PLAN));lastSelected=-99;render();}
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
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=7||result!=RESULT_OK||data==null||data.getData()==null)return;
  Uri uri=data.getData();
  try{
   getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
   String title="Selected audio";
   try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())title=c.getString(0);}
   String old=prefs.getString("uri","");
   prefs.edit().putString("sound_id","local").putString("uri",uri.toString()).putString("title",title).putInt("position",uri.toString().equals(old)?prefs.getInt("position",0):0).apply();
   if(!old.isEmpty()&&!old.equals(uri.toString()))try{getContentResolver().releasePersistableUriPermission(Uri.parse(old),Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
   PlaybackService.error="";file.setText(title);soundChanged();
  }catch(Exception e){PlaybackService.error="Could not retain access. Choose a downloaded audio file from your phone.";}render();
 }
 @Override protected void onResume(){super.onResume();handler.removeCallbacks(refresh);refresh.run();if(updates!=null)updates.check(false);}
 @Override protected void onPause(){stopPreview();handler.removeCallbacks(refresh);super.onPause();}
 private void render(){
  if(state==null)return;
  boolean active=PlaybackService.running,paused=active&&"Paused".equals(PlaybackService.status);Schedule.Snapshot timing=plans.now();long seconds=(timing.remaining+999)/1000;
  count.setTextSize(seconds>=3600?35:48);count.setText(seconds>=3600?String.format(Locale.getDefault(),"%d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60):String.format(Locale.getDefault(),"%02d:%02d",seconds/60,seconds%60));
  String label;int icon;
  if(!active){label="Ready";icon=R.drawable.ic_status_music;}
  else if(paused){label="Paused";icon=R.drawable.ic_status_pause;}
  else if("Loading music".equals(PlaybackService.status)){label="Loading";icon=R.drawable.ic_status_loading;}
  else if("Interrupted".equals(PlaybackService.status)){label="Interrupted";icon=R.drawable.ic_status_pause;}
  else if(timing.quiet){label="Break";icon=R.drawable.ic_status_rest;}
  else{label="Playing";icon=R.drawable.ic_status_music;}
  state.setText(label);state.setTextColor(active?INK:MUTED);
  if(lastStateIcon!=icon){stateIcon.setImageResource(icon);lastStateIcon=icon;}stateIcon.setAlpha(active?1f:.5f);
  file.setText(sounds.title());
  detail.setText(timing.quiet?"until the break ends":"until your next break");
  int playIcon=active&&!paused?R.drawable.ic_control_pause:R.drawable.ic_control_play;if(lastPlayIcon!=playIcon){play.setImageResource(playIcon);lastPlayIcon=playIcon;}
  play.setContentDescription(!active?"Start playback":paused?"Resume playback":"Pause playback");
  stop.setEnabled(active);stop.setAlpha(active?1f:.35f);choose.setEnabled(true);choose.setAlpha(1f);choose.setText("Sounds");
  Schedule.Rhythm rhythm=plans.load();planPreview.setText((timing.quiet?"Break ends at ":"Next break at ")+time(System.currentTimeMillis()+timing.remaining));
  int selected=plans.selected();if(selected!=lastSelected){
   for(int i=0;i<4;i++){boolean chosen=selected==i;presetButtons[i].setBackground(ripple(chosen?selectedShape():shape(CARD,14)));presetButtons[i].setTextColor(chosen?ACCENT:INK);presetButtons[i].setSelected(chosen);}
   customButton.setText(selected==-1?"Custom Rhythm · "+rhythm.work+" / "+rhythm.rest:"Custom Rhythm");customButton.setSelected(selected==-1);lastSelected=selected;
  }
  error.setText(PlaybackService.error);error.setVisibility(PlaybackService.error.isEmpty()?View.GONE:View.VISIBLE);dial.invalidate();
 }
 private class Dial extends View {
  private final Paint p=new Paint(3);
  Dial(Context c){super(c);setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
  @Override protected void onDraw(Canvas c){super.onDraw(c);float pad=dp(7);RectF r=new RectF(pad,pad,getWidth()-pad,getHeight()-pad);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setStrokeCap(Paint.Cap.ROUND);p.setColor(CARD);c.drawArc(r,-90,360,false,p);p.setColor(ACCENT);Schedule.Snapshot timing=plans.now();float sweep=(float)(360.0*timing.phase/timing.cycle);c.drawArc(r,-90,sweep,false,p);}
 }
}
