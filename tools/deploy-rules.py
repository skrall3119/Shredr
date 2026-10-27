"""Run in an authorized Google Cloud Shell: python3 tools/deploy-rules.py inspect|deploy.

Copies public fields and legacy likes without deleting originals. Backs up current
rules and source documents in a private timestamped directory outside the checkout.
Never prints account data or access tokens. Requires gcloud and requests.
"""
import datetime
import json
import os
from pathlib import Path
import subprocess
import sys
import requests

PROJECT = 'jamr-7f77c'
BUCKET = 'jamr-7f77c.appspot.com'
ROOT = Path(__file__).resolve().parent.parent
DOCS = f'projects/{PROJECT}/databases/(default)/documents'
RULES = 'https://firebaserules.googleapis.com/v1/'
FIRESTORE = 'https://firestore.googleapis.com/v1/'
session = requests.Session()
session.headers['Authorization'] = 'Bearer ' + subprocess.check_output(
    ['gcloud', 'auth', 'print-access-token'], text=True).strip()

def call(method, url, **kw):
    response = session.request(method, url, timeout=60, **kw)
    if not response.ok:
        raise RuntimeError(f'{method} failed: HTTP {response.status_code}: {response.text[:500]}')
    return response.json()

def collection(name):
    result, token = [], None
    while True:
        params = {'pageSize': 300}
        if token:
            params['pageToken'] = token
        page = call('GET', FIRESTORE + DOCS + '/' + name, params=params)
        result.extend(page.get('documents', []))
        token = page.get('nextPageToken')
        if not token:
            return result

def main():
    if len(sys.argv) != 2 or sys.argv[1] not in ('inspect', 'deploy'):
        raise SystemExit('Usage: deploy-rules.py inspect|deploy')
    releases = {}
    for release in ['cloud.firestore', 'firebase.storage/' + BUCKET]:
        entry = call('GET', RULES + f'projects/{PROJECT}/releases/{release}')
        releases[release] = {'release': entry, 'ruleset': call('GET', RULES + entry['rulesetName'])}
    profiles, likes = collection('users'), collection('user-likes')
    existing = collection('public-profiles')
    print(json.dumps({'project': PROJECT, 'legacyProfiles': len(profiles),
        'legacyLikeMaps': len(likes), 'publicProfiles': len(existing),
        'rulesets': {k:v['release']['rulesetName'] for k,v in releases.items()}}))
    if sys.argv[1] == 'inspect':
        return
    stamp = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    backup = Path.home() / ('shredr-rules-backup-' + stamp)
    backup.mkdir(mode=0o700)
    data = backup / 'backup.json'
    data.write_text(json.dumps({'releases':releases,'users':profiles,'likes':likes,'publicProfiles':existing}))
    os.chmod(data, 0o600)
    # Compile both rule files first. Compilation errors leave active rules untouched.
    compiled = {}
    for release, filename in [('cloud.firestore','firestore.rules'), ('firebase.storage/'+BUCKET,'storage.rules')]:
        result = call('POST', RULES + f'projects/{PROJECT}/rulesets', json={
            'source': {'files': [{'name':filename,'content':(ROOT / filename).read_text()}]}})
        compiled[release] = result['name']
    known = {p['name'].split('/')[-1] for p in profiles + existing}
    present = {p['name'].split('/')[-1] for p in existing}
    writes = []
    defaults = {'fname':'Member','city':'','age':'','bio':'','pic':''}
    for profile in profiles:
        uid = profile['name'].split('/')[-1]
        if uid in present:
            continue
        old = profile.get('fields', {})
        fields = {'id': {'stringValue':uid}}
        for field, default in defaults.items():
            value = old.get(field, {}).get('stringValue', old.get(field, {}).get('integerValue', default))
            fields[field] = {'stringValue': str(value)}
        if 'demoGroup' in old and 'stringValue' in old['demoGroup']:
            fields['demoGroup'] = old['demoGroup']
        writes.append({'update':{'name':DOCS+'/public-profiles/'+uid,'fields':fields}, 'currentDocument':{'exists':False}})
    # Idempotent create-only like migration; do not overwrite newer individual likes.
    for old in likes:
        uid = old['name'].split('/')[-1]
        if uid not in known:
            continue
        current = {p['name'].split('/')[-1] for p in collection('user-likes/'+uid+'/likes')}
        for other in old.get('fields', {}):
            if other in known and other != uid and other not in current:
                writes.append({'update':{'name':DOCS+f'/user-likes/{uid}/likes/{other}',
                    'fields':{'id':{'stringValue':other}}},'currentDocument':{'exists':False}})
    for offset in range(0, len(writes), 200):
        call('POST', FIRESTORE + f'projects/{PROJECT}/databases/(default)/documents:commit',
            json={'writes':writes[offset:offset+200]})
    for release, ruleset in compiled.items():
        name = f'projects/{PROJECT}/releases/{release}'
        call('PATCH', RULES + name, json={'release':{'name':name,'rulesetName':ruleset},'updateMask':'rulesetName'})
        assert call('GET', RULES + name)['rulesetName'] == ruleset
    print(json.dumps({'deployed':compiled,'migratedDocuments':len(writes),'backup':str(backup)}))

if __name__ == '__main__':
    main()
