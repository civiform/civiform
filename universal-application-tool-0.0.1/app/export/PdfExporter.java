package export;

import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import models.Applicant;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;

public class PdfExporter implements Exporter {
  private PDDocument baseDocument;
  private ImmutableMap<String, String> fieldToValue;

  public PdfExporter(URI documentUrl, Map<String, String> fieldToValue) throws IOException {
    baseDocument = PDDocument.load(documentUrl.toURL().openStream());
    this.fieldToValue = ImmutableMap.copyOf(fieldToValue);
  }

  /**
   * Write a PDF containing the filled-in base form to the provided writer. For the PDF to be valid,
   * the writer should contain no previous writes and should be closed immediately after this call.
   * This function marshals the output document in memory due to restrictions of the PDDocument
   * class.
   */
  @Override
  public void export(Applicant applicant, Writer writer) throws IOException {
    PDAcroForm form = baseDocument.getDocumentCatalog().getAcroForm();
    for (Map.Entry<String, String> fToV : fieldToValue.entrySet()) {
      Optional<String> applicantValue = applicant.getApplicantData().readString(fToV.getValue());
      if (applicantValue.isPresent()) {
        form.getField(fToV.getKey()).setValue(applicantValue.get());
      }
    }
    ByteArrayOutputStream inMemoryFile = new ByteArrayOutputStream();
    baseDocument.save(inMemoryFile);
    inMemoryFile.close();
    writer.write(inMemoryFile.toString(StandardCharsets.UTF_8));
  }
}
