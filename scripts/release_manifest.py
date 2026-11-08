import hashlib,json,pathlib,re,subprocess
REPO='wander-yd/pauso-releases'
def make(apk,code,name,notes):
 if not 12<=code<2100000000 or not re.fullmatch(r'[0-9][A-Za-z0-9._-]{0,63}',name):raise ValueError('Invalid version')
 digest=hashlib.sha256(apk.read_bytes()).hexdigest()
 return {'packageName':'com.andrew.hourpause','versionCode':code,'versionName':name,'minSdk':26,'downloadUrl':f'https://github.com/{REPO}/releases/download/v{name}/Pauso.apk','apkSha256':digest,'apkSizeBytes':apk.stat().st_size,'releaseNotes':notes[:4000]}
if __name__=='__main__':
 v=json.loads(pathlib.Path('build/version.json').read_text());apk=pathlib.Path('build/Pauso.apk');notes=pathlib.Path('RELEASE_NOTES.md').read_text()
 data=make(apk,v['versionCode'],v['versionName'],notes);pathlib.Path('build/update.json').write_text(json.dumps(data,indent=2)+'\n');pathlib.Path('build/SHA256SUMS').write_text(data['apkSha256']+'  Pauso.apk\n')
