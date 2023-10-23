package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.NoSuchElementException;
import models.LifecycleStage;
import models.Question;
import org.junit.Before;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.QuestionOption;
import services.question.QuestionService;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionDefinitionConfig;

public class ExportServiceRepositoryTest extends ResetPostgres {
  private ExportServiceRepository repo;
  // private Applicant applicant;
  // private Program program;
  private QuestionService questionService;
  private VersionRepository versionRepository;

  @Before
  public void setUp() {
    repo = instanceOf(ExportServiceRepository.class);
    questionService = instanceOf(QuestionService.class);
    versionRepository = instanceOf(VersionRepository.class);
  }

  @Test
  public void getMultiSelectedHeaders_NoPreviousQuestionVersion() {
    QuestionDefinition questionDefinition =
        testQuestionBank.applicantKitchenTools().getQuestionDefinition();
    ImmutableMap<Long, String> multiSelectHeaders =
        repo.getMultiSelectedHeaders(questionDefinition);
    assertThat(multiSelectHeaders.size()).isEqualTo(3);
    assertThat(multiSelectHeaders).containsKey(1L);
    checkMap(multiSelectHeaders, 1L, "toaster");
    checkMap(multiSelectHeaders, 2L, "pepper_grinder");
    checkMap(multiSelectHeaders, 3L, "garlic_press");
  }

  @Test
  public void getMultiSelectedHeaders_DraftVersionExcludedButPublishedVersionIncluded()
      throws Exception {
    Question questionWeather =
        createMultiSelectQuestion("weather", "fall", "spring", "summer", LifecycleStage.ACTIVE);
    MultiOptionQuestionDefinition multiOptionQuestionDefinition =
        (MultiOptionQuestionDefinition) questionWeather.getQuestionDefinition();
    QuestionOption newOption =
        QuestionOption.create(4L, 4L, "winter", LocalizedStrings.of(Locale.US, "winter"));
    ImmutableList<QuestionOption> currentOptions = multiOptionQuestionDefinition.getOptions();
    ImmutableList<QuestionOption> newOptionList =
        ImmutableList.<QuestionOption>builder().addAll(currentOptions).add(newOption).build();
    QuestionDefinition toUpdate =
        new QuestionDefinitionBuilder(questionWeather.getQuestionDefinition())
            .setQuestionOptions(newOptionList)
            .build();
    questionService.update(toUpdate);

    ImmutableMap<Long, String> multiSelectHeaders =
        repo.getMultiSelectedHeaders(questionWeather.getQuestionDefinition());
    // not draft versions are not part of the header list
    assertThat(multiSelectHeaders.size()).isEqualTo(3);
    versionRepository.publishNewSynchronizedVersion();
    ImmutableMap<Long, String> multiSelectHeaderUpdated =
        repo.getMultiSelectedHeaders(questionWeather.getQuestionDefinition());
    assertThat(multiSelectHeaderUpdated.size()).isEqualTo(4);
    checkMap(multiSelectHeaderUpdated, 1L, "fall");
    checkMap(multiSelectHeaderUpdated, 2L, "spring");
    checkMap(multiSelectHeaderUpdated, 3L, "summer");
    checkMap(multiSelectHeaderUpdated, 4L, "winter");
  }

  @Test
  public void getMultiSelectedHeaders_ThrowsExceptionOnWrongQuestionType() {
    QuestionDefinition questionDefinition =
        testQuestionBank.applicantAddress().getQuestionDefinition();
    assertThatThrownBy(() -> repo.getMultiSelectedHeaders(questionDefinition))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("The Question Type is not checkbox");
  }

  @Test
  public void getMultiSelectedHeaders_ThrowsExceptionOnUnpublishedQuestion() {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("test")
            .setDescription("test")
            .setQuestionText(LocalizedStrings.of(Locale.US, "test"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
            .build();
    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, 1L, "option1", LocalizedStrings.of(Locale.US, "option1")),
            QuestionOption.create(2L, 2L, "option2", LocalizedStrings.of(Locale.US, "option2")),
            QuestionOption.create(3L, 3L, "option3", LocalizedStrings.of(Locale.US, "option3")));
    QuestionDefinition definition =
        new MultiOptionQuestionDefinition(
            config,
            questionOptions,
            MultiOptionQuestionDefinition.MultiOptionQuestionType.CHECKBOX);
    assertThatThrownBy(() -> repo.getMultiSelectedHeaders(definition))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessage("No value present");
  }

  private void checkMap(ImmutableMap<Long, String> multiSelectHeaders, Long key, String value) {
    assertThat(multiSelectHeaders).containsKey(key);
    assertThat(multiSelectHeaders.get(key)).isEqualTo(value);
  }

  private Question createMultiSelectQuestion(
      String name, String option1, String option2, String option3, LifecycleStage stage) {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName(name)
            .setDescription(name)
            .setQuestionText(LocalizedStrings.of(Locale.US, name))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
            .build();
    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, 1L, option1, LocalizedStrings.of(Locale.US, option1)),
            QuestionOption.create(2L, 2L, option2, LocalizedStrings.of(Locale.US, option2)),
            QuestionOption.create(3L, 3L, option3, LocalizedStrings.of(Locale.US, option3)));
    QuestionDefinition definition =
        new MultiOptionQuestionDefinition(
            config,
            questionOptions,
            MultiOptionQuestionDefinition.MultiOptionQuestionType.CHECKBOX);
    return testQuestionBank.maybeSave(definition, stage);
  }
}
