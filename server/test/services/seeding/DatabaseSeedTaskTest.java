package services.seeding;

import static controllers.dev.seeding.SampleQuestionDefinitions.ADDRESS_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.getAllSampleQuestionDefinitions;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import models.Question;
import org.junit.Before;
import org.junit.Test;
import repository.QuestionRepository;
import repository.ResetPostgres;
import services.question.QuestionService;

public class DatabaseSeedTaskTest extends ResetPostgres {

  private QuestionRepository questionRepository;
  private DatabaseSeedTask databaseSeedTask;

  @Before
  public void setUp() {
    questionRepository = instanceOf(QuestionRepository.class);
    databaseSeedTask = instanceOf(DatabaseSeedTask.class);
  }

  @Test
  public void seedQuestions_whenQuestionsNotSeededYet_itSeedsTheQuestions() throws Exception {
    assertThat(getAllQuestions().size()).isEqualTo(0);

    databaseSeedTask.seedQuestions();

    assertThat(getAllQuestions().size()).isEqualTo(getAllSampleQuestionDefinitions().size());
  }

  @Test
  public void seedQuestions_whenSomeQuestionsAlreadySeeded_itSeedsTheMissingOnes() {
    instanceOf(QuestionService.class).create(ADDRESS_QUESTION_DEFINITION);
    assertThat(getAllQuestions().size()).isEqualTo(1);

    databaseSeedTask.seedQuestions();

    assertThat(getAllQuestions().size()).isEqualTo(getAllSampleQuestionDefinitions().size());
  }

  private Set<Question> getAllQuestions() {
    return questionRepository.listQuestions().toCompletableFuture().join();
  }
}
