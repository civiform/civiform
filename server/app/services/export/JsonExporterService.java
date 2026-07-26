package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.DocumentContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import models.ApplicationModel;
import models.LifecycleStage;
import models.TrustedIntermediaryGroupModel;
import org.apache.commons.lang3.NotImplementedException;
import repository.SubmittedApplicationFilter;
import services.CfJsonDocumentContext;
import services.DateConverter;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ApplicantService;
import services.applicant.ApplicationScoreMetadata;
import services.applicant.JsonPathProvider;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.Scalar;
import services.export.enums.RevisionState;
import services.export.enums.SubmitterType;
import services.pagination.PaginationResult;
import services.pagination.SubmitTimeSequentialAccessPaginationSpec;
import services.program.ProgramDefinition;
import services.program.ProgramService;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionType;

/** Exports all applications for a given program as JSON. */
public final class JsonExporterService {

  private final ApplicantService applicantService;
  private final ProgramService programService;
  private final DateConverter dateConverter;
  private final QuestionJsonPresenter.Factory presenterFactory;
  private static final String EMPTY_VALUE = "";
  // API property names for answer-option scores. These match the storage keys in
  // ApplicationScoreMetadata but are kept separate: those name persisted snapshot keys, these
  // name the exported shape.
  private static final String SCORE_PROPERTY = "score";
  private static final String SCORES_PROPERTY = "scores";
  private static final String TOTAL_SCORE_PROPERTY = "total_score";

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
      SubmitTimeSequentialAccessPaginationSpec paginationSpec,
      SubmittedApplicationFilter filters) {
    return export(programDefinition, paginationSpec, filters, /* includeScores= */ false);
  }

  /**
   * Returns a JSON list of applications to the given program, using the pagination behavior and
   * filters supplied.
   *
   * @param includeScores whether the answer-option-scoring feature flag is on for this request,
   *     resolved at the controller boundary. When true, supported questions gain {@code
   *     score}/{@code scores} properties and applications gain a top-level {@code total_score};
   *     when false the properties are absent entirely.
   */
  public String export(
      ProgramDefinition programDefinition,
      SubmitTimeSequentialAccessPaginationSpec paginationSpec,
      SubmittedApplicationFilter filters,
      boolean includeScores) {
    PaginationResult<ApplicationModel> paginationResult =
        programService.getSubmittedProgramApplicationsAllVersions(
            programDefinition.id(), paginationSpec, filters);

    return exportPage(programDefinition, paginationResult, includeScores);
  }

  /**
   * Returns a JSON list of applications to the given program, using the page of applications
   * supplied, without answer-option scores. See {@link #exportPage(ProgramDefinition,
   * PaginationResult, boolean)}.
   */
  public String exportPage(
      ProgramDefinition programDefinition, PaginationResult<ApplicationModel> paginationResult) {
    return exportPage(programDefinition, paginationResult, /* includeScores= */ false);
  }

  /**
   * Returns a JSON list of applications to the given program, using the page of applications
   * supplied.
   *
   * @param programDefinition the program definition of the exported applications
   * @param paginationResult the page of applications to export
   * @param includeScores whether the answer-option-scoring feature flag is on for this request
   * @return a JSON string representing a list of applications
   */
  public String exportPage(
      ProgramDefinition programDefinition,
      PaginationResult<ApplicationModel> paginationResult,
      boolean includeScores) {
    ImmutableList<ApplicationModel> applications = paginationResult.getPageContents();

    ImmutableMap<Long, ProgramDefinition> programDefinitionsForAllVersions =
        programService.getAllVersionsFullProgramDefinition(programDefinition.id()).stream()
            .collect(ImmutableMap.toImmutableMap(ProgramDefinition::id, pd -> pd));

    // Build a template JSON document of all possible questions that have ever been in the program.
    // TODO(#8147): Reduce code duplication once we find a long term solution. Here we've moved the
    // template creation outside of the loop, so we don't rebuild it for each application, but as a
    // result we've duplicated the ApplicantQuestion -> questionEntries map -> add to JSON document
    // flow.
    Map<Path, ApplicantQuestion> answersToExport = new HashMap<>();
    for (ProgramDefinition pd : programDefinitionsForAllVersions.values()) {
      // We use an empty ApplicantData because these should all be exported as unanswered questions.
      applicantService
          .getReadOnlyApplicantProgramService(new ApplicantData(), pd)
          .getAllQuestions()
          .filter(aq -> !aq.getType().equals(QuestionType.STATIC))
          .forEach(aq -> answersToExport.putIfAbsent(aq.getContextualizedPath(), aq));
    }
    ImmutableMap.Builder<Path, Optional<?>> entriesBuilder = ImmutableMap.builder();
    // The template contains questions from all represented versions. Checkbox score paths cannot
    // take a single static template value (per application they are null when scoring was not
    // applied and [] when it was but the question is unanswered), so they are collected here and
    // initialized per application before persisted values are overlaid. They are stored
    // application-rooted because they are written directly into each application's output
    // document, not through the entry dispatch that rewrites the root.
    Set<Path> templateCheckboxScoreApiPaths = new HashSet<>();
    for (ApplicantQuestion applicantQuestion : answersToExport.values()) {
      // We suppress the unchecked warning because create() returns a genericized
      // QuestionJsonPresenter, but we ignore the generic's type so that we can get
      // the json entries for any Question in one line.
      @SuppressWarnings("unchecked")
      ImmutableMap<Path, Optional<?>> questionEntries =
          presenterFactory
              .create(applicantQuestion.getType())
              .getAllJsonEntries(applicantQuestion.getQuestion());
      entriesBuilder.putAll(questionEntries);
      if (includeScores
          && QuestionType.supportsOptionScores(applicantQuestion.getType())) {
        Path apiPath = applicantQuestion.getContextualizedPath().asNestedEntitiesPath();
        if (applicantQuestion.getType() == QuestionType.CHECKBOX) {
          Path scoresApiPath = apiPath.join(SCORES_PROPERTY);
          entriesBuilder.put(scoresApiPath, Optional.empty());
          templateCheckboxScoreApiPaths.add(scoresApiPath.asApplicationPath());
        } else {
          entriesBuilder.put(apiPath.join(SCORE_PROPERTY), Optional.empty());
        }
      }
    }
    CfJsonDocumentContext template = new CfJsonDocumentContext();
    exportApplicationEntriesToJsonApplication(template, entriesBuilder.buildKeepingLast());
    // TODO(#8147): I'm not sure if reading the template out into a string, just to re-parse it into
    // a JsonData for each application, is more or less efficient than trying to clone a JsonData
    // object.
    String jsonStringTemplate = template.asJsonString();
    ImmutableSet<Path> checkboxScoreApiPaths = ImmutableSet.copyOf(templateCheckboxScoreApiPaths);

    // Then use the template when exporting each application.
    DocumentContext jsonData =
        applications.stream()
            .map(
                app ->
                    buildApplicationExportData(
                        app,
                        programDefinitionsForAllVersions.get(app.getProgram().id),
                        includeScores))
            .collect(
                Collectors.collectingAndThen(
                    ImmutableList.toImmutableList(),
                    appDataList ->
                        convertApplicationExportDataListToJsonArray(
                            appDataList, jsonStringTemplate, includeScores, checkboxScoreApiPaths)));

    return jsonData.jsonString();
  }

  /**
   * Converts a list of {@link ApplicationExportData} to a JSON array, without answer-option
   * scores.
   */
  public DocumentContext convertApplicationExportDataListToJsonArray(
      ImmutableList<ApplicationExportData> applicationExportDataList, String jsonTemplate) {
    return convertApplicationExportDataListToJsonArray(
        applicationExportDataList,
        jsonTemplate,
        /* includeScores= */ false,
        /* checkboxScoreApiPaths= */ ImmutableSet.of());
  }

  /**
   * Converts a list of {@link ApplicationExportData} to a JSON array.
   *
   * @param applicationExportDataList the list of applications to export as JSON
   * @param checkboxScoreApiPaths the application-rooted paths of all checkbox {@code scores}
   *     properties in the template, initialized per application before persisted values are
   *     overlaid
   * @return the exported applications, as a JSON array
   */
  public DocumentContext convertApplicationExportDataListToJsonArray(
      ImmutableList<ApplicationExportData> applicationExportDataList,
      String jsonTemplate,
      boolean includeScores,
      ImmutableSet<Path> checkboxScoreApiPaths) {
    DocumentContext applications = makeEmptyJsonArray();
    applicationExportDataList.forEach(
        applicationExportData -> {
          applications.add(
              "$",
              convertExportDataToJson(
                      applicationExportData, jsonTemplate, includeScores, checkboxScoreApiPaths)
                  .getDocumentContext()
                  .json());
        });
    return applications;
  }

  private ApplicationExportData buildApplicationExportData(
      ApplicationModel application, ProgramDefinition programDefinition, boolean includeScores) {
    // A fresh private copy of the application's stored snapshot; score values come from its
    // persisted metadata only, never re-resolved from current question versions, so exports are
    // stable across later score edits.
    ApplicantData snapshot = application.getApplicantData();
    Optional<Double> totalScore =
        includeScores
            ? snapshot.readDouble(ApplicationScoreMetadata.totalScorePath())
            : Optional.empty();

    ImmutableMap.Builder<Path, Optional<?>> entriesBuilder = ImmutableMap.builder();
    applicantService
        .getReadOnlyApplicantProgramService(application, programDefinition)
        .getAllQuestions()
        .filter(aq -> !aq.getType().equals(QuestionType.STATIC))
        .forEach(
            aq -> {
              // We suppress the unchecked warning because create() returns a genericized
              // QuestionJsonPresenter, but we ignore the generic's type so that we can get
              // the json entries for any Question in one line.
              @SuppressWarnings("unchecked")
              ImmutableMap<Path, Optional<?>> questionEntries =
                  presenterFactory.create(aq.getType()).getAllJsonEntries(aq.getQuestion());
              entriesBuilder.putAll(questionEntries);
              if (includeScores) {
                entriesBuilder.putAll(buildScoreEntries(aq, snapshot, totalScore));
              }
            });

    return ApplicationExportData.builder()
        .setTotalScore(totalScore)
        .setAdminName(programDefinition.adminName())
        .setApplicantId(application.getOriginalApplicantId().orElse(application.getApplicant().id))
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
        .setStatusLastModifiedTime(application.getStatusLastModifiedTime())
        .setApplicationNote(application.getLatestNote())
        .setRevisionState(toRevisionState(application.getLifecycleStage()))
        // TODO(#9212): There should never be duplicate entries because question paths should be
        // unique, but due to #9212 there sometimes are. They point at the same location in the
        // applicant data so it doesn't matter which one we keep.
        .addApplicationEntries(entriesBuilder.buildKeepingLast())
        .build();
  }

  /**
   * Builds the additive {@code score}/{@code scores} entries for a supported-type question,
   * reading persisted score metadata from the application snapshot. This runs centrally rather
   * than in each {@link QuestionJsonPresenter}.
   */
  private static ImmutableMap<Path, Optional<?>> buildScoreEntries(
      ApplicantQuestion applicantQuestion, ApplicantData snapshot, Optional<Double> totalScore) {
    if (!QuestionType.supportsOptionScores(applicantQuestion.getType())) {
      return ImmutableMap.of();
    }
    Path contextualizedPath = applicantQuestion.getContextualizedPath();
    Path apiPath = contextualizedPath.asNestedEntitiesPath();
    if (applicantQuestion.getType() != QuestionType.CHECKBOX) {
      // Unanswered, unscored option, and scoring-not-applied all read as absent and emit null.
      return ImmutableMap.of(
          apiPath.join(SCORE_PROPERTY),
          snapshot.readDouble(ApplicationScoreMetadata.scorePath(contextualizedPath)));
    }

    Path scoresApiPath = apiPath.join(SCORES_PROPERTY);
    if (totalScore.isEmpty()) {
      // Scoring was not applied to this application: null.
      return ImmutableMap.of(scoresApiPath, Optional.empty());
    }
    Optional<ImmutableList<Long>> selections =
        snapshot.readLongList(contextualizedPath.join(Scalar.SELECTIONS));
    Optional<List<Double>> storedScores =
        snapshot.readNullableDoubleList(ApplicationScoreMetadata.scoresPath(contextualizedPath));
    if (selections.isEmpty()
        || storedScores.isEmpty()
        || selections.get().size() != storedScores.get().size()) {
      // Scoring applied but unanswered (or corrupt metadata): empty array.
      return ImmutableMap.of(
          scoresApiPath, Optional.of(new NullableDoubleArray(ImmutableList.of())));
    }
    // Pair the unchanged stored selection ids with the persisted scores (first occurrence wins),
    // then emit in the same definition order and duplicate-filtering as the selections presenter.
    Map<Long, Double> scoreByOptionId = new HashMap<>();
    for (int i = 0; i < selections.get().size(); i++) {
      Double score = storedScores.get().get(i);
      if (score != null) {
        scoreByOptionId.putIfAbsent(selections.get().get(i), score);
      }
    }
    ImmutableList<Long> selectedIds = selections.get();
    List<Double> emittedScores = new ArrayList<>();
    ((MultiOptionQuestionDefinition) applicantQuestion.getQuestionDefinition())
        .getOptions().stream()
            .filter(option -> selectedIds.contains(option.id()))
            .map(QuestionOption::id)
            .forEach(optionId -> emittedScores.add(scoreByOptionId.get(optionId)));
    return ImmutableMap.of(scoresApiPath, Optional.of(new NullableDoubleArray(emittedScores)));
  }

  private CfJsonDocumentContext convertExportDataToJson(
      ApplicationExportData applicationExportData,
      String jsonTemplate,
      boolean includeScores,
      ImmutableSet<Path> checkboxScoreApiPaths) {
    CfJsonDocumentContext jsonApplication = new CfJsonDocumentContext(jsonTemplate);

    if (includeScores) {
      // Initialize every checkbox scores property for this application: null when scoring was not
      // applied, [] when it was; persisted values overlay below.
      for (Path scoresApiPath : checkboxScoreApiPaths) {
        if (applicationExportData.totalScore().isPresent()) {
          jsonApplication.putArray(scoresApiPath, ImmutableList.of());
        } else {
          jsonApplication.putNull(scoresApiPath);
        }
      }
      Path totalScorePath = Path.create(TOTAL_SCORE_PROPERTY);
      applicationExportData
          .totalScore()
          .ifPresentOrElse(
              total -> jsonApplication.putDouble(totalScorePath, total),
              () -> jsonApplication.putNull(totalScorePath));
    }

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

    Path notePath = Path.create("application_note");
    applicationExportData
        .applicationNote()
        .ifPresentOrElse(
            applicationNote -> jsonApplication.putString(notePath, applicationNote),
            () -> jsonApplication.putNull(notePath));

    Path statusLastModiedTimePath = Path.create("status_last_modified_time");
    applicationExportData
        .statusLastModifiedTime()
        .ifPresentOrElse(
            statusLastModifiedTime ->
                jsonApplication.putString(
                    statusLastModiedTimePath,
                    dateConverter.renderDateTimeIso8601ExtendedOffset(statusLastModifiedTime)),
            () -> jsonApplication.putNull(statusLastModiedTimePath));

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
      } else if (maybeJsonValue.get() instanceof String str) {
        jsonApplication.putString(path, str);
      } else if (maybeJsonValue.get() instanceof Long l) {
        jsonApplication.putLong(path, l);
      } else if (maybeJsonValue.get() instanceof Double d) {
        jsonApplication.putDouble(path, d);
      } else if (maybeJsonValue.get() instanceof NullableDoubleArray nullableDoubleArray) {
        // Null-holed numeric score arrays cross the dispatch in an explicit wrapper; without it
        // they would be silently dropped here.
        jsonApplication.putArray(path, nullableDoubleArray.values());
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

  private static RevisionState toRevisionState(LifecycleStage lifecycleStage) {
    return switch (lifecycleStage) {
      case ACTIVE -> RevisionState.CURRENT;
      case OBSOLETE -> RevisionState.OBSOLETE;
      default ->
          throw new NotImplementedException(
              "Revision state not supported for LifeCycleStage." + lifecycleStage.name());
    };
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

    public abstract Optional<String> applicationNote();

    public abstract Optional<String> status();

    public abstract Optional<Instant> statusLastModifiedTime();

    public abstract RevisionState revisionState();

    /**
     * The application's total answer-option score; empty when scoring was not applied (or scores
     * were not requested). Emitted as a nullable 64-bit {@code total_score} when scores are
     * included.
     */
    public abstract Optional<Double> totalScore();

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

      public abstract Builder setStatusLastModifiedTime(Optional<Instant> statusLastModifiedTime);

      public abstract Builder setApplicationNote(Optional<String> applicationNote);

      public abstract Builder setRevisionState(RevisionState revisionState);

      public abstract Builder setTotalScore(Optional<Double> totalScore);

      abstract ImmutableMap.Builder<Path, Optional<?>> applicationEntriesBuilder();

      public Builder addApplicationEntries(ImmutableMap<Path, Optional<?>> applicationEntries) {
        applicationEntriesBuilder().putAll(applicationEntries);
        return this;
      }

      public abstract ApplicationExportData build();
    }
  }
}
