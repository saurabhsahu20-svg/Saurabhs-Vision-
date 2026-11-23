/**
 * Build Class 10 Vision Ka Saar print-ready PDF files directly from the
 * independently authored website practice sets. Runs after Chromium smoke tests.
 * Does not embed or reproduce any photographed source-book pages.
 */
import { chromium } from 'playwright';
import { stat } from 'node:fs/promises';
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
}finally{await browser.close();}
