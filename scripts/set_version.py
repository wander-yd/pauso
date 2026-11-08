import argparse,re,json,pathlib
p=argparse.ArgumentParser();p.add_argument('--code',type=int,required=True);p.add_argument('--name',required=True);a=p.parse_args()
if not 12<=a.code<2100000000 or not re.fullmatch(r'[0-9][A-Za-z0-9._-]{0,63}',a.name):raise SystemExit('Invalid version')
manifest=pathlib.Path('app/src/main/AndroidManifest.xml');s=manifest.read_text();s=re.sub(r'android:versionCode="\d+"',f'android:versionCode="{a.code}"',s);s=re.sub(r'android:versionName="[^"]+"',f'android:versionName="{a.name}"',s);manifest.write_text(s)
pathlib.Path('build').mkdir(exist_ok=True);pathlib.Path('build/version.json').write_text(json.dumps({'versionCode':a.code,'versionName':a.name}))
