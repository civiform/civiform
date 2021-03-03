package export;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import models.Applicant;
import models.Program;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import services.program.*;

public class CsvExporterTest {
  private static Program fakeProgramWithCsvExport;
  private ImmutableList<Applicant> fakeApplicants;
  private Writer writer;
  private ByteArrayOutputStream inMemoryBytes;

  public static CsvExportConfig createFakeCsvConfig() {
    return CsvExportConfig.builder()
        .addColumn(
            Column.builder().setHeader("first name").setJsonPath("$.applicant.first_name").build())
        .addColumn(
            Column.builder().setHeader("last name").setJsonPath("$.applicant.last_name").build())
        .addColumn(Column.builder().setHeader("column").setJsonPath("$.applicant.column").build())
        .build();
  }

  @BeforeClass
  public static void createFakeProgram() {
    ProgramDefinition definition =
        ProgramDefinition.builder()
            .setId(1L)
            .setName("fake program")
            .setDescription("fake program description")
            .addExportDefinition(
                ExportDefinition.builder()
                    .setEngine(ExportEngine.CSV)
                    .setCsvConfig(Optional.of(createFakeCsvConfig()))
                    .build())
            .build();
    fakeProgramWithCsvExport = new Program(definition);
  }

  @Before
  public void createFakeApplicants() {
    Applicant fakeApplicantOne = new Applicant();
    fakeApplicantOne.getApplicantData().put("$.applicant", "first_name", "Alice");
    fakeApplicantOne.getApplicantData().put("$.applicant", "last_name", "Appleton");
    fakeApplicantOne
        .getApplicantData()
        .put("$.applicant", "column", "Some Value \" containing ,,, special characters");

    Applicant fakeApplicantTwo = new Applicant();
    fakeApplicantTwo.getApplicantData().put("$.applicant", "first_name", "Bob");
    fakeApplicantTwo.getApplicantData().put("$.applicant", "last_name", "Baker");
    fakeApplicantTwo.getApplicantData().put("$.applicant", "column", "");
    this.fakeApplicants = ImmutableList.of(fakeApplicantOne, fakeApplicantTwo);
  }

  @Before
  public void createInMemoryWriter() {
    this.inMemoryBytes = new ByteArrayOutputStream();
    this.writer = new OutputStreamWriter(inMemoryBytes, StandardCharsets.UTF_8);
  }

  @Test
  public void fillOutCsv() throws IOException {
    ExporterFactory exporterFactory = new ExporterFactory();
    List<Exporter> exporters = exporterFactory.createExporters(this.fakeProgramWithCsvExport);
    assertThat(exporters).hasSize(1);
    assertThat(exporters.get(0)).isInstanceOf(CsvExporter.class);
    for (Applicant applicant : fakeApplicants) {
      exporters.get(0).export(applicant, writer);
    }
    writer.close();

    CSVParser parser =
        CSVParser.parse(
            inMemoryBytes.toString(StandardCharsets.UTF_8),
            CSVFormat.DEFAULT.withFirstRecordAsHeader());
    assertThat(parser.getHeaderMap()).containsEntry("first name", 0);
    assertThat(parser.getHeaderMap()).containsEntry("last name", 1);
    assertThat(parser.getHeaderMap()).containsEntry("column", 2);
    List<CSVRecord> records = parser.getRecords();
    assertThat(records).hasSize(2);
    assertThat(records.get(0).get("first name")).isEqualTo("Alice");
    assertThat(records.get(1).get("last name")).isEqualTo("Baker");
  }
}
