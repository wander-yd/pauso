"""Developer-only numerical checks after ./test.sh and ./build.sh (numpy/scipy/ffmpeg)."""
from pathlib import Path
import numpy as np,subprocess,zipfile,os
from scipy.signal import welch
root=Path(__file__).resolve().parents[1]
for name,expected in [('white',0),('pink',-1),('brown',-2)]:
 x=np.fromfile(root/f'build/noise-{name}.f32',dtype='>f4').astype(float)
 f,p=welch(x,44100,nperseg=8192);bands=np.geomspace(100,8000,25);centers=[];powers=[]
 for a,b in zip(bands[:-1],bands[1:]):
  mask=(f>=a)&(f<b);centers.append(np.sqrt(a*b));powers.append(p[mask].mean())
 slope=np.polyfit(np.log10(centers),np.log10(powers),1)[0]
 assert abs(slope-expected)<.4,(name,slope)
 print(name,'PSD slope',round(slope,3))
with zipfile.ZipFile(root/os.environ.get('PAUSO_CHECK_APK','build/Pauso.apk')) as z:
 for name in ['forest_rain','forest_birds']:
  items=[i for i in z.infolist() if i.filename.endswith(name+'.ogg')]
  assert len(items)==1 and items[0].compress_type==0,'Require raw AssetFileDescriptor access'
for name in ['forest_rain','forest_birds']:
 x=np.frombuffer(subprocess.check_output(['ffmpeg','-v','error','-i',str(root/f'app/src/main/res/raw/{name}.ogg'),'-ac','1','-ar','44100','-f','f32le','-']),dtype='<f4')
 assert len(x)>20*44100 and np.isfinite(x).all() and np.max(abs(x))<.8
 assert abs(x[0]-x[-1])<.003
 assert .07<np.sqrt(np.mean(x*x))<.11
 print(name,'decode/level/seam checks passed')
print('Numerical signal checks passed. This is not a physical-device listening test.')
