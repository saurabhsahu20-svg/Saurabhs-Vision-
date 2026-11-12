#!/usr/bin/env python3
"""Build complete question-and-answer PDFs from the approved teacher HTML archive."""
from pathlib import Path, PurePosixPath
from zipfile import ZipFile
from concurrent.futures import ProcessPoolExecutor, as_completed
import os, re, sys
from weasyprint import HTML

SOURCE = Path(sys.argv[1])
DEST = Path(sys.argv[2])
STYLE = """<style>
@page{size:A4;margin:15mm 14mm}
body{background:#fff!important;max-width:none!important;margin:0!important;padding:0!important;
font-family:"Noto Sans Devanagari","DejaVu Sans",sans-serif!important;font-size:10.5pt!important}
header{border-radius:0!important}article{break-inside:avoid;page-break-inside:avoid}
.toolbar{display:none!important}
.wm{position:fixed;top:43%;left:6%;width:88%;text-align:center;transform:rotate(-27deg);
z-index:500;font-size:36px;font-weight:900;letter-spacing:1px;color:rgba(14,54,121,.052);
pointer-events:none;opacity:.1}.wm img{display:block;width:96px;height:96px;object-fit:contain;margin:0 auto 8px}
footer{color:#526581}
</style>"""

def render(entry):
    name, raw = entry
    safe = PurePosixPath(name)
    subject, filename = safe.parts[1], safe.parts[2]
    target = DEST / subject / filename.replace(".html", ".pdf")
    target.parent.mkdir(parents=True, exist_ok=True)
    html = raw.decode("utf-8")
    html = html.replace("</head>", STYLE + "</head>", 1)
    logo = (Path(__file__).resolve().parents[1] / "assets" / "vision-icon-192.png").as_uri()
    html = html.replace("<body>", '<body><div class="wm"><img src="' + logo + '" alt="">SAURABH’S VISION™</div>', 1)
    HTML(string=html, base_url=str(DEST)).write_pdf(target)
    if target.stat().st_size < 5000:
        raise RuntimeError("Invalid PDF size: " + name)
    with target.open("rb") as f:
        if f.read(5) != b"%PDF-": raise RuntimeError("Invalid PDF header: " + name)
    return name

def main():
    matches=[]
    with ZipFile(SOURCE) as z:
        for m in z.infolist():
            if m.is_dir() or not m.filename.startswith("Teacher_Library/"): continue
            safe = PurePosixPath(m.filename)
            if (safe.is_absolute() or ".." in safe.parts or len(safe.parts)!=3
                or not safe.parts[1].startswith(("10_","12_"))
                or not re.fullmatch(r"Set_\d\d\.html", safe.name)
                or "\\" in m.filename or m.file_size>100_000):
                raise SystemExit("Unexpected archive member: "+m.filename)
            matches.append((m.filename,z.read(m)))
    if len(matches)!=1050: raise SystemExit(f"Expected 1050 model papers, found {len(matches)}")
    max_workers=min(3,os.cpu_count() or 2)
    with ProcessPoolExecutor(max_workers=max_workers) as pool:
        futures=[pool.submit(render,x) for x in matches]
        done=0
        for fut in as_completed(futures):
            fut.result()
            done+=1
            if done%50==0: print(f"Published PDF source {done}/1050",flush=True)
    print("All 1050 complete PDF files built successfully.",flush=True)

if __name__=="__main__": main()
