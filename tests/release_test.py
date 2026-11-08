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
  script=(ROOT/'scripts/publish_release.sh').read_text();self.assertLess(script.index('gh release upload'),script.index('--draft=false'));self.assertIn('non-increasing',script)
 def test_no_installer_permission(self):
  app=E.parse(ROOT/'app/src/main/AndroidManifest.xml').getroot();names={x.get('{http://schemas.android.com/apk/res/android}name') for x in app.findall('uses-permission')}
  self.assertIn('android.permission.INTERNET',names);self.assertNotIn('android.permission.REQUEST_INSTALL_PACKAGES',names)
if __name__=='__main__':unittest.main()
