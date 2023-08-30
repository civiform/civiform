package services.applications;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import com.itextpdf.text.DocumentException;
import com.typesafe.config.Config;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;
import models.Application;
import services.DeploymentType;
import services.export.PdfExporter;
import services.program.ProgramDefinition;

/** The service responsible for exporting a PDF file */
public final class PdfExporterService {

  private final ApplicationService applicationService;
  private final PdfExporter pdfExporter;

  @Inject
  PdfExporterService(
      ApplicationService applicationService,
      Config configuration,
      DeploymentType deploymentType,
      PdfExporter pdfExporter) {
    this.applicationService = checkNotNull(applicationService);
    this.pdfExporter = checkNotNull(pdfExporter);

    checkNotNull(configuration);
    checkNotNull(deploymentType);
  }

  public PdfExporter.InMemoryPdf generatePdf(Long applicationId, ProgramDefinition program) {
    Optional<Application> applicationMaybe =
        applicationService.getApplication(applicationId, program);
    if (applicationMaybe.isEmpty()) {
      throw new NoSuchElementException(
          String.format("Application %d does not exist.", applicationId));
    }
    Application application = applicationMaybe.get();
    PdfExporter.InMemoryPdf pdf;
    try {
      pdf = pdfExporter.export(application);
    } catch (DocumentException | IOException e) {
      throw new RuntimeException(e);
    }
    return pdf;
  }
}
