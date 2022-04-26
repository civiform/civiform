package services.export;

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
import services.Path;

/** PdfExporter is meant to generate PDF files. The functionality is not fully implemented yet. */
public class PdfExporter {
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
  public void export(Applicant applicant, Writer writer) throws IOException {
    PDAcroForm form = baseDocument.getDocumentCatalog().getAcroForm();
    for (Map.Entry<String, String> fToV : fieldToValue.entrySet()) {
      Optional<String> applicantValue =
          applicant.getApplicantData().readAsString(Path.create(fToV.getValue()));
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
