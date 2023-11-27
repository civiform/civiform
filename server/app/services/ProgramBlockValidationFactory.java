package services;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import models.VersionModel;
import repository.VersionRepository;
import services.question.QuestionService;

/**
 * Factory for creating `ProgramBlockValidation` instances.
 *
 * <p>Needed because - for every question in the question picker, we will need to check if the
 * question is tombstoned, resulting in n+1 DB queries. To avoid this, we create a factory object
 * injecting a VersionRepository and calling getDraftVersionOrCreate() only once per request.
 */
public final class ProgramBlockValidationFactory {

  private final VersionRepository versionRepository;
  private final QuestionService questionService;

  @Inject
  public ProgramBlockValidationFactory(
      VersionRepository versionRepository, QuestionService questionService) {
    this.versionRepository = checkNotNull(versionRepository);
    this.questionService = checkNotNull(questionService);
  }

  /** Creating a ProgramBlockValidation object with version(DB object) as its member variable */
  public ProgramBlockValidation create() {
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    services.question.ActiveAndDraftQuestions activeAndDraftQuestions =
        questionService.getReadOnlyQuestionServiceSync().getActiveAndDraftQuestions();
    return new ProgramBlockValidation(version, activeAndDraftQuestions);
  }
}
