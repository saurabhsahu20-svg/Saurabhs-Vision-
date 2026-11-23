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
class12_subjects=('Physics','Chemistry','Biology','Mathematics','English','Hindi')
root12=root/'model-paper-library'
for subject in class12_subjects:
 source=root12/'pdfs'/'Class_12'/('12_'+subject)/'Original_2025-26.pdf'
 assert source.is_file(),f'missing Class 12 original: {subject}'
 assert source.open('rb').read(5)==b'%PDF-',f'invalid original PDF: {subject}'
pack=root12/'packs'/'Class12_30_Practice_Sets_Step_By_Step.zip'
assert pack.is_file(),'missing Class 12 practice pack'
expected={f'Class_12/12_{subject}/Practice_Set_{n:02d}.pdf'
          for subject in class12_subjects for n in range(1,6)}
with ZipFile(pack) as z:
 pdfs={p for p in z.namelist() if p.endswith('.pdf')}
 assert pdfs==expected and len(pdfs)==30,'Class 12 practice pack must have exactly 25 PDF files'
 assert z.testzip() is None,'Class 12 pack ZIP CRC failed'
 for name in expected:
  with z.open(name) as item:
   assert item.read(5)==b'%PDF-',f'Invalid PDF header: {name}'
 manifest=json.loads(z.read('PAPER_MANIFEST.json'))
 assert len(manifest['papers'])==25 || len(manifest['papers'])==30
 for paper in manifest['papers']:
  expected_marks=80 if paper['subject'] in ('Mathematics','English','Hindi') else 70
  assert paper['total_marks']==expected_marks,f'wrong exam marks for {paper}'
for token in ('SUBJECTS12','Class_12/12_','Practice_Set_','marks:70','marks:80'):
 assert token in model,f'missing online Class 12 selector: {token}'
assert 'Class 10 & 12 Model Papers' in (root/'app.js').read_text(encoding='utf-8')
print('PASS: 6 original Class 12 PDFs, 30 worked practice sets, subject-correct marks, PDF integrity and site links')
