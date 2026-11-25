package com.andrew.hourpause;
import android.content.*;import android.database.Cursor;import android.net.Uri;import android.os.Build;import android.provider.*;import java.util.*;
public final class MediaLibrary {
 public static String permission(){return Build.VERSION.SDK_INT>=33?"android.permission.READ_MEDIA_AUDIO":"android.permission.READ_EXTERNAL_STORAGE";}
 public static ArrayList<Track> scan(Context c){
  ArrayList<Track> list=new ArrayList<>();Uri collection=MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
  String[] fields={MediaStore.Audio.Media._ID,MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ARTIST,MediaStore.Audio.Media.DURATION,MediaStore.Audio.Media.DISPLAY_NAME};
  try(Cursor cursor=c.getContentResolver().query(collection,fields,null,null,MediaStore.Audio.Media.TITLE+" COLLATE NOCASE ASC")){
   if(cursor!=null)while(cursor.moveToNext()&&list.size()<LibraryState.MAX_TRACKS){String title=cursor.getString(1);if(title==null||title.isEmpty())title=cursor.getString(4);list.add(new Track(ContentUris.withAppendedId(collection,cursor.getLong(0)).toString(),title,cursor.getString(2),cursor.getLong(3)));}
  }return list;
 }
 public static Track document(Context c,Uri uri){String name="Selected audio";try(Cursor cursor=c.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())name=cursor.getString(0);}return new Track(uri.toString(),name,"",0);}
 public static Track metadata(Context context,Track fallback){
  android.media.MediaMetadataRetriever reader=new android.media.MediaMetadataRetriever();
  try{
   reader.setDataSource(context,Uri.parse(fallback.uri));
   String title=reader.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE);
   String artist=reader.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST);
   String duration=reader.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
   return new Track(fallback.uri,title==null||title.trim().isEmpty()?fallback.title:title,artist,duration==null?0:Long.parseLong(duration));
  }catch(RuntimeException e){return fallback;}finally{try{reader.release();}catch(Exception ignored){}}
 }
}
