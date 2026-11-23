/**
 * Build Class 10 Vision Ka Saar print-ready PDF files directly from the
 * independently authored website practice sets. Runs after Chromium smoke tests.
 * Does not embed or reproduce any photographed source-book pages.
 */
import { chromium } from 'playwright';
import { stat, mkdir } from 'node:fs/promises';
import { join } from 'node:path';

const base=process.env.VKS_SITE_URL || 'http://127.0.0.1:8770';
const out=process.env.VKS_OUTPUT_DIR || '/tmp/vision-ka-saar-pdf';
const browser=await chromium.launch({channel:'chrome',headless:true,args:['--no-sandbox']});
try{
 for(const [subject,file] of [
  ['Mathematics','Vision_Ka_Saar_Class10_Maths_75_Marks_Solved.pdf'],
  ['Science','Vision_Ka_Saar_Class10_Science_75_Marks_Solved.pdf']
 ]){
   const page=await browser.newPage({viewport:{width:1080,height:1300},deviceScaleFactor:1});
   const response=await page.goto(base+'/vision-ka-saar/?subject='+subject,{waitUntil:'domcontentloaded'});
   if(!response?.ok())throw new Error(subject+' page unavailable: '+response?.status());
   if(await page.locator('.item').count()!==33)throw new Error(subject+' must contain 33 questions');
   const total=await page.locator('.marks').allTextContents();
   const marks=total.reduce((sum,part)=>sum+Number(part.match(/^([1-5])/)[1]),0);
   if(marks!==75)throw new Error(subject+' marks total wrong: '+marks);
   await page.evaluate(()=>{
     document.querySelectorAll('details').forEach(el=>el.open=true);
     document.body.dataset.print='solutions';
   });
   const filename=join(out,file);
   await page.pdf({path:filename,format:'A4',printBackground:true,preferCSSPageSize:true,
                   margin:{top:'14mm',right:'13mm',bottom:'14mm',left:'13mm'}});
   const size=(await stat(filename)).size;
   if(size<15000 || size>10000000)throw new Error(subject+' PDF output invalid size '+size);
   console.log('PASS: '+subject+' 75 marks, 33 original questions, all solutions, PDF '+size+' bytes');
   await page.close();
 }
 // Produce 12 concise, independent source-grounded study PDFs directly
 // from the site’s explained Set A/B pages, without republishing school scans.
 const guides=join(out,'school-solution-guides','pdfs');
 await mkdir(guides,{recursive:true});
 let generated=0;
 for(const grade of [9,10,11,12]){
   const subjects=grade<11?['SST']:['BST','ECO'];
   for(const subject of subjects){
     for(const set of ['A','B']){
       const page=await browser.newPage({viewport:{width:1080,height:1300},deviceScaleFactor:1});
       const url=base+'/vision-ka-saar/school-solution-guides/?class='+grade+'&subject='+subject+'&set='+set;
       const response=await page.goto(url,{waitUntil:'domcontentloaded'});
       if(!response?.ok())throw new Error('Guide unavailable '+url);
       const cards=await page.locator('#guides .card').count();
       if(cards!==1)throw new Error('Expected one '+grade+' '+subject+' '+set+' guide, got '+cards);
       const notes=await page.locator('#guides .card .key span').count();
       const steps=await page.locator('#guides .card details').count();
       if(notes<5||steps<3)throw new Error('Incomplete guide '+grade+' '+subject+' '+set);
       await page.evaluate(()=>document.querySelectorAll('details').forEach(d=>d.open=true));
       await page.emulateMedia({media:'print'});
       const name='Vision_Ka_Saar_Class_'+grade+'_'+subject+'_Set_'+set+'_Online_Guide.pdf';
       const dest=join(guides,name);
       await page.pdf({path:dest,format:'A4',printBackground:true,
         margin:{top:'13mm',bottom:'13mm',left:'12mm',right:'12mm'}});
       const bytes=(await stat(dest)).size;
       if(bytes<8000||bytes>3000000)throw new Error('Invalid compact PDF '+name+' bytes '+bytes);
       console.log('PASS: '+name+' '+notes+' objective keys, '+steps+' explained topics, '+bytes+' bytes');
       generated++;
       await page.close();
     }
   }
 }
 if(generated!==12)throw new Error('Expected 12 Class 9-12 guides, got '+generated);

}finally{await browser.close();}
