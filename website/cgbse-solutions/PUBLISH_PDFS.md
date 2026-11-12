# CGBSE 2026–27 — publish complete PDFs

The public CGBSE section is separate from all other SAURABH'S VISION features.

## One-time public publication
The site reader is committed and provides separate Class / Subject / Set 01–50 selectors with direct PDF viewing and download; it never asks visitors to upload a ZIP.

The complete original 1,050 question-and-answer PDFs must first be uploaded to this GitHub repository as **one release asset**. Do not upload private student information.

1. Download `CGBSE_2026_27_All_1050_PDF_Booklets.zip` (90,207,175 bytes; 1,050 PDFs) from ChatGPT.
2. Open GitHub Releases in `saurabhsahu20-svg/Saurabhs-Vision-` and choose **Draft a new release**.
3. Create tag `cgbse-2026-27` against `main`.
4. Attach the ZIP file, keeping the exact filename `CGBSE_2026_27_All_1050_PDF_Booklets.zip`.
5. Publish release. The updated Pages workflow extracts the 1,050 PDFs and makes them publicly accessible at paths like `/cgbse-solutions/pdfs/10_Mathematics/Set_01.pdf`.

SHA256 of complete archive:
`e8b1d14266e355ca3f1bdb8a5263580ae959072d372e1d6a5a51544076f2f836`.

The workflow validates that there are exactly 1,050 PDFs, that archive paths are safe, and that each file begins with a PDF header. Any failure stops publication rather than silently publishing samples.

**Accuracy caveat:** These are independent practice papers, not CGBSE official model papers. The non-mathematics descriptive answers contain teacher-review drafts. They should be verified before teaching.
