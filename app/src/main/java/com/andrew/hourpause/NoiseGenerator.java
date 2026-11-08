package com.andrew.hourpause;
import java.util.Random;
/** Continuous independent noise, no recorded loop. Output is normalized PCM in [-1,1]. */
public final class NoiseGenerator {
 public static final int RATE=44100;
 private final String type;private final Random random;
 private final double[] rows=new double[16];private double sum,brown,dcPrevious,dcOut;private int counter;
 public NoiseGenerator(String type,long seed){
  if(!type.equals("white")&&!type.equals("pink")&&!type.equals("brown"))throw new IllegalArgumentException("Unknown noise");
  this.type=type;random=new Random(seed);for(int i=0;i<rows.length;i++){rows[i]=white();sum+=rows[i];}
 }
 private double white(){return random.nextDouble()*2-1;}
 public float next(){
  double value;
  if(type.equals("white"))value=white()*.19;
  else if(type.equals("brown")){brown=.995*brown+.02*white();value=brown*.95;}
  else{counter=(counter+1)&65535;int index=counter==0?15:Integer.numberOfTrailingZeros(counter);sum-=rows[index];rows[index]=white();sum+=rows[index];value=(sum+white())*.047;}
  // Reject DC/very slow drift while retaining the different spectral balances.
  double filtered=value-dcPrevious+.9995*dcOut;dcPrevious=value;dcOut=filtered;
  return (float)Math.max(-.8,Math.min(.8,filtered));
 }
}
