import {firebaseConfig, useAppCheck, recaptchaEnterpriseSiteKey} from './firebase-config.js';
import {initializeApp} from 'https://www.gstatic.com/firebasejs/11.6.0/firebase-app.js';
import {getAuth,onAuthStateChanged,signInWithEmailAndPassword,signOut} from 'https://www.gstatic.com/firebasejs/11.6.0/firebase-auth.js';
import {getFirestore,collection,addDoc,doc,getDoc,getDocs,setDoc,updateDoc,query,where,serverTimestamp,limit} from 'https://www.gstatic.com/firebasejs/11.6.0/firebase-firestore.js';
import {getStorage,ref,uploadBytes,getDownloadURL} from 'https://www.gstatic.com/firebasejs/11.6.0/firebase-storage.js';
import {getFunctions,httpsCallable} from 'https://www.gstatic.com/firebasejs/11.6.0/firebase-functions.js';
import {initializeAppCheck,ReCaptchaEnterpriseProvider} from 'https://www.gstatic.com/firebasejs/11.6.0/firebase-app-check.js';

if(!firebaseConfig?.projectId) throw new Error('Firebase config missing.');
const app=initializeApp(firebaseConfig);
if(useAppCheck && recaptchaEnterpriseSiteKey) initializeAppCheck(app,{provider:new ReCaptchaEnterpriseProvider(recaptchaEnterpriseSiteKey),isTokenAutoRefreshEnabled:true});
const auth=getAuth(app),db=getFirestore(app),storage=getStorage(app),functions=getFunctions(app);
const listDocs=async(q)=>{const snapshot=await getDocs(q);return snapshot.docs.map(d=>({id:d.id,...d.data()}))};
const sortRecords=rows=>rows.sort((a,b)=>Number(b.createdAt?.seconds||0)-Number(a.createdAt?.seconds||0));
const normalizeContact=v=>String(v).trim().replace(/\s+/g,' ');
export const service={
 onAuth(cb){return onAuthStateChanged(auth,cb)},
 get user(){return auth.currentUser},
 async login(email,password){await signInWithEmailAndPassword(auth,email,password)},
 async logout(){await signOut(auth)},
 async role(uid){const s=await getDoc(doc(db,'roles',uid));return s.exists()?s.data().role:null},
 async profile(uid){const s=await getDoc(doc(db,'studentProfiles',uid));return s.exists()?s.data():null},
 async settings(){const s=await getDoc(doc(db,'siteSettings','public'));return s.exists()?s.data():{}},
 async submitAdmission(data){const x={studentName:normalizeContact(data.studentName),grade:String(data.grade),course:String(data.course),school:normalizeContact(data.school||''),preferredBatch:normalizeContact(data.preferredBatch||''),guardianName:normalizeContact(data.guardianName),contact:normalizeContact(data.contact),message:String(data.message||'').trim(),consent:true,status:'new',createdAt:serverTimestamp()};return await addDoc(collection(db,'admissions'),x)},
 async slots(){const rows=await listDocs(query(collection(db,'demoSlots'),where('active','==',true),limit(50)));return rows.filter(x=>new Date(x.startAt)>new Date()&&x.bookedCount<x.capacity).sort((a,b)=>new Date(a.startAt)-new Date(b.startAt))},
 async submitDemo(data){const x={slotId:String(data.slotId),grade:String(data.grade),subject:String(data.subject),guardianName:normalizeContact(data.guardianName),contact:normalizeContact(data.contact),message:String(data.message||'').trim(),status:'requested',consent:true,createdAt:serverTimestamp()};return await addDoc(collection(db,'demoBookings'),x)},
 async getQuestions(){return await listDocs(query(collection(db,'quizQuestions'),where('published','==',true),limit(250)))},
 async savePractice(attempt){if(!auth.currentUser)throw new Error('Sign in required');return await addDoc(collection(db,'practiceAttempts'),{...attempt,studentId:auth.currentUser.uid,createdAt:serverTimestamp(),kind:'unverified-practice'})},
 async myAttempts(studentId){if(!studentId)throw new Error('Student ID required');return sortRecords(await listDocs(query(collection(db,'practiceAttempts'),where('studentId','==',studentId),limit(100))))},
 async myDoubts(studentId){return sortRecords(await listDocs(query(collection(db,'doubts'),where('studentId','==',studentId),limit(50))))},
 async submitDoubt(data,file){if(!auth.currentUser)throw new Error('Sign in required');let imagePath='';if(file){if(!['image/jpeg','image/png','image/webp'].includes(file.type)||file.size>5*1024*1024)throw new Error('Choose JPG/PNG/WebP under 5 MB.');const basename=crypto.randomUUID();imagePath=`doubts/${auth.currentUser.uid}/${basename}`;await uploadBytes(ref(storage,imagePath),file,{contentType:file.type});}
 return await addDoc(collection(db,'doubts'),{studentId:auth.currentUser.uid,subjectKey:String(data.subjectKey),chapter:String(data.chapter).trim(),question:String(data.question).trim(),imagePath,status:'pending',answer:'',createdAt:serverTimestamp()})},
 async publicItems(name){if(!['results','gallery','announcements','studyMaterials'].includes(name))throw new Error('Invalid public content');return sortRecords(await listDocs(query(collection(db,name),where('published','==',true),limit(60))))},
 async assignedWork(grade){return await listDocs(query(collection(db,'homework'),where('grade','==',String(grade)),limit(75)))},
 async parentLinks(uid){return await listDocs(query(collection(db,'parentLinks'),where('guardianId','==',uid),limit(25)))},
 async adminList(name){if(!['admissions','demoBookings','demoSlots','quizQuestions','results','gallery','siteSettings','announcements','studyMaterials','homework','doubts'].includes(name))throw new Error('Unsupported collection');return sortRecords(await listDocs(query(collection(db,name),limit(100))))},
 async writeAdmin(name,data){if(!['quizQuestions','demoSlots','results','gallery','announcements','studyMaterials','homework'].includes(name))throw new Error('Unsupported collection');return await addDoc(collection(db,name),{...data,createdAt:serverTimestamp()})},
 async updateAdmin(name,id,data){if(!['admissions','demoBookings','quizQuestions','demoSlots','doubts','results','gallery','announcements'].includes(name))throw new Error('Unsupported collection');return await updateDoc(doc(db,name,id),data)},
 async writeSettings(data){return await setDoc(doc(db,'siteSettings','public'),data,{merge:true})},
 async confirmBooking(id){return await httpsCallable(functions,'confirmDemoBooking')({bookingId:id})},
 async teacherAssignments(uid){const s=await getDoc(doc(db,'teacherAssignments',uid));return s.exists()?s.data().subjectKeys||[]:[]},
 async teacherDoubts(subjectKeys){if(!subjectKeys.length)return [];return await listDocs(query(collection(db,'doubts'),where('subjectKey','in',subjectKeys.slice(0,10)),limit(100)))},
 async answerDoubt(id,answer){return await updateDoc(doc(db,'doubts',id),{answer,status:'answered',answeredAt:serverTimestamp()})},
 async resolveDoubt(id){return await updateDoc(doc(db,'doubts',id),{status:'resolved'})}
};
