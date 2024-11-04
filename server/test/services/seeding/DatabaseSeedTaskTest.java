package services.seeding;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Set;
import models.CategoryModel;
import models.LifecycleStage;
import models.QuestionModel;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;
import repository.CategoryRepository;
import repository.QuestionRepository;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.question.QuestionService;
import services.question.types.DateQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;

public class DatabaseSeedTaskTest extends ResetPostgres {

  private QuestionRepository questionRepository;
  private CategoryRepository categoryRepository;
  private DatabaseSeedTask databaseSeedTask;

  @Before
  public void setUp() {
    questionRepository = instanceOf(QuestionRepository.class);
    categoryRepository = instanceOf(CategoryRepository.class);
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

  @Test
  public void seedProgramCategories_whenCategoriesNotSeededYet_insertsCategories() {
    databaseSeedTask.run();

    ImmutableList<CategoryModel> allCategories = categoryRepository.listCategories();
    ImmutableList<String> supportedLanguages =
        ImmutableList.of("am", "en-US", "es-US", "ko", "lo", "so", "tl", "vi", "zh-TW");

    assertThat(allCategories.size()).isEqualTo(12);
    allCategories.forEach(
        category -> {
          assertThat(category.getLocalizedName().getDefault()).isNotEmpty();
          assertThat(category.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
          supportedLanguages.forEach(
              lang -> {
                assertThat(
                        category
                            .getLocalizedName()
                            .hasTranslationFor(Lang.forCode(lang).toLocale()))
                    .isTrue();
              });
        });
  }

  @Test
  public void seedProgramCategories_whenCategoriesAlreadySeeded_doesNothing() {
    databaseSeedTask.run();
    assertThat(categoryRepository.listCategories().size()).isEqualTo(12);

    databaseSeedTask.run(); // Run again to ensure categories aren't re-added.
    assertThat(categoryRepository.listCategories().size()).isEqualTo(12);
  }

  private Set<QuestionModel> getAllQuestions() {
    return questionRepository.listQuestions().toCompletableFuture().join();
  }
}
