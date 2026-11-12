# CGBSE 2026–27 — Publish all 1,050 complete PDFs

This SAURABH’S VISION™ feature is separate from the existing courses, quizzes and app. It adds **direct PDF reading and downloading** without asking site visitors to upload ZIP files.

## The easiest release asset: ~8 MB

The GitHub Pages workflow can generate **all 1,050 question + answer PDFs** from the compact, previously prepared archive:

- File to attach: `CGBSE_2026_27_Teacher_Papers_And_Answer_Guides.zip`
- Exact SHA-256: `a495301d5cdf761ef0294b01b5a072cd64e075d1213ead80db1ada83491337ec`
- Contains the full existing original teacher guide HTML question and answer content for 21 subjects, 50 sets each.

1. Download the ZIP supplied in ChatGPT to the repository owner.
2. In this GitHub repository, open **Releases → Draft a new release**.
3. Create tag **`cgbse-2026-27`** on branch `main`.
4. Attach the archive keeping its exact filename, then **Publish release**.
5. The workflow automatically renders 1,050 A4 PDFs with transparent SAURABH’S VISION™ branding and publishes them at paths like `/cgbse-solutions/pdfs/10_Mathematics/Set_01.pdf`.
6. Students can choose Class → Subject → Set 01–50 and open, print or download the PDF directly. No sample substitution and no ZIP-picker.

### Alternative: upload PDFs already generated locally

The workflow also accepts `CGBSE_2026_27_All_1050_PDF_Booklets.zip` (90,207,175 bytes; SHA-256 `e8b1d14266e355ca3f1bdb8a5263580ae959072d372e1d6a5a51544076f2f836`) as the same release asset instead of the smaller HTML archive. Finished PDF ZIP takes precedence when both exist.

**Accuracy caution:** These are independently assembled teaching practice papers, not CGBSE official model papers. The non-mathematics descriptive answers include teacher-review drafts and require review before teaching. PDFs must not contain student personal data.
