package services.seeding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import models.QuestionModel;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;
import repository.QuestionRepository;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.question.QuestionService;
import services.question.types.DateQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;

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

    databaseSeedTask.run();

    assertThat(getAllQuestions().size()).isEqualTo(2);
    assertThat(
            getAllQuestions().stream()
                .map(QuestionModel::getQuestionDefinition)
                .map(QuestionDefinition::getName))
        .containsOnly("Name", "Applicant Date of Birth");
  }

  @Test
  public void seedQuestions_whenSomeQuestionsAlreadySeeded_itSeedsTheMissingOnes() {
    instanceOf(QuestionService.class)
        .create(
            new DateQuestionDefinition(
                QuestionDefinitionConfig.builder()
                    .setName("Applicant Date of Birth")
                    .setDescription("Applicant's date of birth")
                    .setQuestionText(
                        LocalizedStrings.of(
                            Lang.forCode("en-US").toLocale(),
                            "Please enter your date of birth in the format mm/dd/yyyy"))
                    .setQuestionHelpText(LocalizedStrings.empty())
                    .build()));
    assertThat(getAllQuestions().size()).isEqualTo(1);

    databaseSeedTask.run();

    assertThat(getAllQuestions().size()).isEqualTo(2);
    assertThat(
            getAllQuestions().stream()
                .map(QuestionModel::getQuestionDefinition)
                .map(QuestionDefinition::getName))
        .containsOnly("Name", "Applicant Date of Birth");
  }

  private Set<QuestionModel> getAllQuestions() {
    return questionRepository.listQuestions().toCompletableFuture().join();
  }
}
