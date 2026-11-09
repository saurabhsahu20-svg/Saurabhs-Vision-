# Quality checklist — SAURABH’S VISION™

## Validated in this package
- Static HTML/CSS/JS and authored question data are present.
- JS syntax is checked using `node --check`.
- Basic file/route smoke assertions can run with Python without installing packages.

## Manual test before real use
1. **Mobile:** check widths 320, 375, 768, 1024 and 1440 px; nav, headings, contrast, page scrolling and forms.
2. **Navigation:** each link, language switcher and breadcrumb should navigate to a real page without losing context.
3. **Quiz:** choose class 6 mathematics, answer 5 questions, verify feedback; repeat with Class 2, 5, 8, 10 and science. Verify quiz explanation for every authored question.
4. **Admission form:** without Firebase it must not submit; with Firebase it saves a real `admissions` record and shows the returned document ID. Validate bad names, contact strings, consent and duplicate submits.
5. **Demo slots:** none invented; with a slot configured request it, then confirm through the deployed Cloud Function. Test two admins trying to confirm the final slot simultaneously: at most one should succeed.
6. **Authentication:** without Firebase there is no pretend login. With Firebase, admin creates a user and authorized role; password auth and sign-out work. Verify unauthenticated and wrong-role access fails.
7. **Privacy:** parent only reads specifically linked child; teacher only reads assigned questions; student only reads own attempts/doubts. Try Firestore direct queries against unauthorized collection records.
8. **Uploads:** only permitted student-owned JPG/PNG/WebP up to 5 MB are accepted; reject HTML/SVG/executables. Recheck Storage permissions; teacher viewing needs a separately authorized secure viewer if introduced.
9. **Results/gallery:** must stay empty until genuinely verified and approved items are published.
10. **Accessibility/security:** keyboard-only navigation, reduced motion, screen-reader labels, Firebase App Check enforcement, content validation, server rate limits and legal review.
11. **Data lifecycle:** backup/restore test, consent withdrawal, access revocation and deletion requests.
12. **SEO:** replace placeholder metadata with actual public canonical domain when owner approves public launch.

**Reminder:** Source code and visual test do not constitute production security certification. Firebase must be configured and backend behavior tested before accepting real student information.
