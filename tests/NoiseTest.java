package com.andrew.hourpause;
import java.io.*;
public class NoiseTest {
 static void check(boolean c,String s){if(!c)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{
  double[] high=new double[3];int kind=0;
  for(String id:new String[]{"white","pink","brown"}){
   NoiseGenerator noise=new NoiseGenerator(id,12345),identical=new NoiseGenerator(id,12345);
   for(int i=0;i<10000;i++)check(noise.next()==identical.next(),"deterministic continuous state");
   noise=new NoiseGenerator(id,56789);int n=NoiseGenerator.RATE*12;double sum=0,energy=0,diff=0;float previous=0;double peak=0;
   new File("build").mkdirs();
   try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream("build/noise-"+id+".f32")))){
    for(int i=0;i<n;i++){float value=noise.next();check(Float.isFinite(value)&&Math.abs(value)<=.8,"bounded finite PCM");sum+=value;energy+=value*value;peak=Math.max(peak,Math.abs(value));if(i>0)diff+=(value-previous)*(value-previous);previous=value;out.writeFloat(value);}
   }
   double rms=Math.sqrt(energy/n);check(rms>.025&&rms<.2,"usable RMS");check(Math.abs(sum/n)<.025,"DC drift bound");high[kind++]=diff/energy;
   System.out.println(id+": RMS="+rms+", mean="+(sum/n)+", peak="+peak+", HF ratio="+(diff/energy));
  }
  check(high[0]>high[1]&&high[1]>high[2],"white/pink/brown have progressively softer high frequencies");
  try{new NoiseGenerator("unknown",0);throw new AssertionError("invalid accepted");}catch(IllegalArgumentException expected){}
  System.out.println("PASS: finite continuous noise, drift/amplitude bounds, deterministic stream and spectral ordering.");
 }
}
