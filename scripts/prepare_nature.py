# Offline preparation from the two documented CC0 sources. Not used by Android.
import numpy as np,subprocess,pathlib,json,hashlib
base=pathlib.Path(__file__).resolve().parents[1];scratch=pathlib.Path('audio-work');result={}
for name,duration,cross in [('rain',80,2),('forest',26,1)]:
 source=scratch/(name+'-source.mp3')
 decoded=subprocess.check_output(['ffmpeg','-v','error','-i',str(source),'-t',str(duration),'-ar','44100','-ac','1','-f','f32le','-'])
 x=np.frombuffer(decoded,dtype='<f4').copy();x-=x.mean();n=int(cross*44100)
 blend=np.linspace(0,1,n,dtype=np.float32)
 seam=x[-n:]*(1-blend)+x[:n]*blend
 x=np.concatenate([x[n:-n],seam])
 rms=np.sqrt(np.mean(x*x));x*=.095/max(rms,1e-8)
 # Gentle peak compression avoids sharp birdsong peaks, then align RMS.
 x=.32*np.tanh(x/.32);x*=.09/max(np.sqrt(np.mean(x*x)),1e-8)
 peak=np.max(np.abs(x))
 if peak>.72:x*=.72/peak
 # Tiny seam guard against lossy-codec edge clicks (not a repeated long silence).
 edge=441;x[:edge]*=np.linspace(0,1,edge);x[-edge:]*=np.linspace(1,0,edge)
 target=base/'app/src/main/res/raw'/('forest_rain.ogg' if name=='rain' else 'forest_birds.ogg')
 subprocess.run(['ffmpeg','-v','error','-y','-f','f32le','-ar','44100','-ac','1','-i','-','-c:a','libvorbis','-q:a','4',str(target)],input=x.astype('<f4').tobytes(),check=True)
 y=np.frombuffer(subprocess.check_output(['ffmpeg','-v','error','-i',str(target),'-f','f32le','-']),dtype='<f4')
 result[name]={'seconds':len(y)/44100,'rms':float(np.sqrt(np.mean(y*y))),'peak':float(np.max(abs(y))),'loop_boundary_step':float(abs(y[-1]-y[0])),'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'bytes':target.stat().st_size}
print(json.dumps(result,indent=2));(scratch/'audio-checks.json').write_text(json.dumps(result,indent=2))
