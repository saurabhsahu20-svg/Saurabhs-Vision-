/**
 * Real Chromium UI smoke tests for the PUBLIC, backend-free website.
 * Runs on GitHub Actions before deploying to GitHub Pages.
 * Uses local http.server; no student data or external services contacted.
 */
import { chromium } from 'playwright';
import assert from 'node:assert/strict';

const BASE = process.env.SV_TEST_URL || 'http://127.0.0.1:8765/';
const browser = await chromium.launch({channel:'chrome',headless:true,args:['--no-sandbox']});
let failures = 0;
async function check(name, run){
  const context = await browser.newContext({viewport:{width:1280,height:850},locale:'en-IN'});
  const page = await context.newPage();
  const errors=[];
  page.on('pageerror',error=>errors.push(error.message));
  try{
    await run(page,context);
    assert.deepEqual(errors,[], 'Uncaught browser exceptions: '+errors.join('; '));
    console.log('PASS:', name);
  }catch(error){failures++;console.error('FAIL:',name,'\n',error.stack||error);
    try{await page.screenshot({path:'/tmp/sv-browser-'+name.replace(/[^a-z0-9]/gi,'_')+'.png',fullPage:true})}catch{}
  }finally{await context.close()}
}
const goto=async(page,path='')=>{
  const response=await page.goto(BASE+path,{waitUntil:'domcontentloaded'});
  assert.equal(response?.status(),200,'HTTP 200 required for '+path);
};
await check('homepage-desktop',async page=>{
  await goto(page);
  await page.locator('main#main').waitFor();
  assert.match(await page.title(),/SAURABH/);
  assert.ok(await page.locator('a[href="./study-desk/index.html"]').count()>0,'study desk on homepage');
  assert.ok(await page.locator('a[href="./admissions-guide/index.html"]').count()>0,'admission guide on homepage');
  assert.ok(await page.locator('a[href="#/admissions"]').count()>0,'enquiry links present');
  await page.goto(BASE+'#/courses',{waitUntil:'domcontentloaded'});
  await page.getByText('Classes 1st – 5th').first().waitFor();
});
await check('mobile-navigation',async(page)=>{
  await page.setViewportSize({width:390,height:844});
  await goto(page);
  await page.locator('#mobile-toggle').click();
  assert.ok(await page.locator('.mobile-panel').isVisible(),'mobile menu visible');
  await page.locator('.mobile-panel a[href="#/courses"]').click();
  assert.ok(page.url().includes('#/courses'),'mobile navigation changes route');
  const dimensions=await page.evaluate(()=>({document:document.documentElement.scrollWidth,viewport:innerWidth}));
  assert.ok(dimensions.document<=dimensions.viewport+5,'no sideways mobile scrolling: '+JSON.stringify(dimensions));
});
await check('admission-class-and-whatsapp',async page=>{
  await page.addInitScript(()=>{window.__whatsappDrafts=[];window.open=(url)=>{window.__whatsappDrafts.push(url);return {closed:false}}});
  await goto(page,'#/admissions');
  await page.locator('#admissions-form').waitFor();
  await page.selectOption('#f-class','3');
  assert.equal(await page.locator('#f-course option[value="secondary"]').isDisabled(),true,'secondary course not for Class 3');
  assert.equal(await page.locator('#f-course option[value="board"]').isDisabled(),true,'board course not for Class 3');
  await page.selectOption('#f-course','primary');
  await page.locator('#f-student').fill('Student Test');
  await page.locator('#f-guardian').fill('Guardian Test');
  await page.locator('#f-contact').fill('9876543210');
  await page.locator('input[name="consent"]').check();
  await page.locator('#admissions-form button[type="submit"]').click();
  await page.locator('#form-status a[href^="https://wa.me/"]').waitFor();
  const drafts=await page.evaluate(()=>window.__whatsappDrafts);
  assert.equal(drafts.length,1,'WhatsApp draft generated');
  assert.ok(drafts[0].includes('918085808586'));
  assert.ok(decodeURIComponent(drafts[0]).includes('Class: 3'));
  assert.match(await page.locator('#form-status').textContent(),/not submitted or stored/i);
  assert.ok(await page.locator('#form-status a').isVisible(),'manual WhatsApp fallback shown');
  const keys=await page.evaluate(()=>Object.keys(localStorage));
  assert.ok(!keys.some(x=>/admission|guardian|phone/i.test(x)),'no student enquiry in browser storage');
});
await check('board-course-and-demo',async page=>{
  await page.addInitScript(()=>{window.__whatsappDrafts=[];window.open=url=>{window.__whatsappDrafts.push(url);return {closed:false}}});
  await goto(page,'#/admissions');
  await page.selectOption('#f-class','10');
  assert.equal(await page.locator('#f-course option[value="primary"]').isDisabled(),true);
  assert.equal(await page.locator('#f-course option[value="board"]').isDisabled(),false);
  await page.goto(BASE+'#/demo',{waitUntil:'domcontentloaded'});
  await page.locator('#demo-form').waitFor();
  assert.ok(await page.locator('input[name="preferredTime"]').count()>0,'non-Firebase demo time input');
  assert.equal(await page.locator('select[name="slotId"]').count(),0,'no unverified published slots');
  await page.selectOption('#f-class','8');
  await page.selectOption('#f-subject',{label:'Science'});
  await page.locator('input[name="preferredTime"]').fill('Saturday afternoon');
  await page.locator('#f-guardian').fill('Guardian Test');
  await page.locator('#f-contact').fill('9876543210');
  await page.locator('input[name="consent"]').check();
  await page.locator('#demo-form button[type="submit"]').click();
  assert.match(await page.locator('#form-status').textContent(),/WhatsApp/i);
  assert.equal(await page.evaluate(()=>window.__whatsappDrafts.length),1);
});
await check('foundation-quiz-and-progress',async page=>{
  await goto(page,'#/quiz');
  await page.locator('#quiz-board').waitFor();
  assert.equal(await page.locator('#quiz-board').inputValue(),'FOUNDATION');
  let finished=false;
  for(let n=0;n<35;n++){
    const next=page.locator('#quiz-next');
    if(await next.count()===0){finished=true;break;}
    if(await page.locator('.quiz-option').count()>0)await page.locator('.quiz-option').first().click();
    else if(await page.locator('#numeric-answer').count()>0)await page.locator('#numeric-answer').fill('42');
    await next.click();
  }
  assert.ok(finished||await page.getByText('PRACTICE REVIEW',{exact:true}).count()>0,'quiz results appear');
  await page.goto(BASE+'#/smart-path',{waitUntil:'domcontentloaded'});
  assert.ok(await page.getByText('Your latest practice review').count()>0,'review persisted locally');
});
await check('study-desk-storage-and-timer',async page=>{
  await goto(page,'study-desk/');
  await page.locator('#grade').selectOption('6');
  await page.locator('#subject').selectOption('Mathematics');
  await page.locator('#topic').fill('Revise fractions');
  await page.locator('#plan-form button[type="submit"]').click();
  assert.equal(await page.locator('.task').count(),1);
  assert.equal((await page.locator('#total').textContent()).trim(),'1');
  await page.reload({waitUntil:'domcontentloaded'});
  assert.equal(await page.locator('.task').count(),1,'goal survives refresh');
  await page.locator('.task input[type="checkbox"]').check();
  assert.equal((await page.locator('#done').textContent()).trim(),'1');
  await page.locator('#start').click();
  await page.waitForTimeout(1300);
  assert.notEqual(await page.locator('#timer').textContent(),'15:00','focus timer counts down');
  await page.locator('#pause').click();
  page.once('dialog',dialog=>dialog.accept());
  await page.locator('#clear-all').click();
  assert.equal(await page.locator('.task').count(),0,'clear data works');
});
await check('daily-sprint-completion',async page=>{
  await goto(page,'study-sprint/');
  await page.locator('#start').click();
  for(let i=0;i<5;i++){
    await page.locator('#options button').first().click();
    await page.locator('#next').click();
  }
  assert.ok(await page.locator('#finish').isVisible(),'five questions result screen shown');
  assert.match(await page.locator('#score').textContent(),/\/5/);
});
await check('admission-guide-and-links',async page=>{
  await goto(page,'admissions-guide/');
  assert.match(await page.title(),/Admissions/);
  assert.ok(await page.locator('a[href^="https://wa.me/"]').count()>0,'direct guardian WhatsApp option');
  assert.ok(await page.locator('a[href^="tel:"]').count()>0,'call option');
  await page.locator('details').first().locator('summary').click();
  assert.ok(await page.locator('details').first().getAttribute('open')!==null,'FAQ expandable');
});
await browser.close();
if(failures)process.exitCode=1;
else console.log('PASS: all 7 desktop/mobile Chromium browser flows');
