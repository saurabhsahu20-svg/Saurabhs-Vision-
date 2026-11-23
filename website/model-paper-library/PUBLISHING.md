# SAURABH'S VISION — Class 10 Model Paper Library publication

## What is live in the source code

The website at `/model-paper-library/` lists **66 real files** intended for:
- 6 uploaded CGBSE 2025–26 Class 10 source papers (`Original_2025-26.pdf`)
- 30 NEW independently written 75-mark guided practice papers (`Full75_Set_01.pdf` to `Full75_Set_05.pdf`, across 6 subjects), including worked explanations and diagrams where appropriate
- 30 retained independently written 30-mark mini papers (`Set_01.pdf` to `Set_05.pdf`, across 6 subjects)

The files are locally contained in the separate SAURABH'S VISION v4.1.5 Study Test APK and the owner ZIP archive. **The website source alone does not host these PDFs.** It must not claim download availability until the release asset has actually been published and GitHub Pages deployment succeeds.

Class 12 source files have not yet been supplied; the site shows the section as unavailable, never a fake file.

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
