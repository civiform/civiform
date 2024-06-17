package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.DocumentContext;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import models.ApplicationModel;
import models.LifecycleStage;
import models.TrustedIntermediaryGroupModel;
import org.apache.commons.lang3.NotImplementedException;
import play.libs.F;
import repository.SubmittedApplicationFilter;
import services.CfJsonDocumentContext;
import services.DateConverter;
import services.IdentifierBasedPaginationSpec;
import services.PaginationResult;
import services.Path;
import services.applicant.AnswerData;
import services.applicant.ApplicantData;
import services.applicant.ApplicantService;
import services.applicant.JsonPathProvider;
import services.export.enums.RevisionState;
import services.export.enums.SubmitterType;
import services.program.ProgramDefinition;
import services.program.ProgramService;

/** Exports all applications for a given program as JSON. */
public final class JsonExporterService {

  private final ApplicantService applicantService;
  private final ProgramService programService;
  private final DateConverter dateConverter;
  private final QuestionJsonPresenter.Factory presenterFactory;
  private static final String EMPTY_VALUE = "";

  @Inject
  JsonExporterService(
      ApplicantService applicantService,
      ProgramService programService,
      DateConverter dateConverter,
      QuestionJsonPresenter.Factory presenterFactory) {
    this.applicantService = checkNotNull(applicantService);
    this.programService = checkNotNull(programService);
    this.dateConverter = dateConverter;
    this.presenterFactory = checkNotNull(presenterFactory);
  }

  /**
   * Returns a JSON list of applications to the given program, using the pagination behavior and
   * filters supplied.
   *
   * @param programDefinition the program definition of the exported application
   * @param paginationSpec the pagination behavior
   * @param filters the filters to apply
   * @return a JSON string representing a list of applications
   */
  public String export(
      ProgramDefinition programDefinition,
      IdentifierBasedPaginationSpec<Long> paginationSpec,
      SubmittedApplicationFilter filters) {
    PaginationResult<ApplicationModel> paginationResult =
        programService.getSubmittedProgramApplicationsAllVersions(
            programDefinition.id(), F.Either.Left(paginationSpec), filters);

    return exportPage(programDefinition, paginationResult);
  }

  /**
   * Returns a JSON list of applications to the given program, using the page of applications
   * supplied.
   *
   * @param programDefinition the program definition of the exported applications
   * @param paginationResult the page of applications to export
   * @return a JSON string representing a list of applications
   */
  public String exportPage(
      ProgramDefinition programDefinition, PaginationResult<ApplicationModel> paginationResult) {
    ImmutableList<ApplicationModel> applications = paginationResult.getPageContents();

    ImmutableMap<Long, ProgramDefinition> programDefinitionsForAllVersions =
        programService.getAllVersionsFullProgramDefinition(programDefinition.id()).stream()
            .collect(ImmutableMap.toImmutableMap(ProgramDefinition::id, pd -> pd));

    DocumentContext jsonData =
        applications.stream()
            .map(a -> buildApplicationExportData(a, programDefinitionsForAllVersions))
            .collect(
                Collectors.collectingAndThen(
                    ImmutableList.toImmutableList(),
                    this::convertApplicationExportDataToJsonArray));

    return jsonData.jsonString();
  }

  /**
   * Converts a list of {@link ApplicationExportData} to a JSON array.
   *
   * @param applicationExportDataList the list of applications to export as JSON
   * @return the exported applications, as a JSON array
   */
  public DocumentContext convertApplicationExportDataToJsonArray(
      ImmutableList<ApplicationExportData> applicationExportDataList) {
    DocumentContext applications = makeEmptyJsonArray();
    applicationExportDataList.forEach(
        applicationExportData -> {
          applications.add(
              "$", convertExportDataToJson(applicationExportData).getDocumentContext().json());
        });
    return applications;
  }

  private ApplicationExportData buildApplicationExportData(
      ApplicationModel application,
      ImmutableMap<Long, ProgramDefinition> programDefinitionsForAllVersions) {
    Map<Path, AnswerData> answersToExport = new HashMap<>();

    // First retrieve the answers for the program version that aligns with this application
    ProgramDefinition pdForApplication =
        programDefinitionsForAllVersions.get(application.getProgram().id);
    applicantService
        .getReadOnlyApplicantProgramService(application.getApplicantData(), pdForApplication)
        .getSummaryDataAllQuestions()
        .forEach(ad -> answersToExport.putIfAbsent(ad.contextualizedPath(), ad));

    // Then populate the questions that were not in the application's program version. We use an
    // empty ApplicantData because these should all be exported as unanswered questions.
    for (ProgramDefinition pd : programDefinitionsForAllVersions.values()) {
      if (application.getProgram().id == pd.id()) {
        continue;
      }
      applicantService
          .getReadOnlyApplicantProgramService(new ApplicantData(), pd)
          .getSummaryDataAllQuestions()
          .forEach(ad -> answersToExport.putIfAbsent(ad.contextualizedPath(), ad));
    }

    ImmutableMap.Builder<Path, Optional<?>> entriesBuilder = ImmutableMap.builder();
    for (AnswerData answerData : answersToExport.values()) {
      // We suppress the unchecked warning because create() returns a genericized
      // QuestionJsonPresenter, but we ignore the generic's type so that we can get
      // the json entries for any Question in one line.
      @SuppressWarnings("unchecked")
      ImmutableMap<Path, Optional<?>> questionEntries =
          presenterFactory
              .create(answerData.applicantQuestion().getType())
              .getAllJsonEntries(answerData.createQuestion());
      entriesBuilder.putAll(questionEntries);
    }

    return ApplicationExportData.builder()
        .setAdminName(programDefinitionsForAllVersions.get(application.getProgram().id).adminName())
        .setApplicantId(application.getApplicant().id)
        .setApplicationId(application.id)
        .setProgramId(application.getProgram().id)
        .setLanguageTag(application.getApplicantData().preferredLocale().toLanguageTag())
        .setCreateTime(application.getCreateTime())
        // The field on the application is called `submitter_email`, but it's only ever used to
        // store the TI's email, never the applicant's.
        // TODO(#5325): Rename the `submitter_email` database field to `ti_email` and move the
        // submitter_type logic upstream.
        .setSubmitterType(
            application.getSubmitterEmail().isPresent()
                ? SubmitterType.TRUSTED_INTERMEDIARY
                : SubmitterType.APPLICANT)
        .setTiEmail(application.getSubmitterEmail().orElse(EMPTY_VALUE))
        .setTiOrganization(
            application
                .getApplicant()
                .getAccount()
                .getManagedByGroup()
                .map(TrustedIntermediaryGroupModel::getName)
                .orElse(EMPTY_VALUE))
        .setSubmitTime(application.getSubmitTime())
        .setStatus(application.getLatestStatus())
        .setRevisionState(toRevisionState(application.getLifecycleStage()))
        .addApplicationEntries(entriesBuilder.build())
        .build();
  }

  private CfJsonDocumentContext convertExportDataToJson(
      ApplicationExportData applicationExportData) {
    CfJsonDocumentContext jsonApplication = new CfJsonDocumentContext(makeEmptyJsonObject());

    jsonApplication.putString(Path.create("program_name"), applicationExportData.adminName());
    jsonApplication.putLong(Path.create("program_version_id"), applicationExportData.programId());
    jsonApplication.putLong(Path.create("applicant_id"), applicationExportData.applicantId());
    jsonApplication.putLong(Path.create("application_id"), applicationExportData.applicationId());
    jsonApplication.putString(Path.create("language"), applicationExportData.languageTag());
    jsonApplication.putString(
        Path.create("create_time"),
        dateConverter.renderDateTimeIso8601ExtendedOffset(applicationExportData.createTime()));
    jsonApplication.putString(
        Path.create("submitter_type"), applicationExportData.submitterType().toString());
    jsonApplication.putString(Path.create("ti_email"), applicationExportData.tiEmail());
    jsonApplication.putString(
        Path.create("ti_organization"), applicationExportData.tiOrganization());
    Path submitTimePath = Path.create("submit_time");
    Optional.ofNullable(applicationExportData.submitTime())
        .ifPresentOrElse(
            submitTime ->
                jsonApplication.putString(
                    submitTimePath, dateConverter.renderDateTimeIso8601ExtendedOffset(submitTime)),
            () -> jsonApplication.putNull(submitTimePath));
    jsonApplication.putString(
        Path.create("revision_state"), applicationExportData.revisionState().toString());

    Path statusPath = Path.create("status");
    applicationExportData
        .status()
        .ifPresentOrElse(
            status -> jsonApplication.putString(statusPath, status),
            () -> jsonApplication.putNull(statusPath));

    exportApplicationEntriesToJsonApplication(
        jsonApplication, applicationExportData.applicationEntries());
    return jsonApplication;
  }

  private static void exportApplicationEntriesToJsonApplication(
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

  private static RevisionState toRevisionState(LifecycleStage lifecycleStage) {
    switch (lifecycleStage) {
      case ACTIVE:
        return RevisionState.CURRENT;
      case OBSOLETE:
        return RevisionState.OBSOLETE;
      default:
        throw new NotImplementedException(
            "Revision state not supported for LifeCycleStage." + lifecycleStage.name());
    }
  }

  @AutoValue
  public abstract static class ApplicationExportData {
    public abstract String adminName();

    public abstract long applicantId();

    public abstract long applicationId();

    public abstract long programId();

    public abstract String languageTag();

    public abstract Instant createTime();

    public abstract SubmitterType submitterType();

    public abstract String tiEmail();

    public abstract String tiOrganization();

    public abstract Instant submitTime();

    public abstract Optional<String> status();

    public abstract RevisionState revisionState();

    public abstract ImmutableMap<Path, Optional<?>> applicationEntries();

    static Builder builder() {
      return new AutoValue_JsonExporterService_ApplicationExportData.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {

      public abstract Builder setAdminName(String adminName);

      public abstract Builder setApplicantId(long applicantId);

      public abstract Builder setApplicationId(long applicationId);

      public abstract Builder setProgramId(long programId);

      public abstract Builder setLanguageTag(String languageTag);

      public abstract Builder setCreateTime(Instant createTime);

      public abstract Builder setSubmitterType(SubmitterType submitterType);

      public abstract Builder setTiEmail(String tiEmail);

      public abstract Builder setTiOrganization(String tiOrganization);

      public abstract Builder setSubmitTime(Instant submitTimeOpt);

      public abstract Builder setStatus(Optional<String> status);

      public abstract Builder setRevisionState(RevisionState revisionState);

      abstract ImmutableMap.Builder<Path, Optional<?>> applicationEntriesBuilder();

      public Builder addApplicationEntries(ImmutableMap<Path, Optional<?>> applicationEntries) {
        applicationEntriesBuilder().putAll(applicationEntries);
        return this;
      }

      public abstract ApplicationExportData build();
    }
  }
}
