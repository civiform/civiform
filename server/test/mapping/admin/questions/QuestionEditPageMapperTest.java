package mapping.admin.questions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import forms.questions.TextQuestionForm;
import forms.questions.YesNoQuestionForm;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import services.question.ReadOnlyQuestionService;
import services.question.types.QuestionDefinition;
import views.admin.questions.QuestionEditPageViewModel;
import views.admin.questions.QuestionEditPageViewModel.YesNoConfig;

public final class QuestionEditPageMapperTest {

  private QuestionEditPageMapper mapper;
  private ReadOnlyQuestionService readOnlyQuestionService;

  @Before
  public void setup() {
    mapper = new QuestionEditPageMapper();
    readOnlyQuestionService = mock(ReadOnlyQuestionService.class);
    when(readOnlyQuestionService.getUpToDateQuestions()).thenReturn(ImmutableList.of());
  }

  private QuestionEditPageViewModel mapTextForm(TextQuestionForm form) {
    return mapper.map(
        1L,
        form,
        Optional.empty(),
        null,
        /* apiBridgeEnabled= */ false,
        /* enumeratorImprovementsEnabled= */ false,
        readOnlyQuestionService,
        Optional.empty());
  }

  @Test
  public void map_setsQuestionTypeName() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionEditPageViewModel result = mapTextForm(form);

    assertThat(result.getQuestionTypeName()).isEqualTo("TEXT");
  }

  @Test
  public void map_setsQuestionTypeLabelAndTitle() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionEditPageViewModel result = mapTextForm(form);

    // The label keeps the original casing (used for the preview URL and modal
    // text); the title lowercases it.
    assertThat(result.getQuestionTypeLabel()).isEqualTo("Text");
    assertThat(result.getTitle()).isEqualTo("Edit text question");
  }

  @Test
  public void map_setsQuestionId() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionEditPageViewModel result =
        mapper.map(
            42L,
            form,
            Optional.empty(),
            null,
            /* apiBridgeEnabled= */ false,
            /* enumeratorImprovementsEnabled= */ false,
            readOnlyQuestionService,
            Optional.empty());

    assertThat(result.getQuestionId()).isEqualTo(42L);
  }

  @Test
  public void map_setsFormActionUrl() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionEditPageViewModel result = mapTextForm(form);

    assertThat(result.getFormActionUrl()).isNotEmpty();
  }

  @Test
  public void map_withNoEnumerator_setsDefaultEnumeratorDisplayName() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionEditPageViewModel result = mapTextForm(form);

    assertThat(result.getEnumeratorDisplayName()).isEqualTo("does not repeat");
  }

  @Test
  public void map_withEnumerator_setsEnumeratorDisplayName() {
    TextQuestionForm form = new TextQuestionForm();
    QuestionDefinition enumQuestion = mock(QuestionDefinition.class);
    when(enumQuestion.getName()).thenReturn("household-members");

    QuestionEditPageViewModel result =
        mapper.map(
            1L,
            form,
            Optional.of(enumQuestion),
            null,
            /* apiBridgeEnabled= */ false,
            /* enumeratorImprovementsEnabled= */ false,
            readOnlyQuestionService,
            Optional.empty());

    assertThat(result.getEnumeratorDisplayName()).isEqualTo("household-members");
  }

  @Test
  public void map_textQuestion_isNotMapQuestion() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionEditPageViewModel result = mapTextForm(form);

    assertThat(result.isMapQuestion()).isFalse();
  }

  @Test
  public void map_showsHelpTextForTextQuestion() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionEditPageViewModel result = mapTextForm(form);

    assertThat(result.isShowHelpText()).isTrue();
  }

  @Test
  public void map_textQuestion_hasQuestionConfigTrue() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionEditPageViewModel result = mapTextForm(form);

    assertThat(result.hasQuestionConfig()).isTrue();
    assertThat(result.getYesNoConfig()).isNull();
  }

  @Test
  public void map_textQuestion_showsDemographicFields() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionEditPageViewModel result = mapTextForm(form);

    assertThat(result.isShowDemographicFields()).isTrue();
  }

  @Test
  public void map_setsErrorMessageWithLegacyPrefixAndToastId() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionEditPageViewModel result =
        mapper.map(
            1L,
            form,
            Optional.empty(),
            null,
            /* apiBridgeEnabled= */ false,
            /* enumeratorImprovementsEnabled= */ false,
            readOnlyQuestionService,
            Optional.of("Error occurred"));

    assertThat(result.getErrorMessage()).contains("Error: Error occurred");
    assertThat(result.getErrorToastId()).isNotEmpty();
  }

  @Test
  public void map_withoutErrorMessage_hasNoToastId() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionEditPageViewModel result = mapTextForm(form);

    assertThat(result.getErrorMessage()).isEmpty();
    assertThat(result.getErrorToastId()).isNull();
  }

  @Test
  public void map_displayModeHiddenWhenApiBridgeDisabled() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionEditPageViewModel result =
        mapper.map(
            1L,
            form,
            Optional.empty(),
            null,
            /* apiBridgeEnabled= */ false,
            /* enumeratorImprovementsEnabled= */ false,
            readOnlyQuestionService,
            Optional.empty());

    assertThat(result.isShowDisplayModeFields()).isFalse();
  }

  @Test
  public void map_displayModeShownWhenApiBridgeEnabled() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionEditPageViewModel result =
        mapper.map(
            1L,
            form,
            Optional.empty(),
            null,
            /* apiBridgeEnabled= */ true,
            /* enumeratorImprovementsEnabled= */ false,
            readOnlyQuestionService,
            Optional.empty());

    assertThat(result.isShowDisplayModeFields()).isTrue();
  }

  @Test
  public void buildYesNoConfig_newQuestion_rendersDefaultsWithLabel() {
    YesNoConfig config = QuestionEditPageMapper.buildYesNoConfig(new YesNoQuestionForm());

    assertThat(config.showLabel()).isTrue();
    assertThat(config.options()).hasSize(4);
    assertThat(config.options().stream().map(row -> row.adminName()))
        .containsExactly("yes", "no", "not-sure", "maybe");
    // The default set renders every option checked; "yes" and "no" are
    // required (checked and disabled, with an extra hidden input).
    assertThat(config.options().stream().allMatch(row -> row.checked())).isTrue();
    assertThat(config.options().get(0).required()).isTrue();
    assertThat(config.options().get(1).required()).isTrue();
    assertThat(config.options().get(2).required()).isFalse();
    assertThat(config.options().get(3).required()).isFalse();
    assertThat(config.options().get(0).ariaLabel()).isEqualTo("Admin ID: yes. Option text: Yes.");
  }

  @Test
  public void buildYesNoConfig_existingQuestion_usesFormOptionsWithoutLabel() {
    YesNoQuestionForm form = new YesNoQuestionForm();
    form.setOptionIds(ImmutableList.of(1L, 0L, 2L, 3L));
    form.setOptionAdminNames(ImmutableList.of("yes", "no", "not-sure", "maybe"));
    form.setOptions(ImmutableList.of("Yes", "No", "Not sure", "Maybe"));
    // Only "not-sure" is displayed in addition to the required options.
    form.setDisplayedOptionIds(ImmutableList.of(1L, 0L, 2L));

    YesNoConfig config = QuestionEditPageMapper.buildYesNoConfig(form);

    assertThat(config.showLabel()).isFalse();
    assertThat(config.options()).hasSize(4);
    assertThat(config.options().get(2).checked()).isTrue();
    assertThat(config.options().get(3).checked()).isFalse();
    // Required options are always checked even if not listed as displayed.
    assertThat(config.options().get(0).checked()).isTrue();
    assertThat(config.options().get(1).checked()).isTrue();
  }
}
