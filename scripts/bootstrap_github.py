"""One-time owner-run setup using an authenticated GitHub CLI. Never prints secrets.
Run from the project root after reviewing AUTOMATIC_UPDATES.md.
The default stages everything with public publishing DISABLED.
"""
import argparse,base64,getpass,json,os,pathlib,re,subprocess,sys
ROOT=pathlib.Path(__file__).resolve().parents[1]
SOURCE='wander-yd/pauso';PUBLIC='wander-yd/pauso-releases'
def run(args,data=None):
 p=subprocess.run(args,input=data,stdout=subprocess.PIPE,stderr=subprocess.PIPE,cwd=ROOT)
 if p.returncode:raise RuntimeError('Command failed: '+args[0]+' '+args[1]+' (check authentication/permissions; secret values omitted)')
 return p.stdout
def gh(*args,data=None):return run(['gh',*args],data)
def api(path,payload=None):
 args=['api',path]
 if payload is not None:args+=['--method','POST','--input','-']
 return json.loads(gh(*args,data=json.dumps(payload).encode() if payload is not None else None))
def secret(name,value):gh('secret','set',name,'--repo',SOURCE,data=value.encode())
def put(repo,path,content):
 try:sha=api(f'repos/{repo}/contents/{path}')['sha']
 except RuntimeError:sha=None
 body={'message':'Prepare Pauso release information','content':base64.b64encode(content).decode()}
 if sha:body['sha']=sha
 gh('api',f'repos/{repo}/contents/{path}','--method','PUT','--input','-',data=json.dumps(body).encode())
def create(repo,private):
 try:
  meta=api(f'repos/{repo}')
  if meta['private']!=private:raise ValueError('Existing repository visibility does not match the approved plan')
 except RuntimeError:
  gh('repo','create',repo,'--private' if private else '--public','--add-readme','--description','Pauso source' if private else 'Pauso Android downloads and update information')
def upload_source():
 # Publish only tracked source files, never a source archive containing private material.
 paths=run(['git','ls-files','-z']).decode().split('\0');entries=[]
 for name in filter(None,paths):
  p=ROOT/name
  if p.suffix in ('.p12','.jks','.keystore','.zip') or name.endswith('.local.env') or '.secrets/' in name:raise ValueError('Private/generated file tracked; refusing upload')
  content=p.read_bytes()
  if (b'-----BEGIN '+b'PRIVATE KEY-----') in content or (b'-----BEGIN '+b'RSA PRIVATE KEY-----') in content or re.search(rb'(?:ghp_|github_pat_)[A-Za-z0-9_]{30,}',content):raise ValueError('Possible credential in tracked source; refusing upload')
  blob=api(f'repos/{SOURCE}/git/blobs',{'content':base64.b64encode(content).decode(),'encoding':'base64'})
  entries.append({'path':name,'mode':'100755' if p.stat().st_mode&0o111 else '100644','type':'blob','sha':blob['sha']})
 head=api(f'repos/{SOURCE}/git/ref/heads/main')['object']['sha'];commit=api(f'repos/{SOURCE}/git/commits/{head}')
 tree=api(f'repos/{SOURCE}/git/trees',{'base_tree':commit['tree']['sha'],'tree':entries})
 new=api(f'repos/{SOURCE}/git/commits',{'message':'Prepare automatic Android builds and update checking','tree':tree['sha'],'parents':[head]})
 gh('api',f'repos/{SOURCE}/git/refs/heads/main','--method','PATCH','--input','-',data=json.dumps({'sha':new['sha'],'force':False}).encode())
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--enable-publication',action='store_true');args=parser.parse_args()
 if gh('api','user','--jq','.login').decode().strip()!='wander-yd':raise ValueError('Authenticate the approved owner wander-yd')
 create(SOURCE,True);create(PUBLIC,False)
 # Disable publication BEFORE pushing any workflow or configuring credentials.
 gh('variable','set','PAUSO_PUBLISH_ENABLED','--repo',SOURCE,'--body','false')
 upload_source();put(PUBLIC,'README.md',(ROOT/'distribution/README.md').read_bytes())
 env={}
 for line in (ROOT/'signing.local.env').read_text().splitlines():
  if '=' in line:
   k,v=line.split('=',1);env[k]=v
 key=pathlib.Path(env['PAUSO_KEYSTORE_PATH']);key=key if key.is_absolute() else ROOT/key
 secret('PAUSO_KEYSTORE_B64',base64.b64encode(key.read_bytes()).decode())
 secret('PAUSO_STORE_PASSWORD',env['PAUSO_STORE_PASSWORD']);secret('PAUSO_KEY_PASSWORD',env['PAUSO_KEY_PASSWORD'])
 print('Signing Secrets configured. Enter a fine-grained token selected ONLY for wander-yd/pauso-releases, Contents: Read and write. Do not paste it into chat.')
 token=getpass.getpass('Release repository token (hidden): ')
 if not token:raise ValueError('No release token supplied')
 secret('PAUSO_RELEASE_TOKEN',token)
 if args.enable_publication:
  gh('variable','set','PAUSO_PUBLISH_ENABLED','--repo',SOURCE,'--body','true')
  gh('workflow','run','android.yml','--repo',SOURCE,'--ref','main')
  print('Publication enabled; inspect the Actions run before claiming a release exists.')
 else:print('Setup staged. Public APK publication is DISABLED pending approval.')
if __name__=='__main__':
 try:main()
 except Exception as e:print(str(e),file=sys.stderr);sys.exit(1)
