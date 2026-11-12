# CGBSE 2026–27 — Site publishing: 1,050 question-and-answer PDFs

**Status / Important:** The GitHub Pages page does not store the actual PDF files in git; it only links to static PDFs produced by the deployment workflow. If the public PDF link says not published, the release asset has not been attached/deployed successfully. Do not substitute samples or claim that all PDFs are hosted before verifying a real URL.

## Current improved complete archive (12 November 2026)

- **Release asset filename (must match):** `CGBSE_2026_27_All_1050_PDF_Booklets.zip`
- **New size:** 124,190,382 bytes.
- **SHA-256:** `ebfe805f7f75c844b6e4b8d4cd2f7e0c27627af8b6fb0b09458d4e620c9887d9`
- **Contains exactly 1,050 PDFs**, each directly navigable by the existing route `/cgbse-solutions/pdfs/<class>_<subject>/Set_XX.pdf`.
- **300 improved PDFs:** 10 Science, 10 Mathematics, 12 Mathematics, 12 Physics, 12 Chemistry, 12 Biology (50 sets per subject). They have question text, answers, topic-aligned explanations, new labelled diagrams where relevant, and a light transparent SAURABH'S VISION watermark.
- **750 retained PDFs:** Other 15 subjects use previously generated answer/draft booklets, **not** newly expert-verified fully worked solutions.
- The materials are independently assembled classroom practice, not official CGBSE question papers or certified answer keys. Teachers should check conceptual accuracy, generic wording, ambiguous questions and 2026–27 official blueprint before use.

## Owner publication steps

1. Obtain the **updated** archive above (the SHA-256 differentiates it from the previous 90 MB version).
2. In this public GitHub repository, open **Releases → Draft a new release**.
3. Use tag `cgbse-2026-27` on branch `main`.
4. Attach `CGBSE_2026_27_All_1050_PDF_Booklets.zip` and publish the release. If a release under this tag already exists, update/replace the same-named asset instead of creating a duplicate release.
5. The existing `deploy-pages.yml` workflow downloads that asset, checks the count and PDF signatures, extracts into the public site artifact, and makes PDF links available. **A deployment must run successfully** after release asset changes; use the workflow's manual `workflow_dispatch` if needed.
6. Verify at least the real sample routes after the deploy, for instance `/cgbse-solutions/pdfs/10_Science/Set_01.pdf` and `/cgbse-solutions/pdfs/12_Chemistry/Set_01.pdf`. Confirm PDF opens on mobile, shows the diagram and downloads.

No visitors need to upload ZIP. Do not store private student records or credentials in the release. **No Android APK update is implied** by this static website publication.

## Alternate earlier source

The earlier `CGBSE_2026_27_Teacher_Papers_And_Answer_Guides.zip` includes old teacher-guide content (much of it draft). Do **not** use that as a replacement for the updated 300 enhanced papers: the ready-to-publish 1,050-PDF zip must take precedence.
