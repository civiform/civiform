package services.export;

import com.google.common.collect.ImmutableList;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import models.Question;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;

/** Provides helper functions for PDF export. PDF export is not fully implemented yet. */
public class PdfMapAssistant {
  // the bit to set for requiredness in a PDF form.
  private static int REQUIRED = 0x2;

  /**
   * Creates and returns a new document, which is a copy of the provided document, except that every
   * form field is converted to a list-selector, in which you can select one of the list of provided
   * questions. This maintains the names of the fields, so the resulting PDF, once filled in, is
   * suitable for reading as a configuration method for PDF services.export. The method for
   * submitting this configuration PDF is to-be-determined since, as noted in
   * https://bugs.chromium.org/p/chromium/issues/detail?id=719344, Chrome does not support PDF
   * submit-buttons.
   */
  public static PDDocument convertToMapPdf(PDDocument doc, ImmutableList<Question> questions)
      throws IOException {
    // Create a copy of the document by writing it to an in-memory object and re-loading it.
    ByteArrayOutputStream inMemoryDoc = new ByteArrayOutputStream();
    doc.save(inMemoryDoc);
    inMemoryDoc.close();
    PDDocument newDoc = PDDocument.load(inMemoryDoc.toByteArray());
    // discard in-memory object now that it's loaded.
    inMemoryDoc.reset();

    // Create the list of strings in the internal PDF format.
    COSArray optionsArray = new COSArray();
    for (Question question : questions) {
      optionsArray.add(new COSString(question.getQuestionDefinition().getName()));
    }

    for (PDField field : newDoc.getDocumentCatalog().getAcroForm().getFields()) {
      COSDictionary fieldDict = field.getCOSObject();
      // Set the list of options (PDF /Opt).
      fieldDict.setItem(COSName.OPT, optionsArray);
      // Set the field type to list box (PDF /Ch).
      fieldDict.setItem(COSName.FT, COSName.CH);
      // Set the field to required (field flag [/Ff] mask 0x2).
      fieldDict.setFlag(COSName.FF, REQUIRED, true);
    }

    // Note that you cannot have a submit button at the end of the PDF.
    // The PDF spec supports it, but Chrome, which is the most popular browser,
    // does not.  It would work in Acrobat, but we can't realistically tell people
    // what PDF reader to use.
    return newDoc;
  }
}
