package services.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import models.Applicant;
import models.Application;
import models.LifecycleStage;
import models.Program;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import repository.WithPostgresContainer;
import services.Path;
import services.program.Column;
import services.program.ColumnType;
import services.program.CsvExportConfig;
import services.program.ExportDefinition;
import services.program.ExportEngine;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import support.ProgramBuilder;

public class CsvExporterTest extends WithPostgresContainer {
  private static Program fakeProgramWithCsvExport;
  private ImmutableList<Applicant> fakeApplicants;
  private Writer writer;
  private ByteArrayOutputStream inMemoryBytes;

  public static CsvExportConfig createFakeCsvConfig() {
    return CsvExportConfig.builder()
        .addColumn(
            Column.builder()
                .setHeader("first name")
                .setJsonPath(Path.create("$.applicant.applicant_name.first"))
                .setColumnType(ColumnType.APPLICANT)
                .build())
        .addColumn(
            Column.builder()
                .setHeader("last name")
                .setJsonPath(Path.create("$.applicant.applicant_name.last"))
                .setColumnType(ColumnType.APPLICANT)
                .build())
        .addColumn(
            Column.builder()
                .setHeader("column")
                .setJsonPath(Path.create("$.applicant.column"))
                .setColumnType(ColumnType.APPLICANT)
                .build())
        .addColumn(
            Column.builder()
                .setHeader("multiselect")
                .setJsonPath(Path.create("$.applicant.multiselect.selection"))
                .setColumnType(ColumnType.APPLICANT)
                .build())
        .build();
  }

  @BeforeClass
  public static void createFakeProgram() {
    fakeProgramWithCsvExport =
        ProgramBuilder.newProgram()
            .withLifecycleStage(LifecycleStage.ACTIVE)
            .withExportDefinition(
                ExportDefinition.builder()
                    .setEngine(ExportEngine.CSV)
                    .setCsvConfig(Optional.of(createFakeCsvConfig()))
                    .build())
            .build();
  }

  @Before
  public void createFakeApplicants() {
    Applicant fakeApplicantOne = new Applicant();
    fakeApplicantOne
        .getApplicantData()
        .putString(Path.create("applicant.applicant_name.first"), "Alice");
    fakeApplicantOne
        .getApplicantData()
        .putString(Path.create("applicant.applicant_name.last"), "Appleton");
    fakeApplicantOne
        .getApplicantData()
        .putString(
            Path.create("applicant.column"), "Some Value \" containing ,,, special characters");
    fakeApplicantOne
        .getApplicantData()
        .putLong(Path.create("applicant.multiselect.selection[0]"), 1L);
    fakeApplicantOne
        .getApplicantData()
        .putLong(Path.create("applicant.multiselect.selection[1]"), 2L);
    fakeApplicantOne
        .getApplicantData()
        .putString(Path.create("applicant.applicant_favorite_color.text"), "fuchsia");
    fakeApplicantOne.save();

    Applicant fakeApplicantTwo = new Applicant();
    fakeApplicantTwo
        .getApplicantData()
        .putString(Path.create("applicant.applicant_name.first"), "Bob");
    fakeApplicantTwo
        .getApplicantData()
        .putString(Path.create("applicant.applicant_name.last"), "Baker");
    fakeApplicantTwo.getApplicantData().putString(Path.create("applicant.column"), "");
    fakeApplicantTwo
        .getApplicantData()
        .putString(Path.create("applicant.multiselect.selection[0]"), "hello");
    fakeApplicantTwo
        .getApplicantData()
        .putString(Path.create("applicant.applicant_favorite_color.text"), "maroon");
    fakeApplicantTwo.save();
    this.fakeApplicants = ImmutableList.of(fakeApplicantOne, fakeApplicantTwo);
  }

  @Before
  public void createInMemoryWriter() {
    this.inMemoryBytes = new ByteArrayOutputStream();
    this.writer = new OutputStreamWriter(inMemoryBytes, StandardCharsets.UTF_8);
  }

  @Test
  public void fillOutCsv() throws IOException {
    ExporterFactory exporterFactory = instanceOf(ExporterFactory.class);
    CsvExporter exporter = exporterFactory.csvExporter(fakeProgramWithCsvExport);
    for (Applicant applicant : fakeApplicants) {
      Application application =
          new Application(applicant, fakeProgramWithCsvExport, LifecycleStage.ACTIVE);
      exporter.export(application, writer);
    }
    writer.close();

    CSVParser parser =
        CSVParser.parse(
            inMemoryBytes.toString(StandardCharsets.UTF_8),
            CSVFormat.DEFAULT.withFirstRecordAsHeader());
    assertThat(parser.getHeaderMap()).containsEntry("first name", 0);
    assertThat(parser.getHeaderMap()).containsEntry("last name", 1);
    assertThat(parser.getHeaderMap()).containsEntry("column", 2);
    assertThat(parser.getHeaderMap()).containsEntry("multiselect", 3);
    List<CSVRecord> records = parser.getRecords();
    assertThat(records).hasSize(2);
    assertThat(records.get(0).get("first name")).isEqualTo("Alice");
    assertThat(records.get(1).get("last name")).isEqualTo("Baker");
    // Check list for multiselect
    // TODO: export the string values of the selects instead of the IDs
    assertThat(records.get(0).get("multiselect")).isEqualTo("[1, 2]");
  }

  @Test
  public void useExporterService() throws IOException, ProgramNotFoundException {
    ProgramDefinition definition =
        ProgramBuilder.newProgram()
            .withBlock()
            .withQuestion(testQuestionBank().applicantFavoriteColor())
            .buildDefinition();
    ExporterService exporterService = instanceOf(ExporterService.class);

    for (Applicant applicant : fakeApplicants) {
      Application application =
          new Application(applicant, definition.toProgram(), LifecycleStage.ACTIVE);
      application.save();
    }

    CSVParser parser =
        CSVParser.parse(
            exporterService.getProgramCsv(definition.id()),
            CSVFormat.DEFAULT.withFirstRecordAsHeader());

    assertThat(parser.getHeaderMap()).containsEntry("ID", 0);
    assertThat(parser.getHeaderMap()).containsEntry("Submit time", 1);
    assertThat(parser.getHeaderMap()).containsEntry("applicant favorite color.text", 2);

    assertThat(parser.getHeaderMap()).hasSize(3);
    List<CSVRecord> records = parser.getRecords();
    assertThat(records).hasSize(2);
    assertThat(records.get(0).get("applicant favorite color.text")).isEqualTo("fuchsia");
    assertThat(records.get(1).get("applicant favorite color.text")).isEqualTo("maroon");
  }
}
