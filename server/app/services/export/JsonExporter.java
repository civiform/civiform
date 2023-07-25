package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.DocumentContext;
import java.time.Instant;
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

    String adminName = application.getProgram().getProgramDefinition().adminName();
    long applicantId = application.getApplicant().id;
    long applicationId = application.id;
    String languageTag =
        roApplicantProgramService.getApplicantData().preferredLocale().toLanguageTag();
    Instant createTime = application.getCreateTime();
    String submitterEmail = application.getSubmitterEmail().orElse("Applicant");
    Instant submitTime = application.getSubmitTime();
    Optional<String> status = application.getLatestStatus();
    ImmutableList<AnswerData> answerDatas = roApplicantProgramService.getSummaryData();

    return buildJsonApplication(
        adminName,
        applicantId,
        applicationId,
        languageTag,
        createTime,
        submitterEmail,
        submitTime,
        status,
        answerDatas,
        programDefinition);
  }

  private CfJsonDocumentContext buildJsonApplication(
      String adminName,
      long applicantId,
      long applicationId,
      String languageTag,
      Instant createTime,
      String submitterEmail,
      Instant submitTimeOpt,
      Optional<String> statusOpt,
      ImmutableList<AnswerData> answerDatas,
      ProgramDefinition programDefinition) {
    CfJsonDocumentContext jsonApplication = new CfJsonDocumentContext(makeEmptyJsonObject());

    jsonApplication.putString(Path.create("program_name"), adminName);
    jsonApplication.putLong(Path.create("program_version_id"), programDefinition.id());
    jsonApplication.putLong(Path.create("applicant_id"), applicantId);
    jsonApplication.putLong(Path.create("application_id"), applicationId);
    jsonApplication.putString(Path.create("language"), languageTag);
    jsonApplication.putString(
        Path.create("create_time"), dateConverter.renderDateTimeDataOnly(createTime));
    jsonApplication.putString(Path.create("submitter_email"), submitterEmail);

    Path submitTimePath = Path.create("submit_time");
    Optional.ofNullable(submitTimeOpt)
        .ifPresentOrElse(
            submitTime ->
                jsonApplication.putString(
                    submitTimePath, dateConverter.renderDateTimeDataOnly(submitTime)),
            () -> jsonApplication.putNull(submitTimePath));

    Path statusPath = Path.create("status");
    statusOpt.ifPresentOrElse(
        status -> jsonApplication.putString(statusPath, status),
        () -> jsonApplication.putNull(statusPath));

    for (AnswerData answerData : answerDatas) {
      // We suppress the unchecked warning because create() returns a genericized
      // QuestionJsonPresenter, but we ignore the generic's type so that we can get
      // the json entries for any Question in one line.
      @SuppressWarnings("unchecked")
      ImmutableMap<Path, Optional<?>> entries =
          presenterFactory
              .create(answerData.applicantQuestion().getType())
              .getJsonEntries(answerData.createQuestion());

      exportEntriesToJsonApplication(jsonApplication, entries);
    }

    return jsonApplication;
  }

  public static void exportEntriesToJsonApplication(
      CfJsonDocumentContext jsonApplication, ImmutableMap<Path, Optional<?>> entries) {
    for (Map.Entry<Path, Optional<?>> entry : entries.entrySet()) {
      Path path = entry.getKey().asApplicationPath();

      var maybeJsonValue = entry.getValue();
      if (maybeJsonValue.isEmpty()) {
        jsonApplication.putNull(path);
      } else if (maybeJsonValue.get() instanceof String) {
        jsonApplication.putString(path, (String) maybeJsonValue.get());
      } else if (maybeJsonValue.get() instanceof Long) {
        jsonApplication.putLong(path, (Long) maybeJsonValue.get());
      } else if (maybeJsonValue.get() instanceof Double) {
        jsonApplication.putDouble(path, (Double) maybeJsonValue.get());
      } else if (instanceOfNonEmptyImmutableListOfString(maybeJsonValue.get())) {
        @SuppressWarnings("unchecked")
        ImmutableList<String> list = (ImmutableList<String>) maybeJsonValue.get();
        jsonApplication.putArray(path, list);
      } else if (instanceOfEmptyImmutableList(maybeJsonValue.get())) {
        jsonApplication.putArray(path, ImmutableList.of());
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

  // Returns true if value is an empty ImmutableList<>.
  private static boolean instanceOfEmptyImmutableList(Object value) {
    if (!(value instanceof ImmutableList<?>)) {
      return false;
    }

    return ((ImmutableList<?>) value).isEmpty();
  }

  private DocumentContext makeEmptyJsonArray() {
    return JsonPathProvider.getJsonPath().parse("[]");
  }

  private DocumentContext makeEmptyJsonObject() {
    return JsonPathProvider.getJsonPath().parse("{}");
  }
}
