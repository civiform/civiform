package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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

/** Exports all applications for a given program as JSON. */
public final class JsonExporter {

  private final ApplicantService applicantService;
  private final ProgramService programService;
  private final DateConverter dateConverter;
  private final QuestionJsonPresenter.Factory presenterFactory;

  @Inject
  JsonExporter(
      ApplicantService applicantService,
      ProgramService programService,
      DateConverter dateConverter,
      QuestionJsonPresenter.Factory presenterFactory) {
    this.applicantService = checkNotNull(applicantService);
    this.programService = checkNotNull(programService);
    this.dateConverter = dateConverter;
    this.presenterFactory = checkNotNull(presenterFactory);
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

  private void exportToJsonApplication(
      CfJsonDocumentContext jsonApplication, AnswerData answerData) {
    // We suppress the unchecked warning because create() returns a genericized
    // QuestionJsonPresenter, but we ignore the generic's type so that we can get
    // the json entries for any Question in one line.
    @SuppressWarnings("unchecked")
    ImmutableMap<Path, ?> entries =
        presenterFactory
            .create(answerData.applicantQuestion().getType())
            .getJsonEntries(answerData.createQuestion());

    for (Map.Entry<Path, ?> entry : entries.entrySet()) {
      Path path = entry.getKey().asApplicationPath();

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
