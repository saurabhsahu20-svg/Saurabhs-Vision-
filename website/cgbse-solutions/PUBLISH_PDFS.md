# CGBSE 2026–27 | 1,050 complete question + teacher-guide PDFs

**No ZIP uploads are required by students, visitors, or the repository owner.**

The repository now includes the complete compressed original question-and-answer
HTML source in `source/CGBSE_1050_Teacher_Library_Base64.txt` (622,732 Base64 characters).
It reconstructs 1,050 teacher guides (21 subjects × 50 sets).

On each push affecting `website/**`, the existing
`.github/workflows/deploy-pages.yml` workflow:

1. Verifies the checked-in archive using SHA-256.
2. Reconstructs all 1,050 source teacher-guide pages safely.
3. Uses `build_pdfs_from_html.py` and WeasyPrint to render each as an A4 PDF
   with the existing SAURABH'S VISION transparent watermark.
4. Verifies there are **exactly 1,050 valid PDF files** before deploying.
5. Publishes them at paths such as
   `/cgbse-solutions/pdfs/10_Mathematics/Set_01.pdf`.
6. Keeps the compressed sources and build tools out of GitHub Pages output.

Students select **Class → Subject → Set → PDF View / Download**. There is no
ZIP upload, release-creation, or custom account requirement.

If PDFs are still missing, check **Actions → Publish Saurabh Vision Website**
for the latest build result. A failed workflow does not mean the PDFs are live;
only successful published files count. A final build failure should be fixed,
not replaced with samples.

**Scope:** Original independent practice papers and their assembled answer
guides; not official CGBSE publications. For non-mathematics subjects, several
descriptive answers are *teacher-review drafts* and must be checked before
teaching. No student private data is included.
