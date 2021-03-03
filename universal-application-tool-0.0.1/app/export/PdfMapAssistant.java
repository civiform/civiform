package export;

import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import models.Question;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.filespecification.PDSimpleFileSpecification;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionSubmitForm;
import org.apache.pdfbox.pdmodel.interactive.action.PDAnnotationAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.action.PDFormFieldAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.*;

public class PdfMapAssistant {
    public static PDDocument convertToMapPdf(PDDocument doc, ImmutableList<Question> questions) throws IOException {
        // Create a copy of the document by writing it to an in-memory object and re-loading it.
        ByteArrayOutputStream inMemoryDoc = new ByteArrayOutputStream();
        doc.save(inMemoryDoc);
        inMemoryDoc.close();
        PDDocument newDoc = PDDocument.load(inMemoryDoc.toByteArray());
        // discard in-memory object now that it's loaded.
        inMemoryDoc.reset();
        for (PDField field : newDoc.getDocumentCatalog().getAcroForm().getFields()) {
            COSDictionary fieldDict = field.getCOSObject();
            COSArray arr = new COSArray();
            for (Question opt : questions) {
                arr.add(new COSString(opt.getQuestionDefinition().getName()));
            }
            // Set the list of options (PDF /Opt).
            fieldDict.setItem(COSName.OPT, arr);
            // Set the field type to list box (PDF /Ch).
            fieldDict.setItem(COSName.FT, COSName.CH);
            // Set the field to required (field flag [/Ff] mask 0x2).
            fieldDict.setFlag(COSName.FF, 0x2, true);
        }
        return newDoc;
    }
}
