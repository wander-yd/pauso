"""Fail before CI build if private signing material or credentials are tracked."""
import pathlib,re,subprocess
root=pathlib.Path(__file__).resolve().parents[1]
paths=subprocess.check_output(['git','ls-files','-z'],cwd=root).decode().split('\0')
for name in filter(None,paths):
 p=root/name
 if p.suffix in ('.p12','.jks','.keystore','.zip') or name.endswith('.local.env') or name=='.env' or '.secrets/' in name:raise SystemExit('Private/generated file is tracked; remove it before uploading source.')
 b=p.read_bytes()
 if (b'-----BEGIN '+b'PRIVATE KEY-----') in b or (b'-----BEGIN '+b'RSA PRIVATE KEY-----') in b or re.search(rb'(?:ghp_|github_pat_)[A-Za-z0-9_]{30,}',b):raise SystemExit('Possible private credential in tracked source; remove it before uploading.')
print('Tracked source passes private-material exclusion checks.')
