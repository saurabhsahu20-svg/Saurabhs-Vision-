/* SAURABH'S VISION — offline caching for PUBLIC static resources only.
 * No Firebase auth, database, private dashboards, imported app banks or user submissions.
 */
const CACHE = 'sv-public-2026-11-11-v2';
const BASE = new URL(self.registration.scope);
const publicPaths = [
  './','./index.html','./app.js','./styles.css','./firebase-config.js',
  './assets/vision-icon-64.png','./assets/vision-icon-192.png','./assets/vision-icon-512.png','./assets/brand-3d.webp',
  './study-desk/','./study-sprint/','./admissions-guide/','./quiz-club/','./contact/'
];
const paths = new Set(publicPaths.map(p=>new URL(p,BASE).pathname));
self.addEventListener('install',event=>{
  event.waitUntil(
    caches.open(CACHE)
      .then(cache=>Promise.allSettled(publicPaths.map(p=>cache.add(new URL(p,BASE).href))))
      .then(()=>self.skipWaiting())
  );
});
self.addEventListener('activate',event=>{
  event.waitUntil(
    caches.keys().then(names=>Promise.all(names.filter(n=>n.startsWith('sv-public-')&&n!==CACHE).map(n=>caches.delete(n))))
      .then(()=>self.clients.claim())
  );
});
self.addEventListener('fetch',event=>{
  const req=event.request;
  if(req.method!=='GET')return;
  const url=new URL(req.url);
  if(url.origin!==BASE.origin || !paths.has(url.pathname))return;
  event.respondWith(
    fetch(req).then(res=>{
      if(res.ok && res.type==='basic'){
        const copy=res.clone();
        event.waitUntil(caches.open(CACHE).then(c=>c.put(req,copy)));
      }
      return res;
    }).catch(()=>caches.match(req).then(x=>x||Response.error()))
  );
});
