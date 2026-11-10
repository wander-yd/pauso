import json,pathlib,shutil,subprocess,tempfile,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
class VersionTest(unittest.TestCase):
 def test_ci_preserves_deliberate_version(self):
  with tempfile.TemporaryDirectory() as directory:
   root=pathlib.Path(directory);(root/'scripts').mkdir();(root/'app/src/main').mkdir(parents=True)
   for n in ['set_version.py','version_from_manifest.py']:shutil.copyfile(ROOT/'scripts'/n,root/'scripts'/n)
   manifest=root/'app/src/main/AndroidManifest.xml';manifest.write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android" android:versionCode="1020" android:versionName="0.15.0"/>')
   before=manifest.read_bytes();subprocess.run(['python3','scripts/version_from_manifest.py'],cwd=root,check=True)
   self.assertEqual(manifest.read_bytes(),before);self.assertEqual(json.loads((root/'build/version.json').read_text()),{'versionCode':1020,'versionName':'0.15.0'})
 def test_workflow_does_not_invent_product_version(self):
  workflow=(ROOT/'.github/workflows/android.yml').read_text();self.assertIn('python3 scripts/version_from_manifest.py',workflow);self.assertNotIn('0.14.$GITHUB_RUN_NUMBER',workflow)
