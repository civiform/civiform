import re

with open('server/app/services/export/PdfExporter.java', 'r') as f:
    content = f.read()

# Fix multi-line new Paragraph(..., smallGrayFontSelector)
for selector in ['h1FontSelector', 'h2FontSelector', 'h3FontSelector', 'paragraphFontSelector', 'smallGrayFontSelector', 'predicateFontSelector', 'linkFontSelector']:
    content = re.sub(
        r'new Paragraph\(\s*((?:[^,()]+|\([^)]*\))+?)\s*,\s*' + selector + r'\)',
        r'new Paragraph(' + selector + r'.process(\1))',
        content, flags=re.DOTALL
    )

with open('server/app/services/export/PdfExporter.java', 'w') as f:
    f.write(content)
