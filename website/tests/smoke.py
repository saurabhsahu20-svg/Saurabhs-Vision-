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
