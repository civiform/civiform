package export;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import models.Applicant;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;

public class PdfExporter implements Exporter {
  private PDDocument baseDocument;
  private ImmutableMap<String, String> fieldToValue;
  public PdfExporter(String documentUrl, Map<String, String> fieldToValue) throws IOException {
    baseDocument = PDDocument.load(new URL(documentUrl).openStream());
    this.fieldToValue = ImmutableMap.copyOf(fieldToValue);
  }

  public PdfExporter(File document, Map<String, String> fieldToValue) throws IOException {
    baseDocument = PDDocument.load(document);
    this.fieldToValue = ImmutableMap.copyOf(fieldToValue);
  }

  @Override
  public void export(Applicant applicant, Writer writer) throws IOException {
    PDAcroForm form = baseDocument.getDocumentCatalog().getAcroForm();
    for (Map.Entry<String, String> fToV : fieldToValue.entrySet()) {
      Optional<String> applicantValue = applicant.getApplicantData().readString(fToV.getValue());
      if (applicantValue.isPresent()) {
        form.getField(fToV.getKey()).setValue(applicantValue.get());
      }
    }
  }
}
