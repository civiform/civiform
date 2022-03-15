package services.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import models.Applicant;
import models.DisplayMode;
import models.Program;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.Path;
import services.program.ExportDefinition;
import services.program.ExportEngine;
import services.program.PdfExportConfig;
import services.program.ProgramDefinition;

public class PdfExporterTest extends ResetPostgres {
  private static Program fakeProgramWithPdfExport;
  private Applicant fakeApplicant;
  private Writer writer;
  private ByteArrayOutputStream inMemoryBytes;
  private static final String APPLICANT_VALUE = "this will get filled into the form.";

  @BeforeClass
  public static void createFakeProgram() {
    File basePdf = new File("test/services/export/base.pdf");
    ProgramDefinition definition =
        ProgramDefinition.builder()
            .setId(1L)
            .setAdminName("fake program")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "fake program"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "fake program description"))
            .setExternalLink("")
            .setDisplayMode(DisplayMode.PUBLIC)
            .addExportDefinition(
                ExportDefinition.builder()
                    .setEngine(ExportEngine.PDF)
                    .setPdfConfig(
                        Optional.of(
                            PdfExportConfig.builder()
                                .setBaseDocument(basePdf.toURI())
                                .setMappings(ImmutableMap.of("formfield", "$.applicant.formValue"))
                                .build()))
                    .build())
            .build();
    fakeProgramWithPdfExport = new Program(definition);
  }

  @Before
  public void createFakeApplicant() {
    this.fakeApplicant = new Applicant();
    this.fakeApplicant
        .getApplicantData()
        .putString(Path.create("applicant.formValue"), APPLICANT_VALUE);
  }

  @Before
  public void createInMemoryWriter() {
    this.inMemoryBytes = new ByteArrayOutputStream();
    this.writer = new OutputStreamWriter(inMemoryBytes, StandardCharsets.UTF_8);
  }

  @Test
  public void fillOneFormEntry() throws IOException {
    // Check that the form is as expected.
    File basePdf = new File("test/services/export/base.pdf");
    assertThat(basePdf.canRead()).isTrue();
    PDDocument doc = PDDocument.load(basePdf);
    PDField formfield = doc.getDocumentCatalog().getAcroForm().getField("formfield");
    assertThat(formfield).isNotNull();
    assertThat(formfield.getValueAsString()).isEmpty();

    // Create exporter and perform services.export.
    ExporterFactory exporterFactory = instanceOf(ExporterFactory.class);
    PdfExporter exporters = exporterFactory.pdfExporter(this.fakeProgramWithPdfExport);
    exporters.export(fakeApplicant, writer);
    writer.close();

    // Load output document and check value.
    PDDocument outputDocument = PDDocument.load(inMemoryBytes.toByteArray());
    PDField outputField = outputDocument.getDocumentCatalog().getAcroForm().getField("formfield");
    assertThat(outputField).isNotNull();
    assertThat(outputField.getValueAsString()).isEqualTo(APPLICANT_VALUE);
  }
}
