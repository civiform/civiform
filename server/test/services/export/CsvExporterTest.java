package services.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import java.util.AbstractMap;
import java.util.List;
import java.util.Optional;
import models.Question;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;
import repository.TimeFilter;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.FileUploadQuestion;
import services.applicant.question.MultiSelectQuestion;
import services.applicant.question.NameQuestion;
import services.applicant.question.Scalar;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import support.ProgramBuilder;

public class CsvExporterTest extends AbstractExporterTest {

  private static final CSVFormat DEFAULT_FORMAT = CSVFormat.DEFAULT.builder().setHeader().build();

  private ApplicantQuestion getApplicantQuestion(QuestionDefinition questionDefinition) {
    return new ApplicantQuestion(questionDefinition, new ApplicantData(), Optional.empty());
  }

  @Override
  protected void createFakeProgram() {
    ProgramBuilder fakeProgram = ProgramBuilder.newActiveProgram();

    createFakeQuestions();
    fakeQuestions.forEach(
        question -> fakeProgram.withBlock().withRequiredQuestion(question).build());

    this.fakeProgram = fakeProgram.build();
  }

  @Test
  public void useProgramCsvExport_noRepeatedEntities() throws Exception {
    createFakeProgram();
    createFakeApplications();

    ExporterService exporterService = instanceOf(ExporterService.class);
    CSVParser parser =
        CSVParser.parse(exporterService.getProgramCsv(fakeProgram.id), DEFAULT_FORMAT);
    List<CSVRecord> records = parser.getRecords();

    assertThat(records).hasSize(3);

    // Assert CSV headers
    Streams.mapWithIndex(
            fakeQuestions.stream()
                .filter(question -> !question.getQuestionDefinition().isEnumerator())
                .flatMap(
                    question ->
                        getApplicantQuestion(question.getQuestionDefinition())
                            .getContextualizedScalars()
                            .keySet()
                            .stream()
                            .filter(
                                path -> !Scalar.getMetadataScalarKeys().contains(path.keyName()))),
            (path, index) -> new AbstractMap.SimpleEntry<Path, Integer>(path, (int) index))
        .forEach(
            entry ->
                assertThat(parser.getHeaderMap())
                    .containsEntry(ExporterService.pathToHeader(entry.getKey()), entry.getValue()));

    NameQuestion nameApplicantQuestion =
        getApplicantQuestion(testQuestionBank.applicantName().getQuestionDefinition())
            .createNameQuestion();
    String firstNameHeader = ExporterService.pathToHeader(nameApplicantQuestion.getFirstNamePath());
    String lastNameHeader = ExporterService.pathToHeader(nameApplicantQuestion.getLastNamePath());
    // Applications should appear most recent first.
    assertThat(records.get(0).get(firstNameHeader)).isEqualTo("Bob");
    assertThat(records.get(1).get(lastNameHeader)).isEqualTo("Appleton");
    // Check list for multiselect in default locale
    Question checkboxQuestion =
        testQuestionBank.getSampleQuestionsForAllTypes().get(QuestionType.CHECKBOX);
    MultiSelectQuestion multiSelectApplicantQuestion =
        getApplicantQuestion(checkboxQuestion.getQuestionDefinition()).createMultiSelectQuestion();
    String multiSelectHeader =
        ExporterService.pathToHeader(multiSelectApplicantQuestion.getSelectionPath());
    assertThat(records.get(1).get(multiSelectHeader)).isEqualTo("[toaster, pepper grinder]");
    // Check link for uploaded file
    Question fileuploadQuestion =
        testQuestionBank.getSampleQuestionsForAllTypes().get(QuestionType.FILEUPLOAD);
    FileUploadQuestion fileuploadApplicantQuestion =
        getApplicantQuestion(fileuploadQuestion.getQuestionDefinition()).createFileUploadQuestion();
    String fileKeyHeader =
        ExporterService.pathToHeader(fileuploadApplicantQuestion.getFileKeyPath());
    assertThat(records.get(1).get(fileKeyHeader))
        .contains(String.format("/admin/programs/%d/files/my-file-key", fakeProgram.id));
  }

  @Test
  public void useProgramCsvExport_noEntities() throws Exception {
    createFakeProgram();

    ExporterService exporterService = instanceOf(ExporterService.class);
    CSVParser parser =
        CSVParser.parse(exporterService.getProgramCsv(fakeProgram.id), DEFAULT_FORMAT);
    List<CSVRecord> records = parser.getRecords();

    assertThat(records).hasSize(0);

    // Assert CSV headers
    Streams.mapWithIndex(
            fakeQuestions.stream()
                .filter(question -> !question.getQuestionDefinition().isEnumerator())
                .flatMap(
                    question ->
                        getApplicantQuestion(question.getQuestionDefinition())
                            .getContextualizedScalars()
                            .keySet()
                            .stream()
                            .filter(
                                path -> !Scalar.getMetadataScalarKeys().contains(path.keyName()))),
            (path, index) -> new AbstractMap.SimpleEntry<Path, Integer>(path, (int) index))
        .forEach(
            entry ->
                assertThat(parser.getHeaderMap())
                    .containsEntry(ExporterService.pathToHeader(entry.getKey()), entry.getValue()));
  }

  @Test
  public void demographyExport_withRepeatedEntities() throws Exception {
    createFakeProgram();
    createFakeApplications();
    createFakeProgramWithEnumerator();

    ExporterService exporterService = instanceOf(ExporterService.class);
    CSVParser parser =
        CSVParser.parse(
            exporterService.getDemographicsCsv(TimeFilter.builder().build()), DEFAULT_FORMAT);

    int id = 0;
    assertThat(parser.getHeaderMap())
        .containsExactlyEntriesOf(
            ImmutableMap.<String, Integer>builder()
                .put("Opaque ID", id++)
                .put("Program", id++)
                .put("Submitter Email (Opaque)", id++)
                .put("TI Organization", id++)
                .put("Create time", id++)
                .put("Submit time", id++)
                .build());
  }

  @Test
  public void demographyExport_noEntities() throws Exception {
    ExporterService exporterService = instanceOf(ExporterService.class);
    CSVParser parser =
        CSVParser.parse(
            exporterService.getDemographicsCsv(TimeFilter.builder().build()), DEFAULT_FORMAT);

    int id = 0;
    assertThat(parser.getHeaderMap())
        .containsExactlyEntriesOf(
            ImmutableMap.<String, Integer>builder()
                .put("Opaque ID", id++)
                .put("Program", id++)
                .put("Submitter Email (Opaque)", id++)
                .put("TI Organization", id++)
                .put("Create time", id++)
                .put("Submit time", id++)
                .build());
    assertThat(parser.getRecords()).hasSize(0);
  }

  @Test
  public void useDefaultCsvConfig_withRepeatedEntities() throws Exception {
    createFakeProgram();
    createFakeApplications();
    createFakeProgramWithEnumerator();

    // Generate default CSV
    ExporterService exporterService = instanceOf(ExporterService.class);
    CSVParser parser =
        CSVParser.parse(
            exporterService.getProgramCsv(fakeProgramWithEnumerator.id), DEFAULT_FORMAT);

    int id = 0;
    assertThat(parser.getHeaderMap())
        .containsExactlyEntriesOf(
            ImmutableMap.<String, Integer>builder()
                .put("Applicant ID", id++)
                .put("Application ID", id++)
                .put("Applicant language", id++)
                .put("Submit time", id++)
                .put("Submitted by", id++)
                .put("applicant name (first_name)", id++)
                .put("applicant name (middle_name)", id++)
                .put("applicant name (last_name)", id++)
                .put("applicant favorite color (text)", id++)
                .put("applicant monthly income (currency)", id++)
                .put("applicant household members[0] - household members name (first_name)", id++)
                .put("applicant household members[0] - household members name (middle_name)", id++)
                .put("applicant household members[0] - household members name (last_name)", id++)
                .put("applicant household members[1] - household members name (first_name)", id++)
                .put("applicant household members[1] - household members name (middle_name)", id++)
                .put("applicant household members[1] - household members name (last_name)", id++)
                .put(
                    "applicant household members[0] - household members jobs[0] - household"
                        + " members days worked (number)",
                    id++)
                .put(
                    "applicant household members[0] - household members jobs[1] - household"
                        + " members days worked (number)",
                    id++)
                .put(
                    "applicant household members[0] - household members jobs[2] - household"
                        + " members days worked (number)",
                    id++)
                .put(
                    "applicant household members[1] - household members jobs[0] - household"
                        + " members days worked (number)",
                    id++)
                .build());

    List<CSVRecord> records = parser.getRecords();
    assertThat(records).hasSize(3);

    // Records should be ordered most recent first.
    assertThat(
            records
                .get(0)
                .get(
                    "applicant household members[0] - household members jobs[2] - household"
                        + " members days worked (number)"))
        .isEqualTo("333");
    assertThat(
            records
                .get(1)
                .get(
                    "applicant household members[0] - household members jobs[2] - household"
                        + " members days worked (number)"))
        .isEqualTo("333");
    assertThat(
            records
                .get(1)
                .get(
                    "applicant household members[1] - household members jobs[0] - household"
                        + " members days worked (number)"))
        .isEqualTo("");
    assertThat(
            records
                .get(2)
                .get(
                    "applicant household members[0] - household members jobs[2] - household"
                        + " members days worked (number)"))
        .isEqualTo("");
    assertThat(
            records
                .get(2)
                .get(
                    "applicant household members[1] - household members jobs[0] - household"
                        + " members days worked (number)"))
        .isEqualTo("100");
  }
}
