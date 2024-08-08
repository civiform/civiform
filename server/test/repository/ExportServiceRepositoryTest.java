package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import models.LifecycleStage;
import models.QuestionModel;
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
        testQuestionBank.checkboxApplicantKitchenTools().getQuestionDefinition();
    ImmutableList<String> multiSelectHeaders =
        repo.getAllHistoricMultiOptionAdminNames(questionDefinition);
    assertThat(multiSelectHeaders.size()).isEqualTo(3);
    assertThat(multiSelectHeaders).containsExactly("toaster", "pepper_grinder", "garlic_press");
  }

  @Test
  public void getMultiSelectedHeaders_DraftVersionExcludedButPublishedVersionIncluded()
      throws Exception {
    QuestionModel questionWeather =
        createMultiSelectQuestion("weather", "fall", "spring", "summer", LifecycleStage.ACTIVE);
    MultiOptionQuestionDefinition multiOptionQuestionDefinition =
        (MultiOptionQuestionDefinition) questionWeather.getQuestionDefinition();

    // add new option but do not publish
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
    ImmutableList<String> multiSelectHeaders =
        repo.getAllHistoricMultiOptionAdminNames(questionWeather.getQuestionDefinition());
    // draft version are not part of the header list
    assertThat(multiSelectHeaders.size()).isEqualTo(3);

    // publish with updated options
    versionRepository.publishNewSynchronizedVersion();
    ImmutableList<String> multiSelectHeaderUpdated =
        repo.getAllHistoricMultiOptionAdminNames(questionWeather.getQuestionDefinition());
    assertThat(multiSelectHeaderUpdated.size()).isEqualTo(4);
    assertThat(multiSelectHeaderUpdated).containsExactly("fall", "spring", "summer", "winter");
  }

  @Test
  public void getMultiSelectedHeaders_DeletedOptionsIncluded() throws Exception {
    QuestionModel questionWeather =
        createMultiSelectQuestion("weather", "fall", "spring", "summer", LifecycleStage.ACTIVE);
    MultiOptionQuestionDefinition multiOptionQuestionDefinition =
        (MultiOptionQuestionDefinition) questionWeather.getQuestionDefinition();

    // add new option and publish
    QuestionOption winterOption =
        QuestionOption.create(4L, 4L, "winter", LocalizedStrings.of(Locale.US, "winter"));
    ImmutableList<QuestionOption> currentOptionsWithoutWinter =
        multiOptionQuestionDefinition.getOptions();
    ImmutableList<QuestionOption> newOptionListWithWinter =
        ImmutableList.<QuestionOption>builder()
            .addAll(currentOptionsWithoutWinter)
            .add(winterOption)
            .build();
    QuestionDefinition toUpdate =
        new QuestionDefinitionBuilder(questionWeather.getQuestionDefinition())
            .setQuestionOptions(newOptionListWithWinter)
            .build();
    questionService.update(toUpdate);

    versionRepository.publishNewSynchronizedVersion();
    ImmutableList<String> multiSelectHeaderWithNewOption =
        repo.getAllHistoricMultiOptionAdminNames(questionWeather.getQuestionDefinition());
    assertThat(multiSelectHeaderWithNewOption.size()).isEqualTo(4);

    // publish again without "winter" option
    QuestionDefinition newUpdate =
        new QuestionDefinitionBuilder(questionWeather.getQuestionDefinition())
            .setQuestionOptions(currentOptionsWithoutWinter)
            .build();
    questionService.update(newUpdate);
    versionRepository.publishNewSynchronizedVersion();
    ImmutableList<String> multiSelectHeaderUpdated =
        repo.getAllHistoricMultiOptionAdminNames(questionWeather.getQuestionDefinition());
    assertThat(multiSelectHeaderUpdated.size()).isEqualTo(4);
    assertThat(multiSelectHeaderUpdated).containsExactly("fall", "spring", "summer", "winter");
  }

  @Test
  public void getMultiSelectedHeaders_ThrowsExceptionOnWrongQuestionType() {
    QuestionDefinition questionDefinition =
        testQuestionBank.addressApplicantAddress().getQuestionDefinition();
    assertThatThrownBy(() -> repo.getAllHistoricMultiOptionAdminNames(questionDefinition))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("The Question Type is not a multi-option type");
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
    assertThatThrownBy(() -> repo.getAllHistoricMultiOptionAdminNames(definition))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Draft questions cannot be exported");
  }

  // TODO(#5957): Structuring this using the Builder pattern would make this easier to extend or
  // customize
  private QuestionModel createMultiSelectQuestion(
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
