const {before, after, beforeEach, test} = require('node:test');
const fs = require('node:fs');
const {initializeTestEnvironment, assertFails, assertSucceeds} = require('@firebase/rules-unit-testing');
const {doc, collection, getDoc, getDocs, setDoc, updateDoc, deleteDoc, writeBatch} = require('firebase/firestore');
const {ref, uploadBytes, getBytes, deleteObject} = require('firebase/storage');
let env;
const profile = id => ({id, fname: id, age:'25', city:'Chicago', bio:'Musician', pic:''});
const db = id => id ? env.authenticatedContext(id).firestore() : env.unauthenticatedContext().firestore();
const like = (a,b) => `user-likes/${a}/likes/${b}`;
const match = (a,b) => `user-matches/${a}/matches/${b}`;
const msg = (a,b,id='m') => `user-messages/${a}/${b}/${id}`;
const message = () => ({id:'m',text:'Hello',fromId:'alice',toId:'bob',timeStamp:Math.floor(Date.now()/1000)});
before(async () => {
  env = await initializeTestEnvironment({projectId:'demo-shredr',
    firestore:{rules:fs.readFileSync('firestore.rules','utf8')},
    storage:{rules:fs.readFileSync('storage.rules','utf8')}});
});
after(async () => { await env.cleanup(); });
beforeEach(async () => {
  await env.clearFirestore();
  await env.clearStorage();
  await env.withSecurityRulesDisabled(async c => {
    for (const id of ['alice','bob','mallory']) await setDoc(doc(c.firestore(),`public-profiles/${id}`),profile(id));
    await setDoc(doc(c.firestore(),'users/alice'),{email:'private@example.com'});
  });
});
async function likes() {
  await setDoc(doc(db('alice'),like('alice','bob')),{id:'bob'});
  await setDoc(doc(db('bob'),like('bob','alice')),{id:'alice'});
}
async function matches() {
  await likes(); const d=db('alice'), b=writeBatch(d);
  b.set(doc(d,match('alice','bob')),profile('bob'));
  b.set(doc(d,match('bob','alice')),profile('alice'));
  await assertSucceeds(b.commit());
}
test('profiles are discoverable only to signed-in users; legacy private data stays owner-only',async()=>{
  await assertFails(getDocs(collection(db(),'public-profiles')));
  await assertSucceeds(getDocs(collection(db('bob'),'public-profiles')));
  await assertSucceeds(getDoc(doc(db('alice'),'users/alice')));
  await assertFails(getDoc(doc(db('bob'),'users/alice')));
  await assertFails(getDocs(collection(db('alice'),'users')));
});
test('only owners edit profiles, with bounded public fields and immutable identity',async()=>{
  await assertSucceeds(updateDoc(doc(db('alice'),'public-profiles/alice'),{bio:'New bio'}));
  await assertFails(updateDoc(doc(db('bob'),'public-profiles/alice'),{bio:'Hacked'}));
  await assertFails(updateDoc(doc(db('alice'),'public-profiles/alice'),{email:'leak@example.com'}));
  await assertFails(updateDoc(doc(db('alice'),'public-profiles/alice'),{id:'bob'}));
  await assertFails(updateDoc(doc(db('alice'),'public-profiles/alice'),{bio:'x'.repeat(2001)}));
  await assertSucceeds(setDoc(doc(db('new'),'public-profiles/new'),{id:'new',fname:'New',age:'20'}));
});
test('likes cannot be forged or enumerated by other users',async()=>{
  await assertSucceeds(setDoc(doc(db('alice'),like('alice','bob')),{id:'bob'}));
  await assertSucceeds(getDoc(doc(db('bob'),like('alice','bob'))));
  await assertSucceeds(getDoc(doc(db('alice'),like('bob','alice'))));
  await assertSucceeds(getDocs(collection(db('alice'),'user-likes/alice/likes')));
  await assertFails(getDocs(collection(db('bob'),'user-likes/alice/likes')));
  await assertFails(getDoc(doc(db('mallory'),like('alice','bob'))));
  await assertFails(setDoc(doc(db('mallory'),like('alice','bob')),{id:'bob'}));
  await assertFails(setDoc(doc(db('alice'),like('alice','alice')),{id:'alice'}));
  await assertFails(setDoc(doc(db('alice'),like('alice','missing')),{id:'missing'}));
});
test('matches require reciprocal likes, correct profile snapshots and atomic mirrored records',async()=>{
  const d=db('alice');
  await assertFails(setDoc(doc(d,match('alice','bob')),profile('bob')));
  await likes();
  await assertFails(setDoc(doc(d,match('alice','bob')),profile('bob')));
  let b=writeBatch(d);b.set(doc(d,match('alice','bob')),{...profile('bob'),fname:'Fake'});b.set(doc(d,match('bob','alice')),profile('alice'));
  await assertFails(b.commit());
  const attacker=db('mallory');b=writeBatch(attacker);b.set(doc(attacker,match('alice','bob')),profile('bob'));b.set(doc(attacker,match('bob','alice')),profile('alice'));
  await assertFails(b.commit());
  await matches();
  await assertSucceeds(getDocs(collection(d,'user-matches/alice/matches')));
  await assertFails(getDocs(collection(db('mallory'),'user-matches/alice/matches')));
});
test('matched sender can send mirrored messages; unrelated users cannot read or forge them',async()=>{
  await matches(); const d=db('alice'), m=message(), b=writeBatch(d);
  b.set(doc(d,msg('alice','bob')),m);b.set(doc(d,msg('bob','alice')),m);await assertSucceeds(b.commit());
  await assertSucceeds(getDocs(collection(db('bob'),'user-messages/bob/alice')));
  await assertFails(getDocs(collection(db('mallory'),'user-messages/bob/alice')));
  await assertFails(updateDoc(doc(d,msg('alice','bob')),{text:'Changed'}));
  await assertFails(deleteDoc(doc(db('mallory'),msg('alice','bob'))));
  await assertSucceeds(deleteDoc(doc(d,msg('alice','bob'))));
});
test('unmatched, spoofed, oversized, backdated and one-sided messages fail',async()=>{
  const d=db('alice');
  async function send(m) {const b=writeBatch(d);b.set(doc(d,msg('alice','bob')),m);b.set(doc(d,msg('bob','alice')),m);return b.commit();}
  await assertFails(send(message()));await matches();
  await assertFails(send({...message(),fromId:'bob',toId:'alice'}));
  await assertFails(send({...message(),text:'x'.repeat(4001)}));
  await assertFails(send({...message(),timeStamp:0}));
  await assertFails(setDoc(doc(d,msg('alice','bob')),message()));
  await deleteDoc(doc(db('bob'),like('bob','alice')));
  await assertFails(send(message()));
});
test('default deny covers unknown Firestore paths',async()=>{
  await assertFails(setDoc(doc(db('alice'),'admin/settings'),{admin:true}));
  await assertFails(getDoc(doc(db('alice'),'admin/settings')));
});
test('photo ownership, type, size, authentication and path are enforced',async()=>{
  const own=env.authenticatedContext('alice').storage(), other=env.authenticatedContext('bob').storage();
  const path='users/alice/profile.jpg', bytes=new Uint8Array([255,216,255,217]);
  await assertSucceeds(uploadBytes(ref(own,path),bytes,{contentType:'image/jpeg'}));
  await assertSucceeds(getBytes(ref(other,path)));
  await assertFails(getBytes(ref(env.unauthenticatedContext().storage(),path)));
  await assertFails(uploadBytes(ref(other,path),bytes,{contentType:'image/jpeg'}));
  await assertFails(uploadBytes(ref(own,path),bytes,{contentType:'text/html'}));
  await assertFails(uploadBytes(ref(own,path),new Uint8Array(5*1024*1024+1),{contentType:'image/jpeg'}));
  await assertFails(uploadBytes(ref(own,'users/alice/other.jpg'),bytes,{contentType:'image/jpeg'}));
  await assertFails(deleteObject(ref(other,path)));
  await assertSucceeds(deleteObject(ref(own,path)));
});
