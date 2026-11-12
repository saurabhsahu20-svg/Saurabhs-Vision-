#!/usr/bin/env python3
"""Safely turn the checked-in compressed 1,050-set CGBSE archive into a ZIP."""
from __future__ import annotations

import base64
import hashlib
import io
import re
import sys
import tarfile
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

EXPECTED_SHA256 = "79243d885f80180da45ff587d191472f57363b462839fdae40aa9fc4e25d1b1b"
EXPECTED_SETS = 1050
ALLOWED = re.compile(
    r"^Teacher_Library/(?:10|12)_[A-Za-z_]+/Set_(?:0[1-9]|[1-4][0-9]|50)\.html$"
)


def main() -> None:
    source = Path(sys.argv[1])
    destination = Path(sys.argv[2])
    data = base64.b64decode(source.read_text("ascii").strip(), validate=True)
    digest = hashlib.sha256(data).hexdigest()
    if digest != EXPECTED_SHA256:
        raise SystemExit("Compressed question bank SHA-256 mismatch")
    destination.parent.mkdir(parents=True, exist_ok=True)
    count = 0
    with tarfile.open(fileobj=io.BytesIO(data), mode="r:xz") as archive:
        with ZipFile(destination, mode="w", compression=ZIP_DEFLATED, compresslevel=7) as out:
            for member in archive:
                if not member.isfile() or not ALLOWED.fullmatch(member.name):
                    raise SystemExit(f"Invalid question bank entry: {member.name}")
                if member.size > 100_000:
                    raise SystemExit(f"Oversized question bank entry: {member.name}")
                src = archive.extractfile(member)
                if src is None:
                    raise SystemExit(f"Cannot read question bank entry: {member.name}")
                content = src.read()
                if len(content) != member.size:
                    raise SystemExit(f"Truncated question bank entry: {member.name}")
                out.writestr(member.name, content)
                count += 1
    if count != EXPECTED_SETS:
        destination.unlink(missing_ok=True)
        raise SystemExit(f"Expected {EXPECTED_SETS} paper sets; got {count}")
    print(f"Validated and reconstructed {count} teacher question-and-answer guides")


if __name__ == "__main__":
    main()
