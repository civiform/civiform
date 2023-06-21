package services.export;

import static com.google.common.base.Preconditions.checkNotNull;
import static services.question.types.QuestionType.ADDRESS;
import static services.question.types.QuestionType.DROPDOWN;
import static services.question.types.QuestionType.EMAIL;
import static services.question.types.QuestionType.FILEUPLOAD;
import static services.question.types.QuestionType.ID;
import static services.question.types.QuestionType.NAME;
import static services.question.types.QuestionType.RADIO_BUTTON;
import static services.question.types.QuestionType.TEXT;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.DocumentContext;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import models.Application;
import org.apache.commons.lang3.tuple.Pair;
import play.libs.F;
import repository.SubmittedApplicationFilter;
import services.CfJsonDocumentContext;
import services.DateConverter;
import services.IdentifierBasedPaginationSpec;
import services.PaginationResult;
import services.Path;
import services.applicant.AnswerData;
import services.applicant.ApplicantService;
import services.applicant.JsonPathProvider;
import services.applicant.ReadOnlyApplicantProgramService;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.question.types.QuestionType;

/** Exports all applications for a given program as JSON. */
public final class JsonExporter {

  private final ApplicantService applicantService;
  private final ProgramService programService;
  private final DateConverter dateConverter;

  // Question types for which we should call getApplicationPath() on their path
  // when constructing the API response.
  private static final ImmutableSet<QuestionType> USE_APPLICATION_PATH_TYPES =
      ImmutableSet.of(NAME, ID, TEXT, EMAIL, ADDRESS, DROPDOWN, RADIO_BUTTON, FILEUPLOAD);

  @Inject
  JsonExporter(
      ApplicantService applicantService,
      ProgramService programService,
      DateConverter dateConverter) {
    this.applicantService = checkNotNull(applicantService);
    this.programService = checkNotNull(programService);
    this.dateConverter = dateConverter;
  }

  public Pair<String, PaginationResult<Application>> export(
      ProgramDefinition programDefinition,
      IdentifierBasedPaginationSpec<Long> paginationSpec,
      SubmittedApplicationFilter filters) {
    PaginationResult<Application> paginationResult;
    try {
      paginationResult =
          programService.getSubmittedProgramApplicationsAllVersions(
              programDefinition.id(), F.Either.Left(paginationSpec), filters);
    } catch (ProgramNotFoundException e) {
      throw new RuntimeException(e);
    }

    return export(programDefinition, paginationResult);
  }

  public Pair<String, PaginationResult<Application>> export(
      ProgramDefinition programDefinition, PaginationResult<Application> paginationResult) {
    var applications = paginationResult.getPageContents();

    DocumentContext jsonApplications = makeEmptyJsonArray();

    for (Application application : applications) {
      CfJsonDocumentContext applicationJson = buildJsonApplication(application, programDefinition);
      jsonApplications.add("$", applicationJson.getDocumentContext().json());
    }

    return Pair.of(jsonApplications.jsonString(), paginationResult);
  }

  private CfJsonDocumentContext buildJsonApplication(
      Application application, ProgramDefinition programDefinition) {
    ReadOnlyApplicantProgramService roApplicantProgramService =
        applicantService.getReadOnlyApplicantProgramService(application, programDefinition);
    CfJsonDocumentContext jsonApplication = new CfJsonDocumentContext(makeEmptyJsonObject());

    jsonApplication.putString(
        Path.create("program_name"), application.getProgram().getProgramDefinition().adminName());
    jsonApplication.putLong(Path.create("program_version_id"), application.getProgram().id);
    jsonApplication.putLong(Path.create("applicant_id"), application.getApplicant().id);
    jsonApplication.putLong(Path.create("application_id"), application.id);
    jsonApplication.putString(
        Path.create("language"),
        roApplicantProgramService.getApplicantData().preferredLocale().toLanguageTag());
    jsonApplication.putString(
        Path.create("create_time"),
        dateConverter.renderDateTimeDataOnly(application.getCreateTime()));
    jsonApplication.putString(
        Path.create("submitter_email"), application.getSubmitterEmail().orElse("Applicant"));

    Path submitTimePath = Path.create("submit_time");
    Optional.ofNullable(application.getSubmitTime())
        .ifPresentOrElse(
            submitTime ->
                jsonApplication.putString(
                    submitTimePath, dateConverter.renderDateTimeDataOnly(submitTime)),
            () -> jsonApplication.putNull(submitTimePath));

    Path statusPath = Path.create("status");
    application
        .getLatestStatus()
        .ifPresentOrElse(
            status -> jsonApplication.putString(statusPath, status),
            () -> jsonApplication.putNull(statusPath));

    for (AnswerData answerData : roApplicantProgramService.getSummaryData()) {
      exportToJsonApplication(jsonApplication, answerData);
    }

    return jsonApplication;
  }

  private static void exportToJsonApplication(
      CfJsonDocumentContext jsonApplication, AnswerData answerData) {
    ImmutableMap<Path, ?> entries = answerData.createQuestion().getJsonEntries();

    for (Map.Entry<Path, ?> entry : entries.entrySet()) {
      Path path = entry.getKey();

      if (USE_APPLICATION_PATH_TYPES.contains(answerData.questionDefinition().getQuestionType())) {
        path = path.asApplicationPath();
      }

      Object value = entry.getValue();
      if (value instanceof String) {
        jsonApplication.putString(path, (String) value);
      } else if (value instanceof Long) {
        jsonApplication.putLong(path, (Long) value);
      } else if (value instanceof Double) {
        jsonApplication.putDouble(path, (Double) value);
      } else if (instanceOfNonEmptyImmutableListOfString(value)) {
        @SuppressWarnings("unchecked")
        ImmutableList<String> list = (ImmutableList<String>) value;
        jsonApplication.putArray(path, list);
      }
    }
  }

  // Returns true if value is a non-empty ImmutableList<String>. This is the best
  // we can do given Java type erasure.
  private static boolean instanceOfNonEmptyImmutableListOfString(Object value) {
    if (!(value instanceof ImmutableList<?>)) {
      return false;
    }

    ImmutableList<?> list = (ImmutableList<?>) value;
    return !list.isEmpty() && list.get(0) instanceof String;
  }

  private DocumentContext makeEmptyJsonArray() {
    return JsonPathProvider.getJsonPath().parse("[]");
  }

  private DocumentContext makeEmptyJsonObject() {
    return JsonPathProvider.getJsonPath().parse("{}");
  }
}
