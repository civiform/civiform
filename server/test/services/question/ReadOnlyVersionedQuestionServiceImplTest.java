package services.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import models.LifecycleStage;
import models.QuestionModel;
import models.VersionModel;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.QuestionType;
import support.TestQuestionBank;

public class ReadOnlyVersionedQuestionServiceImplTest extends ResetPostgres {
  private VersionRepository versionRepository;
  private TestQuestionBank testQuestionBank;
  private QuestionModel nameQuestion;
  private QuestionModel addressQuestion;
  private QuestionModel basicQuestion;
  private ImmutableList<QuestionModel> questions;
  private ReadOnlyQuestionService service;

  @Before
  public void setupQuestions() {
    testQuestionBank = new TestQuestionBank(true);
    // The question bank initializes these questions in the active version.
    nameQuestion = testQuestionBank.applicantName();
    addressQuestion = testQuestionBank.applicantAddress();
    basicQuestion = testQuestionBank.applicantFavoriteColor();
    questions = ImmutableList.of(nameQuestion, addressQuestion, basicQuestion);
    versionRepository = instanceOf(VersionRepository.class);
    VersionModel version = new VersionModel(LifecycleStage.OBSOLETE);
    addQuestionsToVersion(version, questions);
    service = new ReadOnlyVersionedQuestionServiceImpl(version, versionRepository);
  }

  @Test
  public void getAll_returnsEmpty() {
    VersionModel obsoleteVersion = new VersionModel(LifecycleStage.OBSOLETE);
    obsoleteVersion.save();
    ReadOnlyQuestionService emptyService =
        new ReadOnlyVersionedQuestionServiceImpl(
            obsoleteVersion, instanceOf(VersionRepository.class));
    assertThat(emptyService.getAllQuestions()).isEmpty();
  }

  @Test
  public void getAllQuestions() {
    assertThat(service.getAllQuestions().size()).isEqualTo(3);
  }

  @Test
  public void getActiveAndDraftQuestions_notSupported() {
    assertThatThrownBy(() -> service.getActiveAndDraftQuestions())
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("does not support getActiveAndDraftQuestions");
  }

  @Test
  public void getUpToDateQuestions_notSupported() {
    assertThatThrownBy(() -> service.getUpToDateQuestions())
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("does not support getUpToDateQuestions");
  }

  @Test
  public void getUpToDateEnumeratorQuestions_notSupported() {
    assertThatThrownBy(() -> service.getUpToDateEnumeratorQuestions())
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("does not support getUpToDateEnumeratorQuestions");
  }

  @Test
  public void getEnumeratorQuestions() {
    QuestionModel enumeratorQuestion = testQuestionBank.applicantHouseholdMembers();

    VersionModel version = new VersionModel(LifecycleStage.OBSOLETE);
    addQuestionsToVersion(version, questions);
    addQuestionsToVersion(version, ImmutableList.of(enumeratorQuestion));
    var service = new ReadOnlyVersionedQuestionServiceImpl(version, versionRepository);
    assertThat(service.getAllEnumeratorQuestions().size()).isEqualTo(1);
    assertThat(service.getAllEnumeratorQuestions().get(0))
        .isEqualTo(enumeratorQuestion.getQuestionDefinition());
  }

  @Test
  public void getQuestionDefinition_byId() throws QuestionNotFoundException {
    long questionId = nameQuestion.id;
    assertThat(service.getQuestionDefinition(questionId))
        .isEqualTo(nameQuestion.getQuestionDefinition());
  }

  @Test
  public void getQuestionDefinition_notFound() throws QuestionNotFoundException {
    var questionDefinition = service.getQuestionDefinition(9999L);

    assertThat(questionDefinition.getQuestionType()).isEqualTo(QuestionType.NULL_QUESTION);
  }

  private static void addQuestionsToVersion(
      VersionModel version, ImmutableList<QuestionModel> questions) {
    questions.stream().forEach(version::addQuestion);
    version.save();
  }
}
