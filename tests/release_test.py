import importlib.util,pathlib,tempfile,unittest,json,xml.etree.ElementTree as E
ROOT=pathlib.Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('release_manifest',ROOT/'scripts/release_manifest.py');mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
class ReleaseTest(unittest.TestCase):
 def test_manifest(self):
  with tempfile.TemporaryDirectory() as d:
   p=pathlib.Path(d)/'Pauso.apk';p.write_bytes(b'fixture-apk');m=mod.make(p,1002,'0.12.2','Public notes')
   self.assertEqual(m['versionCode'],1002);self.assertEqual(m['apkSizeBytes'],11);self.assertEqual(m['downloadUrl'],'https://github.com/wander-yd/pauso-releases/releases/download/v0.12.2/Pauso.apk');self.assertEqual(len(m['apkSha256']),64)
   for code,name in [(11,'0.12.2'),(2100000000,'0.12.2'),(1002,'../bad')]:
    with self.assertRaises(ValueError):mod.make(p,code,name,'')
 def test_publish_boundaries(self):
  workflow=(ROOT/'.github/workflows/android.yml').read_text();verify=workflow.split('  publish:')[0]
  self.assertNotIn('secrets.',verify);self.assertNotIn('pull_request_target',workflow);self.assertIn("vars.PAUSO_PUBLISH_ENABLED == 'true'",workflow);self.assertIn("github.ref == 'refs/heads/main'",workflow)
  script=(ROOT/'scripts/publish_release.sh').read_text();self.assertLess(script.index('gh release upload'),script.index('--draft=false'));self.assertIn('non-increasing',script);self.assertLess(script.index('--draft=false'),script.rindex('sync_manifest'));self.assertIn('Refusing to replace newer direct update metadata',script)
 def test_no_installer_permission(self):
  app=E.parse(ROOT/'app/src/main/AndroidManifest.xml').getroot();names={x.get('{http://schemas.android.com/apk/res/android}name') for x in app.findall('uses-permission')}
  self.assertIn('android.permission.INTERNET',names);self.assertNotIn('android.permission.REQUEST_INSTALL_PACKAGES',names)

class DirectManifestPublishTest(unittest.TestCase):
 def run_publish(self,old_code,published):
  import os,subprocess,base64
  with tempfile.TemporaryDirectory() as directory:
   root=pathlib.Path(directory);(root/'build').mkdir();(root/'bin').mkdir()
   new={'versionCode':1006,'versionName':'0.13.6'};(root/'build/update.json').write_text(json.dumps(new));(root/'RELEASE_NOTES.md').write_text('fixture')
   current={'sha':'fixture-sha','content':base64.b64encode(json.dumps({'versionCode':old_code}).encode()).decode()};(root/'fixture.json').write_text(json.dumps(current))
   stub=root/'bin/gh';stub.write_text('''#!/usr/bin/env python3
import sys,json,pathlib
args=sys.argv[1:];root=pathlib.Path.cwd()
with (root/'calls').open('a') as out:out.write(' '.join(args)+'\\n')
if args[:2]==['release','view'] and '--json' in args and args[args.index('--json')+1]=='tagName':
 print('v0.13.6');sys.exit(0)
if args[:2]==['release','view']:
 print('false' if (root/'published').exists() else '');sys.exit(0 if (root/'published').exists() else 1)
if args[:2]==['release','download']:sys.exit(1)
if args[0]=='api' and args[1].endswith('/releases'):print('0')
elif args[0]=='api' and '--method' in args:
 payload=json.loads(pathlib.Path(args[args.index('--input')+1]).read_text());(root/'submitted.json').write_text(json.dumps(payload));print('{}')
elif args[0]=='api':print((root/'fixture.json').read_text())
''');stub.chmod(0o755)
   if published:(root/'published').touch()
   env=dict(os.environ,PATH=str(root/'bin')+os.pathsep+os.environ['PATH'],GH_TOKEN='fixture-only')
   result=subprocess.run(['bash',str(ROOT/'scripts/publish_release.sh')],cwd=root,env=env,capture_output=True,text=True)
   calls=(root/'calls').read_text();payload=json.loads((root/'submitted.json').read_text()) if (root/'submitted.json').exists() else None
   return result,calls,payload
 def test_direct_metadata_after_publication(self):
  import base64
  result,calls,payload=self.run_publish(1005,False);self.assertEqual(result.returncode,0,result.stderr)
  self.assertLess(calls.index('release edit'),calls.index('--method PUT'))
  self.assertEqual(json.loads(base64.b64decode(payload['content']))['versionCode'],1006);self.assertEqual(payload['sha'],'fixture-sha')
 def test_rerun_repairs_metadata_without_republishing(self):
  result,calls,payload=self.run_publish(1005,True);self.assertEqual(result.returncode,0,result.stderr);self.assertIsNotNone(payload);self.assertNotIn('release upload',calls)
 def test_no_direct_metadata_regression(self):
  result,calls,payload=self.run_publish(1010,True);self.assertNotEqual(result.returncode,0);self.assertIsNone(payload);self.assertNotIn('--method PUT',calls)

if __name__=='__main__':unittest.main()
