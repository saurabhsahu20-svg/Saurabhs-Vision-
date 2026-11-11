# SAURABH’S VISION™ — Website source package

**Public website:** https://saurabhsahu20-svg.github.io/Saurabhs-Vision-/ — GitHub Pages deploys the public frontend from the `main` branch. Public admissions and demo requests use guardian-initiated WhatsApp drafts when Firebase is not configured. The website does not submit or store those draft enquiries: visitors must review and press Send in WhatsApp. Authentication, remote admission records, private student dashboards, and confirmed demo bookings still require a properly configured Firebase backend, security rules and role provisioning. Do not advertise these private services as active until verified.

## Preview immediately (without credentials)

1. Extract this ZIP.
2. From this folder run `python3 -m http.server 8765` and visit `http://localhost:8765/` (or serve with any static HTTP server). The homepage and original foundation quizzes work without a Firebase project.
3. Offline/local preview has no online admissions, booking, secure account dashboards or cloud saving; these features explicitly explain the missing configuration.

## What this project includes

- Responsive home, about, course, learning-world, admission, demo, quiz, results, gallery, contact, policy, and portal pages, with hash-based client routing.
- Starter foundation quizzes: original questions, explanations, review, and a locally generated revision checklist. Results are **practice only**, not verified exams.
- Interactive Maths, gravity, 20-element chemistry explorer, plant-cell diagram and junior counting game.
- Firebase Authentication (email/password, administrator-created student/guardian accounts), Firestore for admission enquiries, demo requests, teacher-published questions, practice attempts, doubts, homework, study materials, approved achievements, and basic administrative tools.
- Role-based Firestore rules; server-side Firebase Cloud Function to confirm demo slots atomically, preventing overbooking when that function is used.
- Firebase Storage security rules for student doubt-image uploads.
- Bilingual navigation essentials; the **complete Hindi translation has not yet been completed**.

## Configure Google Firebase

1. Create your own Firebase project in the Firebase console. Add a **Web App** and copy its *web config* into `firebase-config.js`. This config is not a service-account key; **do not** put private service credentials in frontend files.
2. Enable Firebase Authentication > Email/Password (only admin-provisioned users should receive accounts). Set up Firestore and Cloud Storage.
3. Use Firebase CLI: `npm install -g firebase-tools`, `firebase login`, `firebase use --add` (select the project). Make sure project ownership and billing requirements are understood before enabling paid services.
4. Deploy **security rules first**: `firebase deploy --only firestore:rules,storage`.
5. Run `cd functions && npm install && cd ..` and review/deploy the transaction function with `firebase deploy --only functions`. Some Firebase services and Cloud Functions may require a billing-enabled plan. **Enable App Check** and set the reCAPTCHA Enterprise site key in `firebase-config.js` (`useAppCheck = true`) before using `confirmDemoBooking` with `enforceAppCheck:true`. Set the site’s authorized domains in Firebase Auth settings.
6. In Firebase Authentication create the institute's first adult administrator user. Find the `uid` in Firebase console. In Firestore console create `roles/{uid}` with `role` = `admin`. Creating this first document from the trusted Firebase console works even though client rules prohibit unauthorized writes. Provision other accounts similarly: `student`, `parent`, `teacher`.
7. For students, create `studentProfiles/{studentUid}` with `grade` (string `"1"`–`"10"`) and optionally `displayName`. For each guardian link create `parentLinks/{guardianUid}_{studentUid}` with fields `guardianId` and `studentId` after verification. For teachers create `teacherAssignments/{teacherUid}` with `subjectKeys`: `['Mathematics']`, `['Science']` or both.
8. Once Firebase is configured, use the private admin portal to add real demo slots, questions, content and institute contact information. Verify each permission carefully.
9. Before a **public launch**, set a production domain, update SEO metadata / sitemap, review legal policies, run security tests, turn on App Check and rate limiting, verify guardian consent, and prepare backup/retention practices.

### Important deployment and privacy notes

- **Publication approved by owner in this conversation.** The GitHub Pages frontend is publicly accessible; avoid placing private student records, account exports, Firebase credentials or signing keys in this repository. A Firebase Hosting URL is likewise not private without identity gating; private educational records always require strict backend role-based authorization.
- On the public GitHub Pages site with Firebase disabled, the admission and demo forms open a WhatsApp draft only after guardian consent and a visitor click. The browser does not persist or submit this information itself. WhatsApp processes the message only when the visitor chooses to send it. If Firebase is enabled later, configure and independently test a rate-limited, consent-aware backend (App Check, spam checks, per-IP quotas, retention controls) before accepting public traffic.
- Security rules **must be reviewed and tested against Firestore Rules Emulator** with adversarial cases before production use. Firebase rules do not provide a substitute for server-side score verification in formal examinations.
- Student practice score saving is client-generated and deliberately labeled *unverified practice*. Formal exams require a separate server-graded submission pipeline.
- The public gallery/results are blank until real, consent-approved content is uploaded. Never invent names, marks, ranks or testimonials.
- The bundled SVG emblem is an original temporary brand mark; replace it with the approved official SAURABH’S VISION logo before release.
- The Biology World diagram and Chemistry Lab shell visualization are simplified educational aids, not scientific-scale diagrams.
- This initial version does not contain full NCERT/CGBSE/ICSE chapter libraries, verified board curricula, PDF uploads or a complete attendance/fee management module. They should be phased in with authorized material and syllabus review.
- Review the web text and course descriptions with the institute owner before publishing them as service commitments.

## Google AI Studio — Build Mode

Google AI Studio Build supports React/Node full-stack applications and Firebase setup. To iterate on this project in Google's Build UI:

1. Create a **private GitHub repository** and upload the extracted source folder to it (do not include private credentials).
2. Open **Google AI Studio → Build**. In the prompt input, use **Add files (+) → Import from GitHub** and select the private repository (after authorizing GitHub access).
3. Paste the instructions from `GOOGLE_AI_STUDIO_BUILD_PROMPT.md` to upgrade the existing project in stages. Ask the agent to retain its real Firebase backend and role-based access—not replace it with mock data.
4. Use preview and test. Do not press Publish until explicitly ready and authorized. Google AI Studio can deploy to Cloud Run; costs/eligibility depend on your Google account and deployment tier.

The project is currently plain JS/CSS for fast preview. Google AI Studio can migrate it to React/TypeScript in a separate branch while keeping routes, database semantics, and visual branding; carefully review migration diffs.

## Test commands

- `node --check app.js`
- `node --check firebase-service.js`
- `node --check functions/index.js`
- `python3 tests/smoke.py` (file checks and route/component coverage)
- `firebase emulators:start --only firestore,auth,storage,functions,hosting` once Firebase dependencies are configured.

See `TEST_PLAN.md` for manual and security tests.

## Updated official brand artwork
The owner-supplied red/blue metallic logo is included in `assets/brand-3d.webp`, with resized app icons in `assets/vision-icon-64.png`, `assets/vision-icon-192.png`, and `assets/vision-icon-512.png`. These are referenced by the home page, navigation, favicon, and PWA manifest. Do not replace them with generic art.

The `SAURABHS_VISION_PREVIEW_UPDATED.html` standalone preview is provided separately as a convenience. Firebase services require configuration and deployment; the preview does not persist submitted data.

## Free public learning resources (November 2026)

- **Study Desk:** `/study-desk/` — browser-local study goals, progress checklist, print view and an optional 15-minute focus timer. No registration or backend. On shared devices, students should not enter personal details and can erase their goals.
- **Admissions Guide:** `/admissions-guide/` — verified course scopes, admissions steps, guardian-led WhatsApp enquiry, contact number, social profile and FAQs. Current batch availability and fees are intentionally not invented.
- **Daily Study Sprint:** `/study-sprint/` — original short practice with explanations.
- **Offline web app support:** A service worker caches a small allowlist of *public* learning pages and assets for later offline loading. It explicitly does **not** cache private account or backend traffic or the imported learning-bank library. Fresh live information always needs a network connection.
- The sitemap and homepage include these free public pages for navigation and search discovery. No required account, subscriptions or advertising have been activated.

### Launch validation

GitHub Actions runs JavaScript syntax checks and `website/tests/smoke.py` before publishing via GitHub Pages. A successful workflow shows that the public frontend was deployed; it does not certify every mobile/browser workflow or remote Firebase service.
