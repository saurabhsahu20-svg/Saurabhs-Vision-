# SAURABH'S VISION — Class 10 Model Paper Library publication

## What is live in the source code

The website at `/model-paper-library/` lists **66 real files** intended for:
- 6 uploaded CGBSE 2025–26 Class 10 source papers (`Original_2025-26.pdf`)
- 30 NEW independently written 75-mark guided practice papers (`Full75_Set_01.pdf` to `Full75_Set_05.pdf`, across 6 subjects), including worked explanations and diagrams where appropriate
- 30 retained independently written 30-mark mini papers (`Set_01.pdf` to `Set_05.pdf`, across 6 subjects)

The files are locally contained in the separate SAURABH'S VISION v4.1.5 Study Test APK and the owner ZIP archive. **The website source alone does not host these PDFs.** It must not claim download availability until the release asset has actually been published and GitHub Pages deployment succeeds.

Class 12 source files are now present for twelve subjects. The first six original PDFs and 30 practice sets are published; another six uploaded originals and 30 independent practice sets are being added through the verified build process. An unavailable PDF must keep Preview/Download disabled.

## One-time publishing steps for the owner (binary assets)

1. Sign in to the owner's GitHub repository `saurabhsahu20-svg/Saurabhs-Vision-`.
2. Open **Releases → Draft a new release**.
3. Create the tag `vision-class10-66-model-papers` from `main`.
4. Attach the **exact** file `SAURABHS_VISION_ALL_66_PAPERS_STEP_SOLUTIONS.zip` that was generated with the v4.1.5 APK. Do **not** substitute the older `Class10_All_66_Model_Papers_And_Guides.zip`.
5. Publish the release. The `.github/workflows/deploy-pages.yml` website pipeline runs on a published release; if necessary, run it manually.
6. Verify that the GitHub Pages deployment completed and test at least:
   - `/model-paper-library/pdfs/Class_10/10_Mathematics/Full75_Set_01.pdf`
   - `/model-paper-library/pdfs/Class_10/10_Science/Original_2025-26.pdf`
   - `/model-paper-library/pdfs/Class_10/10_English/Set_05.pdf`
   - Then test **Preview** and **Download** separately inside Grifly on Android.

The deploy script checks exactly 66 PDF filenames, validates the `%PDF-` signature, rejects unsafe paths and oversized files, and extracts them into the public site. If the owner hasn't published the ZIP, website deployment continues but every missing PDF remains visibly unavailable rather than displaying a misleading sample.

## Correct directory structure within the ZIP

`Class_10/10_<Subject>/Original_2025-26.pdf`

`Class_10/10_<Subject>/Full75_Set_01.pdf` … `Full75_Set_05.pdf`

`Class_10/10_<Subject>/Set_01.pdf` … `Set_05.pdf`

Subject folder names: `English`, `Hindi`, `Mathematics`, `Sanskrit`, `Science`, `Social_Science`.

The generated independent practice sets are not officially released CGBSE papers, and teacher verification of worked solutions is recommended. No private student data is included or requested.

## Grifly linking

Grifly beta **1.8.8** opens the online catalog in its WebView via the `Model Papers` quick tab and Study Tools tile. It retains the `Offline Papers` shortcut that looks for the installed v4.1.5 study APK (then older versions). Website PDFs and Android APKs are distinct distributions; a GitHub source merge does not automatically update already installed Android apps.

## Class 12 source and guided papers (published with site)

- 5 original CGBSE 2025–26 model PDF sources uploaded into `website/model-paper-library/pdfs/Class_12/12_<Subject>/Original_2025-26.pdf`.
- 25 independently prepared worked practice papers uploaded as the ZIP `website/model-paper-library/packs/Class12_25_Practice_Sets_Step_By_Step.zip`; GitHub Pages workflow validates 25 exact paths and PDF signatures, extracts to `pdfs/Class_12/12_<Subject>/Practice_Set_01.pdf` … `05.pdf`.
- Subjects / exact marks: Physics 70, Chemistry 70, Biology 70, Mathematics 80, English 80; all practice sets have separate question/solution sections, and science/math diagrams where relevant.
- Those 30 Class 12 PDFs are separate from the not-yet-published Class 10 ZIP mentioned above. Class 12 originals and question practices are not to be mislabeled as 75-mark papers.
- Independently written practice sets are **not** official CGBSE papers or official marking schemes; teacher review of solutions is encouraged.

## November 2026: six additional Class 12 subjects (70/80 marks)

Six more original 2025–26 CGBSE model PDFs were uploaded by the owner and their SHA-256 checksums recorded:
- Accountancy (301), 80 marks — `7c7e8b503922bbe6efe371275f64e9a668ef27a0613e794dba378cfddc16eb26`
- Business Studies (302), 80 marks — `2a891933de41e93497037f3caf0711ab41b91d365b58cee0fbfe96e50c80725a`
- Economics (303), 80 marks — `4c7c1e1194b90f43eda873e33e1f79eb249823a5e0280bb0f00a5efd4493fe82`
- History (101), 80 marks — `531963a47719fa1d7be26c65c6ebdb8138ca2932db35ce6b7b4b779e284f0afa`
- Geography (102), 70 marks — `2f3b8a8f931c9db75978d5d131f675c80d29554c0f1b2a2f800b492601745051`
- Political Science (103), 80 marks — `bc4a327c55b32fe410bb4e7407f3164b9cb35f80d13c2a489d5b608544cc1243`

The six original PDFs are preserved unchanged in the user-provided downloadable package. A separate GitHub workflow **attempts** to retrieve the same public CGBSE 2025–26 PDF copies from a public source, and publishes each only if its SHA-256 is *identical* to the user upload. It refuses silently substituting another revision. A complete 36-PDF pack is created **only if all six originals pass**. This avoids claiming a missing file is live.

The 30 new, independently authored practice PDFs are reproducibly generated using `generator/build_class12_additional6.py`, with five distinct sets per subject, correct marks, separate solved sections, and historical/geographic map illustrations. GitHub Pages publishes the generated 30 PDFs and `packs/Class12_Additional_30_Guided_Sets.zip` when its CI build succeeds.

Practice sets are *not* official CGBSE sample papers. Explanatory answers should be teacher-reviewed; different reasonable descriptive answers may receive credit.

The Grifly browser links to this same website. Updating static site PDF content does not need a new Android APK or a reinstallation.
