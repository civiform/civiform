package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auto.value.AutoValue;
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

    ImmutableList<AnswerData> answerDatas = roApplicantProgramService.getSummaryData();
    ImmutableMap.Builder<Path, Optional<?>> entriesBuilder = ImmutableMap.builder();

    for (AnswerData answerData : answerDatas) {
      // We suppress the unchecked warning because create() returns a genericized
      // QuestionJsonPresenter, but we ignore the generic's type so that we can get
      // the json entries for any Question in one line.
      @SuppressWarnings("unchecked")
      ImmutableMap<Path, Optional<?>> questionEntries =
          presenterFactory
              .create(answerData.applicantQuestion().getType())
              .getJsonEntries(answerData.createQuestion());
      entriesBuilder.putAll(questionEntries);
    }

    JsonExportData jsonExportData =
        JsonExportData.builder()
            .setAdminName(application.getProgram().getProgramDefinition().adminName())
            .setApplicantId(application.getApplicant().id)
            .setApplicationId(application.id)
            .setProgramId(programDefinition.id())
            .setLanguageTag(
                roApplicantProgramService.getApplicantData().preferredLocale().toLanguageTag())
            .setCreateTime(application.getCreateTime())
            .setSubmitterEmail(application.getSubmitterEmail().orElse("Applicant"))
            .setSubmitTimeOpt(application.getSubmitTime())
            .setStatusOpt(application.getLatestStatus())
            .setApplicationEntries(entriesBuilder.build())
            .build();

    return buildJsonApplication(jsonExportData);
  }

  private CfJsonDocumentContext buildJsonApplication(JsonExportData jsonExportData) {
    CfJsonDocumentContext jsonApplication = new CfJsonDocumentContext(makeEmptyJsonObject());

    jsonApplication.putString(Path.create("program_name"), jsonExportData.adminName());
    jsonApplication.putLong(Path.create("program_version_id"), jsonExportData.programId());
    jsonApplication.putLong(Path.create("applicant_id"), jsonExportData.applicantId());
    jsonApplication.putLong(Path.create("application_id"), jsonExportData.applicationId());
    jsonApplication.putString(Path.create("language"), jsonExportData.languageTag());
    jsonApplication.putString(
        Path.create("create_time"),
        dateConverter.renderDateTimeDataOnly(jsonExportData.createTime()));
    jsonApplication.putString(Path.create("submitter_email"), jsonExportData.submitterEmail());

    Path submitTimePath = Path.create("submit_time");
    Optional.ofNullable(jsonExportData.submitTimeOpt())
        .ifPresentOrElse(
            submitTime ->
                jsonApplication.putString(
                    submitTimePath, dateConverter.renderDateTimeDataOnly(submitTime)),
            () -> jsonApplication.putNull(submitTimePath));

    Path statusPath = Path.create("status");
    jsonExportData
        .statusOpt()
        .ifPresentOrElse(
            status -> jsonApplication.putString(statusPath, status),
            () -> jsonApplication.putNull(statusPath));

    exportEntriesToJsonApplication(jsonApplication, jsonExportData.applicationEntries());
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

  @AutoValue
  public abstract static class JsonExportData {
    public abstract String adminName();

    public abstract long applicantId();

    public abstract long applicationId();

    public abstract long programId();

    public abstract String languageTag();

    public abstract Instant createTime();

    public abstract String submitterEmail();

    public abstract Instant submitTimeOpt();

    public abstract Optional<String> statusOpt();

    public abstract ImmutableMap<Path, Optional<?>> applicationEntries();

    static Builder builder() {
      return new AutoValue_JsonExporter_JsonExportData.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {

      public abstract Builder setAdminName(String adminName);

      public abstract Builder setApplicantId(long applicantId);

      public abstract Builder setApplicationId(long applicationId);

      public abstract Builder setProgramId(long programId);

      public abstract Builder setLanguageTag(String languageTag);

      public abstract Builder setCreateTime(Instant createTime);

      public abstract Builder setSubmitterEmail(String submitterEmail);

      public abstract Builder setSubmitTimeOpt(Instant submitTimeOpt);

      public abstract Builder setStatusOpt(Optional<String> statusOpt);

      public abstract Builder setApplicationEntries(
          ImmutableMap<Path, Optional<?>> applicationEntries);

      public abstract JsonExportData build();
    }
  }
}
