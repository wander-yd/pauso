import json,re,pathlib
v=json.loads(pathlib.Path('build/version.json').read_text());s=pathlib.Path('build/package.txt').read_text()
assert "name='com.andrew.hourpause'" in s
assert "versionCode='"+str(v['versionCode'])+"'" in s
assert "versionName='"+v['versionName']+"'" in s
assert re.search(r"(?m)^(?:sdkVersion|minSdkVersion):'26'$",s)
assert 'android.permission.REQUEST_INSTALL_PACKAGES' not in s
