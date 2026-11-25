package com.andrew.hourpause;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

final class FolderImport {
 interface Progress { void update(int finished, int total); }

 static LibraryState load(Context context) {
  return LibraryState.decode(context.getSharedPreferences("music_folders", 0).getString("state", ""));
 }

 static synchronized void save(Context context, LibraryState.Playlist folder) {
  if (folder.tracks.isEmpty()) return;
  LibraryState folders = load(context);
  LibraryState.Playlist existing = folders.find(folder.id);
  if (existing != null) {
   LibraryState.add(existing, folder.tracks);
  } else {
   if (folders.playlists.size() >= LibraryState.MAX_LISTS) throw new IllegalStateException("Folder limit reached");
   folders.playlists.add(folder);
  }
  if (!context.getSharedPreferences("music_folders", 0).edit().putString("state", folders.encode()).commit()) {
   throw new IllegalStateException("Cannot save folder");
  }
 }

 static LibraryState.Playlist scan(Context context, Uri tree, Progress progress) {
  ContentResolver resolver = context.getContentResolver();
  String rootId = DocumentsContract.getTreeDocumentId(tree);
  String id = tree.getAuthority() + ":" + rootId;
  Uri root = DocumentsContract.buildDocumentUriUsingTree(tree, rootId);
  LibraryState.Playlist folder = new LibraryState.Playlist(MediaLibrary.document(context, root).title);
  folder.id = id;
  LibraryState.Playlist previous = load(context).find(id);
  Set<String> imported = new HashSet<>();
  if (previous != null) {
   folder.tracks.addAll(previous.tracks);
   for (Track track : previous.tracks) imported.add(track.uri);
  }

  String[] columns = {
   DocumentsContract.Document.COLUMN_DOCUMENT_ID,
   DocumentsContract.Document.COLUMN_MIME_TYPE,
   DocumentsContract.Document.COLUMN_DISPLAY_NAME
  };
  ArrayDeque<String> directories = new ArrayDeque<>();
  Set<String> visited = new HashSet<>();
  ArrayList<Track> pending = new ArrayList<>();
  directories.add(rootId);
  while (!directories.isEmpty() && pending.size() + folder.tracks.size() < LibraryState.MAX_TRACKS) {
   String directory = directories.removeFirst();
   if (!visited.add(directory)) continue;
   Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, directory);
   try (Cursor cursor = resolver.query(children, columns, null, null, null)) {
    if (cursor == null) throw new IllegalStateException("Cannot read folder");
    while (pending.size() + folder.tracks.size() < LibraryState.MAX_TRACKS && cursor.moveToNext()) {
     String childId = cursor.getString(0);
     String mime = cursor.getString(1);
     String name = cursor.getString(2);
     if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
      directories.addLast(childId);
      continue;
     }
     if (!isAudio(mime, name)) continue;
     String uri = DocumentsContract.buildDocumentUriUsingTree(tree, childId).toString();
     if (imported.add(uri)) pending.add(new Track(uri, name, "", 0));
    }
   }
  }

  progress.update(0, pending.size());
  for (int start = 0; start < pending.size(); start += 200) {
   int end = Math.min(start + 200, pending.size());
   for (int i = start; i < end; i++) {
    Track track = pending.get(i);
    folder.tracks.add(MediaLibrary.metadata(context, track));
   }
   progress.update(end, pending.size());
  }
  return folder;
 }

 private static boolean isAudio(String mime, String name) {
  return (mime != null && audioMime(mime))
      || (name != null && name.toLowerCase(java.util.Locale.ROOT).matches(".*\\.(mp3|m4a|aac|ogg|oga|opus|wav|flac|alac|wma|aiff|aif|amr|m4b|mid|midi|ape|webm)$"));
 }

 private static boolean audioMime(String mime) {
  return mime.startsWith("audio/") || mime.equals("application/ogg");
 }
}
