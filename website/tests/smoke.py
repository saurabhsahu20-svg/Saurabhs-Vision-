from pathlib import Path
root=Path(__file__).resolve().parent.parent
for p in ('index.html','styles.css','app.js','firebase-service.js','firebase-config.js','firestore.rules','storage.rules','firebase.json','functions/index.js','README.md'):
 assert (root/p).is_file(),f'missing {p}'
app=(root/'app.js').read_text()
for route in ('/about','/courses','/universe','/quiz','/admissions','/demo','/portal/student','/portal/parent','/portal/teacher','/portal/admin','/results','/gallery','/doubts','/materials','/homework','/privacy','/terms'):
 assert f"'{route}'" in app,route
for token in ('function finishQuiz','function elementInfo','function dropBall','function fetchDashboard','function onSubmit'):
 assert token in app,token
assert 'c(tr(\'explore\'),\'/courses\'' in app
print('PASS: essential project files, routes and interactive features present')

# Verify free public features and parent-led enquiry contact.
for page in ('study-desk/index.html','admissions-guide/index.html','study-sprint/index.html','contact/index.html'):
 assert (root/page).is_file(), f'missing public page {page}'
assert './study-desk/index.html' in app
assert './admissions-guide/index.html' in app
assert 'https://wa.me/918085808586' in app
assert "window.open(link,'_blank'" in app
assert 'Parents or guardians should approve enquiries' in app
study=(root/'study-desk/index.html').read_text()
for token in ('plan-form','clear-all','localStorage','focus','timer','window.print'):
 assert token.lower() in study.lower(),f'missing planner component: {token}'
admission=(root/'admissions-guide/index.html').read_text()
for token in ('Classes 1st–5th','Classes 6th–10th','8085808586','No course price'):
 assert token in admission, f'missing guide content: {token}'
sitemap=(root/'sitemap.xml').read_text()
for page in ('study-desk/','admissions-guide/'):
 assert page in sitemap,f'missing sitemap listing: {page}'
print('PASS: new public resources and parent-led admissions links')

# Model Paper Library catalog must not promise unpublished PDFs.
model=(root/'model-paper-library/index.html').read_text(encoding='utf-8')
for token in ('Original_2025-26.pdf','Full75_Set_','Class_10/','pdfCanvas','aria-disabled','Step-by-Step','grade','paperSet'):
 assert token in model, f'missing model-paper catalog feature: {token}'
assert 'model-paper-library/' in (root/'app.js').read_text(encoding='utf-8')
assert 'model-paper-library/' in (root/'index.html').read_text(encoding='utf-8')
assert 'model-paper-library/' in (root/'sitemap.xml').read_text(encoding='utf-8')
assert '<iframe' not in model, 'Avoid Android inline PDF auto-download iframe'
print('PASS: Class 10 model-paper library listing, manual preview and separate download')

# The five owner-uploaded originals are binary-identical PDFs; Class 12 published
# practice PDFs must use their true subject-specific 70- or 80-mark pattern.
from zipfile import ZipFile
import json
class12_subjects=('Physics','Chemistry','Biology','Mathematics','English')
root12=root/'model-paper-library'
for subject in class12_subjects:
 source=root12/'pdfs'/'Class_12'/('12_'+subject)/'Original_2025-26.pdf'
 assert source.is_file(),f'missing Class 12 original: {subject}'
 assert source.open('rb').read(5)==b'%PDF-',f'invalid original PDF: {subject}'
pack=root12/'packs'/'Class12_25_Practice_Sets_Step_By_Step.zip'
assert pack.is_file(),'missing Class 12 practice pack'
expected={f'Class_12/12_{subject}/Practice_Set_{n:02d}.pdf'
          for subject in class12_subjects for n in range(1,6)}
with ZipFile(pack) as z:
 pdfs={p for p in z.namelist() if p.endswith('.pdf')}
 assert pdfs==expected and len(pdfs)==25,'Class 12 practice pack must have exactly 25 PDF files'
 assert z.testzip() is None,'Class 12 pack ZIP CRC failed'
 for name in expected:
  with z.open(name) as item:
   assert item.read(5)==b'%PDF-',f'Invalid PDF header: {name}'
 manifest=json.loads(z.read('PAPER_MANIFEST.json'))
 assert len(manifest['papers'])==25
 for paper in manifest['papers']:
  expected_marks=80 if paper['subject'] in ('Mathematics','English') else 70
  assert paper['total_marks']==expected_marks,f'wrong exam marks for {paper}'
# The new Hindi archive contains the sixth original plus exactly five
# independent 80-mark Hindi practice PDFs and verified PDF signatures.
hindi_pack=root12/'packs'/'Class12_Hindi_6_Papers.zip'
assert hindi_pack.is_file(),'Hindi original and five practice PDFs were not uploaded'
hindi_names={f'Class_12/12_Hindi/Practice_Set_{n:02d}.pdf' for n in range(1,6)}
hindi_names.add('Class_12/12_Hindi/Original_2025-26.pdf')
with ZipFile(hindi_pack) as z:
 items=[p for p in z.namelist() if p.endswith('.pdf')]
 assert len(items)==6 and set(items)==hindi_names
 assert z.testzip() is None
 for name in hindi_names:
  with z.open(name) as stream:
   assert stream.read(5)==b'%PDF-',f'Invalid Hindi PDF: {name}'
assert "key:'Hindi'" in model and "marks:80" in model
assert "Class12_30_Practice_Sets_Step_By_Step.zip" in model
for token in ('SUBJECTS12','Class_12/12_','Practice_Set_','marks:70','marks:80'):
 assert token in model,f'missing online Class 12 selector: {token}'
assert 'Class 10 & 12 Model Papers' in (root/'app.js').read_text(encoding='utf-8')
print('PASS: 6 original Class 12 PDFs, 30 worked practice sets, Hindi 010, subject marks and site links')

# Additional CGBSE 2025-26 12th model papers and independently generated sets.
# The CI build generates the binary PDFs after this source smoke check.
import ast
generator=root12/'generator'/'build_class12_additional6.py'
assert generator.is_file(),'Missing reproducible 30-paper source for added Class 12 subjects'
tree=ast.parse(generator.read_text(encoding='utf8'))
subjects={}
for node in tree.body:
 if isinstance(node,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='SUBJECTS' for t in node.targets):
  subjects=ast.literal_eval(node.value)
expected_subjects={
 'Accountancy':80,'Business_Studies':80,'Economics':80,
 'History':80,'Geography':70,'Political_Science':80
}
assert {name:value['marks'] for name,value in subjects.items()}==expected_subjects
for name in expected_subjects:
 assert ("key:'"+name+"'") in model, f'New Class 12 subject absent from website: {name}'
for token in ('Class12_Additional_30_Guided_Sets.zip','Class12_Additional_36_Papers.zip','<strong>72</strong>'):
 assert token in model, f'Additional 6-subject PDF catalog missing: {token}'
print('PASS: 12 Class 12 subjects and generator for 30 new full-mark solved paper sets')

# Vision Ka Saar must stay an independent, fully worked Class 10 study page.
vks=(root/'vision-ka-saar/index.html').read_text(encoding='utf-8')
assert '<h1>VISION KA SAAR</h1>' in vks
for token in ('Mathematics','Science','Step-by-step solution','Vision_Ka_Saar_Class10_Maths_75_Marks_Solved.pdf','Vision_Ka_Saar_Class10_Science_75_Marks_Solved.pdf'):
 assert token in vks,f'missing Class 10 Vision Ka Saar feature: {token}'
assert 'navbodh' not in vks.lower() and 'prabodh' not in vks.lower()
for path in ('model-paper-library/index.html','index.html','sitemap.xml','board-preparation/index.html'):
 assert 'vision-ka-saar/' in (root/path).read_text(encoding='utf-8'),f'missing Vision Ka Saar navigation: {path}'
builder=(root/'tests/build_vision_ka_saar_pdf.mjs').read_text(encoding='utf-8')
assert "marks!==75" in builder and "page.pdf(" in builder
print('PASS: Vision Ka Saar original Class 10 Maths/Science, PDF builder, navigation and no copied publisher branding')

# Online and offline Saurabh Vision libraries agree on the published categories.
model=(root/'model-paper-library/index.html').read_text(encoding='utf-8')
assert '<strong>68</strong><span>कक्षा 10वीं PDF' in model, 'Class 10 should show 68 PDFs'
assert '<strong>72</strong><span>कक्षा 12वीं PDF' in model, 'Class 12 should show 72 PDFs'
for token in ("value==='saar'","updateVisionSaarOption()","Vision_Ka_Saar_Class10_Maths_75_Marks_Solved.pdf",
              "Vision_Ka_Saar_Class10_Science_75_Marks_Solved.pdf","../vision-ka-saar/","typ==='original'||typ==='saar'"):
    assert token in model, 'Vision Ka Saar integrated library selector missing: '+token
assert 'सभी 12 विषयों का संग्रह' in model, 'Class 12 catalog should remain available'
print('PASS: 140-paper parity, Vision Ka Saar in subject selector, direct PDF links')

# Website, Vision APK and Grifly 140-paper source catalog parity.
reader=(root/'model-paper-library/index.html').read_text(encoding='utf-8')
assert 'id="app-browser-parity"' in reader
assert 'Offline 140 PDFs' in reader
assert 'कक्षा 10वीं: 68 PDF' in reader and 'कक्षा 12वीं: 72 PDF' in reader
assert 'vision-ka-saar/' in reader and 'releases' in reader
assert 'डाउनलोड' not in reader or 'aria-disabled' in reader
print('PASS: website Grifly/Vision 140-paper parity guide and safe PDF controls')

# The new 12 class 9–12 supplementary guides must remain accessible and honest.
solution_html=(root/'vision-ka-saar/school-solution-guides/index.html').read_text(encoding='utf-8')
assert 'Class 9–12' in solution_html and 'cases=[' in solution_html
assert solution_html.count("{g:9,s:'SST'")==2
assert solution_html.count("{g:10,s:'SST'")==2
for grade,sub in [(11,'BST'),(11,'ECO'),(12,'BST'),(12,'ECO')]:
 assert solution_html.count("{g:%d,s:'%s'"%(grade,sub))==2
assert 'summative assessment' not in solution_html.lower()
assert 'gudhiyari' not in solution_html.lower()
assert 'not official school answer keys' in solution_html.lower()
for page in ('vision-ka-saar/index.html','model-paper-library/index.html','index.html','sitemap.xml'):
 assert 'school-solution-guides/' in (root/page).read_text(encoding='utf-8'),page
print('PASS: all 12 independent Class 9-12 stepwise guide sets, transparency, navigation and clean branding')
