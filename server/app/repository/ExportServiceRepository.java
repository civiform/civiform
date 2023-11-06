package repository;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Provider;
import io.ebean.DB;
import io.ebean.Database;
import java.util.LinkedHashSet;
import java.util.List;
import javax.inject.Inject;
import models.Application;
import models.LifecycleStage;
import models.Version;
import services.applicant.AnswerData;
import services.applicant.ApplicantService;
import services.applicant.ReadOnlyApplicantProgramService;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

/** Implements queries related to CSV exporting needs. */
public final class ExportServiceRepository {
  private final Database database;
  private final Provider<VersionRepository> versionRepositoryProvider;
  private final ApplicantService applicantService;

  @Inject
  public ExportServiceRepository(
      Provider<VersionRepository> versionRepositoryProvider, ApplicantService applicantService) {
    this.database = DB.getDefault();
    this.versionRepositoryProvider = checkNotNull(versionRepositoryProvider);
    this.applicantService = checkNotNull(applicantService);
  }

  /**
   * This method queries for all submitted applications in the database ordered by their createTime
   * and checks if they have used the question passed in the input param. If the question is
   * present, it creates a unique set of all options ever picked along with the question's active
   * version options and creates the return list.
   *
   * @param questionDefinition of a Checkbox question whose CSV Headers needs to be generated
   * @return ImmutableList of all options ever picked + its active version's options for the
   *     checkbox question
   * @throws RuntimeException when the question is not of type Checkbox
   * @throws NoSuchElementException when the active version of the question is missing
   */
  public ImmutableList<String> getMultiSelectedHeaders(QuestionDefinition questionDefinition) {
    if (!questionDefinition.getQuestionType().equals(QuestionType.CHECKBOX)) {
      throw new RuntimeException("The Question Type is not checkbox");
    }
    String questionName = questionDefinition.getName();
    List<Application> applications =
        database
            .find(Application.class)
            .fetch("applicant.applicantData")
            .where()
            .in("lifecycle_stage", ImmutableList.of(LifecycleStage.ACTIVE, LifecycleStage.OBSOLETE))
            .orderBy("createTime")
            .findList();
    LinkedHashSet<AnswerData> answerSet = new LinkedHashSet<>();
    for (Application application : applications) {
      ReadOnlyApplicantProgramService roApplicantService =
          applicantService
              .getReadOnlyApplicantProgramService(application)
              .toCompletableFuture()
              .join();
      roApplicantService
          .getSummaryData()
          .forEach(
              data -> {
                if (!answerSet.contains(data)) {
                  answerSet.add(data);
                }
              });
    }
    LinkedHashSet<String> allOptions = new LinkedHashSet<>();
    answerSet.stream()
        .forEach(
            e -> {
              if (e.questionDefinition().getQuestionType().equals(QuestionType.CHECKBOX)
                  && e.questionDefinition().getName().equals(questionName)
                  && e.isAnswered()) {
                e
                    .applicantQuestion()
                    .createMultiSelectQuestion()
                    .getSelectedOptionAdminNames()
                    .get()
                    .stream()
                    .forEach(
                        option -> {
                          if (!allOptions.contains(option)) {
                            allOptions.add(option);
                          }
                        });
              }
            });
    // update it with the latest version
    Version activeVersion = versionRepositoryProvider.get().getActiveVersion();
    MultiOptionQuestionDefinition currentQuestion =
        (MultiOptionQuestionDefinition)
            versionRepositoryProvider
                .get()
                .getQuestionByNameForVersion(questionName, activeVersion)
                .get()
                .getQuestionDefinition();
    currentQuestion.getOptions().stream()
        .forEach(
            e -> {
              if (!allOptions.contains(e.adminName())) {
                allOptions.add(e.adminName());
              }
            });
    ImmutableList.Builder<String> immtableListBuilder = ImmutableList.builder();
    return immtableListBuilder.addAll(allOptions).build();
  }
}
