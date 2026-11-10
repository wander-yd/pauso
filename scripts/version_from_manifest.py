"""Keep the deliberate release version; CI run numbers are not product versions."""
import pathlib,subprocess,sys,xml.etree.ElementTree as ET

def main():
 root=pathlib.Path(__file__).resolve().parents[1]
 manifest=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot()
 namespace='{http://schemas.android.com/apk/res/android}'
 subprocess.run([sys.executable,str(root/'scripts/set_version.py'),'--code',manifest.attrib[namespace+'versionCode'],'--name',manifest.attrib[namespace+'versionName']],cwd=root,check=True)

if __name__=='__main__':main()
