import olefile
import re
import sys

path = r"c:\Users\asus\Desktop\附件1：可行性研究报告模板.doc"
ole = olefile.OleFileIO(path)
data = ole.openstream("WordDocument").read()
ole.close()

# UTF-16LE Chinese text fragments in Word binary
text = data.decode("utf-16le", errors="ignore")
lines = []
for line in text.split("\r"):
    line = line.strip()
    if len(line) >= 2 and re.search(r"[\u4e00-\u9fff]", line):
        lines.append(line)

seen = set()
unique = []
for line in lines:
    if line not in seen:
        seen.add(line)
        unique.append(line)

out = r"e:\java\hr-management\docs\template-extract.txt"
with open(out, "w", encoding="utf-8") as f:
    f.write("\n".join(unique))
print(f"extracted {len(unique)} lines -> {out}")
