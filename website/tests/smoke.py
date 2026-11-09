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
