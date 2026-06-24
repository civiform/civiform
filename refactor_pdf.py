import re

with open('server/app/services/export/PdfExporter.java', 'r') as f:
    content = f.read()

# Remove static font constants
content = re.sub(
    r'\s+// A set of fonts.*?private static final Font LINK_FONT =\n\s+FontFactory\.getFont\(FontFactory\.HELVETICA, 11, Font\.UNDERLINE, new BaseColor\(0, 94, 162\)\);\n',
    '\n', content, flags=re.DOTALL
)

# Add imports
imports = """import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.FontSelector;
import com.itextpdf.text.Phrase;"""
content = re.sub(r'import com.itextpdf.text.Paragraph;', imports + '\nimport com.itextpdf.text.Paragraph;', content)

# Inject environment
content = content.replace('StatusService statusService, LanguageUtils languageUtils) {', 'StatusService statusService, LanguageUtils languageUtils, play.Environment environment) {')

init_fonts = """
    ImmutableList.Builder<BaseFont> fontBuilder = ImmutableList.builder();
    for (String fontPath : FONT_PATHS) {
        try {
            java.io.File fontFile = environment.getFile(fontPath);
            if (fontFile.exists()) {
                fontBuilder.add(BaseFont.createFont(fontFile.getAbsolutePath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED));
            } else {
                System.err.println("Font file not found: " + fontPath);
            }
        } catch (Exception e) {
            System.err.println("Failed to load font " + fontPath + ": " + e.getMessage());
        }
    }
    ImmutableList<BaseFont> baseFonts = fontBuilder.build();

    this.h1FontSelector = createFontSelector(baseFonts, 30, Font.BOLD, BaseColor.BLACK);
    this.h2FontSelector = createFontSelector(baseFonts, 16, Font.BOLD, BaseColor.BLACK);
    this.h3FontSelector = createFontSelector(baseFonts, 16, Font.NORMAL, BaseColor.BLACK);
    this.paragraphFontSelector = createFontSelector(baseFonts, 12, Font.NORMAL, BaseColor.BLACK);
    this.smallGrayFontSelector = createFontSelector(baseFonts, 10, Font.NORMAL, BaseColor.GRAY);
    this.predicateFontSelector = createFontSelector(baseFonts, 11, Font.NORMAL, BaseColor.BLUE);
    this.linkFontSelector = createFontSelector(baseFonts, 11, Font.UNDERLINE, new BaseColor(0, 94, 162));
"""
content = content.replace('this.languageUtils = checkNotNull(languageUtils);', 'this.languageUtils = checkNotNull(languageUtils);\n' + init_fonts)

fields = """
  private static final ImmutableList<String> FONT_PATHS = ImmutableList.of(
      "conf/fonts/NotoSans-Regular.ttf",
      "conf/fonts/NotoSansArabic-Regular.ttf",
      "conf/fonts/NotoSansEthiopic-Regular.ttf",
      "conf/fonts/NotoSansLao-Regular.ttf",
      "conf/fonts/NotoSansTC-Regular.ttf",
      "conf/fonts/NotoSansJP-Regular.ttf",
      "conf/fonts/NotoSansKR-Regular.ttf"
  );
  private final FontSelector h1FontSelector;
  private final FontSelector h2FontSelector;
  private final FontSelector h3FontSelector;
  private final FontSelector paragraphFontSelector;
  private final FontSelector smallGrayFontSelector;
  private final FontSelector predicateFontSelector;
  private final FontSelector linkFontSelector;

  private FontSelector createFontSelector(ImmutableList<BaseFont> baseFonts, int size, int style, BaseColor color) {
      FontSelector selector = new FontSelector();
      if (baseFonts.isEmpty()) {
          selector.addFont(FontFactory.getFont(FontFactory.HELVETICA, size, style, color));
      } else {
          for (BaseFont baseFont : baseFonts) {
              selector.addFont(new Font(baseFont, size, style, color));
          }
          // Always add Helvetica as a final fallback
          selector.addFont(FontFactory.getFont(FontFactory.HELVETICA, size, style, color));
      }
      return selector;
  }
"""
content = content.replace('private static final int INDENTATION_PER_LEVEL = 25;', 'private static final int INDENTATION_PER_LEVEL = 25;\n' + fields)


# Replace text() method
old_text = """  private static Paragraph text(String text, Font font, int indentationLevel) {
    Paragraph paragraph = new Paragraph(text, font);
    paragraph.setIndentationLeft(indentationLevel * INDENTATION_PER_LEVEL);
    return paragraph;
  }"""
new_text = """  private Paragraph text(String text, FontSelector fontSelector, int indentationLevel) {
    Paragraph paragraph = new Paragraph(fontSelector.process(text));
    paragraph.setIndentationLeft(indentationLevel * INDENTATION_PER_LEVEL);
    return paragraph;
  }"""
content = content.replace(old_text, new_text)

# Replace all font usages
content = content.replace('H1_FONT', 'h1FontSelector')
content = content.replace('H2_FONT', 'h2FontSelector')
content = content.replace('H3_FONT', 'h3FontSelector')
content = content.replace('PARAGRAPH_FONT', 'paragraphFontSelector')
content = content.replace('SMALL_GRAY_FONT', 'smallGrayFontSelector')
content = content.replace('PREDICATE_FONT', 'predicateFontSelector')
# Link font is used differently: Anchor anchor = new Anchor(fileName, LINK_FONT);
content = content.replace('Anchor anchor = new Anchor(fileName, LINK_FONT);', 'Anchor anchor = new Anchor(); anchor.add(linkFontSelector.process(fileName));')

content = content.replace('new Paragraph(programDefinition.localizedName().getOrDefault(prefferedLocale.toLocale()), h1FontSelector)', 'new Paragraph(h1FontSelector.process(programDefinition.localizedName().getOrDefault(prefferedLocale.toLocale())))')

content = re.sub(r'new Paragraph\(\s*(.*?),\s*FontFactory\.getFont\(FontFactory\.HELVETICA_BOLD, 16\)\)', r'new Paragraph(h2FontSelector.process(\1))', content)
content = re.sub(r'new Paragraph\(\s*(.*?),\s*FontFactory\.getFont\(FontFactory\.HELVETICA_BOLD, 15\)\)', r'new Paragraph(h2FontSelector.process(\1))', content)
content = re.sub(r'new Paragraph\(\s*(.*?),\s*FontFactory\.getFont\(FontFactory\.HELVETICA_BOLD, 12\)\)', r'new Paragraph(paragraphFontSelector.process(\1))', content)
content = re.sub(r'new Paragraph\(\s*(.*?),\s*FontFactory\.getFont\(FontFactory\.HELVETICA, 11\)\)', r'new Paragraph(paragraphFontSelector.process(\1))', content)
content = re.sub(r'new Paragraph\(\s*(.*?),\s*FontFactory\.getFont\(FontFactory\.HELVETICA, 10\)\)', r'new Paragraph(smallGrayFontSelector.process(\1))', content)

# Anchor with single param string
content = content.replace('Anchor anchor = new Anchor(answerData.answerText());', 'Anchor anchor = new Anchor(); anchor.add(linkFontSelector.process(answerData.answerText()));')

content = re.sub(r'new Paragraph\((.*?),\s*smallGrayFontSelector\)', r'new Paragraph(smallGrayFontSelector.process(\1))', content)
content = re.sub(r'new Paragraph\((.*?),\s*paragraphFontSelector\)', r'new Paragraph(paragraphFontSelector.process(\1))', content)

content = content.replace('list.add(new ListItem(option.optionText().getOrDefault(prefferedLocale.toLocale())));', 'list.add(new ListItem(paragraphFontSelector.process(option.optionText().getOrDefault(prefferedLocale.toLocale()))));')
content = content.replace('list.add(new ListItem(condition, predicateFontSelector));', 'list.add(new ListItem(predicateFontSelector.process(condition)));')

with open('server/app/services/export/PdfExporter.java', 'w') as f:
    f.write(content)
