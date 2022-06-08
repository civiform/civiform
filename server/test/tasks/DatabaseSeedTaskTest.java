package tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.Set;
import models.Question;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;
import repository.QuestionRepository;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.question.QuestionService;
import services.question.types.DateQuestionDefinition;
import services.question.types.QuestionDefinition;

public class DatabaseSeedTaskTest extends ResetPostgres {

  private QuestionRepository questionRepository;
  private DatabaseSeedTask databaseSeedTask;

  @Before
  public void setUp() {
    questionRepository = instanceOf(QuestionRepository.class);
    databaseSeedTask = instanceOf(DatabaseSeedTask.class);
  }

  @Test
  public void seedCanonicalQuestions_whenQuestionsNotSeededYet_itSeedsTheCanonicalQuestions()
      throws Exception {
    assertThat(getAllQuestions().size()).isEqualTo(0);

    databaseSeedTask.run();

    assertThat(getAllQuestions().size()).isEqualTo(2);
    assertThat(
            getAllQuestions().stream()
                .map(Question::getQuestionDefinition)
                .map(QuestionDefinition::getName))
        .containsOnly("Applicant Name", "Applicant Date of Birth");
  }

  @Test
  public void seedCanonicalQuestions_whenSomeQuestionsAlreadySeeded_itSeedsTheMissingOnes() {
    instanceOf(QuestionService.class)
        .create(
            new DateQuestionDefinition(
                /* name= */ "Applicant Date of Birth",
                /* enumeratorId= */ Optional.empty(),
                /* description= */ "Applicant's date of birth",
                /* questionText= */ LocalizedStrings.of(
                    Lang.forCode("en-US").toLocale(),
                    "Please enter your date of birth in the format mm/dd/yyyy"),
                /* questionHelpText= */ LocalizedStrings.empty()));
    assertThat(getAllQuestions().size()).isEqualTo(1);

    databaseSeedTask.run();

    assertThat(getAllQuestions().size()).isEqualTo(2);
    assertThat(
            getAllQuestions().stream()
                .map(Question::getQuestionDefinition)
                .map(QuestionDefinition::getName))
        .containsOnly("Applicant Name", "Applicant Date of Birth");
  }

  private Set<Question> getAllQuestions() {
    return questionRepository.listQuestions().toCompletableFuture().join();
  }
}
