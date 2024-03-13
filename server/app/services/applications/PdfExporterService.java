package services.applications;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.itextpdf.text.DocumentException;
import java.io.IOException;
import models.ApplicationModel;
import services.TranslationNotFoundException;
import services.export.PdfExporter;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;

/** The service responsible for exporting a PDF file */
public final class PdfExporterService {
  private final PdfExporter pdfExporter;

  @Inject
  PdfExporterService(PdfExporter pdfExporter) {
    this.pdfExporter = checkNotNull(pdfExporter);
  }

  /**
   * Creates and returns a PDF containing the answers submitted in the given {@code application}.
   *
   * <p>Used for applicants to download a copy of their submitted application and for program admins
   * to review applications.
   */
  public PdfExporter.InMemoryPdf generateApplicationPdf(
      ApplicationModel application, boolean showEligibilityText, boolean includeHiddenBlocks) {
    PdfExporter.InMemoryPdf pdf;
    try {
      pdf = pdfExporter.exportApplication(application, showEligibilityText, includeHiddenBlocks);
    } catch (DocumentException | IOException e) {
      throw new RuntimeException(e);
    }
    return pdf;
  }

  /**
   * Creates a returns a PDF containing all the blocks and questions in the given {@code
   * programDefinition}.
   *
   * <p>Used for admins to see their current program setup.
   */
  public PdfExporter.InMemoryPdf generateProgramPreviewPdf(
      ProgramDefinition programDefinition, ImmutableList<QuestionDefinition> allQuestions) {
    PdfExporter.InMemoryPdf pdf;
    try {
      pdf = pdfExporter.exportProgram(programDefinition, allQuestions);
    } catch (DocumentException | IOException | TranslationNotFoundException e) {
      throw new RuntimeException(e);
    }
    return pdf;
  }
}
