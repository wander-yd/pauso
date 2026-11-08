package com.andrew.hourpause;
import java.net.URI;
/** Untrusted update metadata is checked before any browser link is opened. */
public final class UpdateInfo {
 public static final String REPOSITORY="wander-yd/pauso-releases",PACKAGE="com.andrew.hourpause";
 public final int versionCode,minSdk;public final String versionName,downloadUrl,notes,sha256;
 public UpdateInfo(String packageName,int code,String name,int sdk,String url,String notes,String sha){
  if(!PACKAGE.equals(packageName)||code<1||sdk<26||sdk>100||name==null||!name.matches("[0-9][A-Za-z0-9._-]{0,63}")||sha==null||!sha.matches("[a-fA-F0-9]{64}"))throw new IllegalArgumentException("Invalid update metadata");
  try{URI u=new URI(url);String base="/"+REPOSITORY+"/releases/download/";
   if(!"https".equals(u.getScheme())||!"github.com".equals(u.getHost())||u.getPort()!=-1||u.getUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null||!u.getRawPath().equals(u.getPath())||!u.getPath().startsWith(base))throw new IllegalArgumentException("Invalid download URL");
   String tail=u.getPath().substring(base.length());if(!tail.matches("v[0-9][A-Za-z0-9._-]{0,63}/Pauso\\.apk"))throw new IllegalArgumentException("Invalid release asset");
  }catch(java.net.URISyntaxException e){throw new IllegalArgumentException("Invalid download URL");}
  versionCode=code;versionName=name;minSdk=sdk;downloadUrl=url;this.notes=notes==null?"":notes.substring(0,Math.min(4000,notes.length()));sha256=sha;
 }
 public static int integer(Object value){if(!(value instanceof Integer)&&!(value instanceof Long))throw new IllegalArgumentException("Expected integer");long n=((Number)value).longValue();if(n<1||n>2100000000L)throw new IllegalArgumentException("Invalid integer range");return (int)n;}
 public boolean newerThan(long installed,int sdk){return versionCode>installed&&minSdk<=sdk;}
}
