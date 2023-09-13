package services.applications;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import com.itextpdf.text.DocumentException;
import java.io.IOException;
import models.Application;
import services.export.PdfExporter;

/** The service responsible for exporting a PDF file */
public final class PdfExporterService {
  private final PdfExporter pdfExporter;

  @Inject
  PdfExporterService(PdfExporter pdfExporter) {
    this.pdfExporter = checkNotNull(pdfExporter);
  }

  public PdfExporter.InMemoryPdf generatePdf(Application application) {
    PdfExporter.InMemoryPdf pdf;
    try {
      pdf = pdfExporter.export(application);
    } catch (DocumentException | IOException e) {
      throw new RuntimeException(e);
    }
    return pdf;
  }
}
