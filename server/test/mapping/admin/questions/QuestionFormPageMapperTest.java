package mapping.admin.questions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import forms.questions.DropdownQuestionForm;
import forms.questions.TextQuestionForm;
import forms.questions.YesNoQuestionForm;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import services.question.ReadOnlyQuestionService;
import services.question.types.QuestionDefinition;
import views.admin.questions.QuestionFormPageViewModel;

public final class QuestionFormPageMapperTest {

  private QuestionFormPageMapper mapper;
  private ReadOnlyQuestionService readOnlyQuestionService;

  @Before
  public void setup() {
    mapper = new QuestionFormPageMapper();
    readOnlyQuestionService = mock(ReadOnlyQuestionService.class);
    when(readOnlyQuestionService.getUpToDateQuestions()).thenReturn(ImmutableList.of());
  }

  private QuestionFormPageViewModel mapNewTextForm(TextQuestionForm form) {
    return mapper.mapNew(
        form,
        ImmutableList.of(),
        null,
        /* apiBridgeEnabled= */ false,
        /* enumeratorImprovementsEnabled= */ false,
        /* answerOptionScoringEnabled= */ false,
        readOnlyQuestionService,
        Optional.empty());
  }

  private QuestionFormPageViewModel mapEditTextForm(TextQuestionForm form) {
    return mapper.mapEdit(
        1L,
        form,
        Optional.empty(),
        null,
        /* apiBridgeEnabled= */ false,
        /* enumeratorImprovementsEnabled= */ false,
        /* answerOptionScoringEnabled= */ false,
        readOnlyQuestionService,
        Optional.empty());
  }

  @Test
  public void mapNew_setsQuestionTypeName() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionFormPageViewModel result = mapNewTextForm(form);

    assertThat(result.getQuestionTypeName()).isEqualTo("TEXT");
  }

  @Test
  public void mapNew_setsQuestionTypeLabelAndTitle() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionFormPageViewModel result = mapNewTextForm(form);

    // The label keeps the original casing (used for the preview URL); the
    // title lowercases it.
    assertThat(result.getQuestionTypeLabel()).isEqualTo("Text");
    assertThat(result.getTitle()).isEqualTo("New text question");
  }

  @Test
  public void mapNew_isNotEditMode() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionFormPageViewModel result = mapNewTextForm(form);

    assertThat(result.isEditMode()).isFalse();
  }

  @Test
  public void mapNew_setsFormActionUrl() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionFormPageViewModel result = mapNewTextForm(form);

    assertThat(result.getFormActionUrl()).isNotEmpty();
  }

  @Test
  public void mapNew_withoutRedirectUrl_cancelGoesToQuestionIndex() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionFormPageViewModel result = mapNewTextForm(form);

    assertThat(result.getCancelUrl()).isEqualTo("/admin/questions");
  }

  @Test
  public void mapNew_withRedirectUrl_cancelUsesRedirectUrl() {
    TextQuestionForm form = new TextQuestionForm();
    form.setRedirectUrl("/some/redirect/url");

    QuestionFormPageViewModel result = mapNewTextForm(form);

    assertThat(result.getCancelUrl()).isEqualTo("/some/redirect/url");
  }

  @Test
  public void mapNew_buildsEnumeratorOptionsWithDefaultFirst() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionFormPageViewModel result = mapNewTextForm(form);

    assertThat(result.getEnumeratorOptions()).hasSize(1);
    assertThat(result.getEnumeratorOptions().get(0).getLabel()).isEqualTo("does not repeat");
    assertThat(result.getEnumeratorOptions().get(0).getValue()).isEmpty();
  }

  @Test
  public void mapNew_enumeratorSelectEnabledByDefault() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionFormPageViewModel result = mapNewTextForm(form);

    assertThat(result.isEnumeratorSelectEnabled()).isTrue();
  }

  @Test
  public void mapNew_enumeratorSelectDisabledWhenFormDisablesIt() {
    TextQuestionForm form = new TextQuestionForm();
    form.setEnumeratorSelectEnabled(false);

    QuestionFormPageViewModel result = mapNewTextForm(form);

    assertThat(result.isEnumeratorSelectEnabled()).isFalse();
  }

  @Test
  public void mapNew_textQuestion_isNotMapQuestion() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionFormPageViewModel result = mapNewTextForm(form);

    assertThat(result.isMapQuestion()).isFalse();
  }

  @Test
  public void mapNew_showsHelpTextForTextQuestion() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionFormPageViewModel result = mapNewTextForm(form);

    assertThat(result.isShowHelpText()).isTrue();
  }

  @Test
  public void mapNew_textQuestion_hasQuestionConfigTrue() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionFormPageViewModel result = mapNewTextForm(form);

    assertThat(result.hasQuestionConfig()).isTrue();
    assertThat(result.getYesNoConfig()).isNull();
  }

  @Test
  public void mapNew_yesNoQuestion_buildsYesNoConfig() {
    QuestionFormPageViewModel result =
        mapper.mapNew(
            new YesNoQuestionForm(),
            ImmutableList.of(),
            null,
            /* apiBridgeEnabled= */ false,
            /* enumeratorImprovementsEnabled= */ false,
            /* answerOptionScoringEnabled= */ false,
            readOnlyQuestionService,
            Optional.empty());

    assertThat(result.getYesNoConfig()).isNotNull();
    assertThat(result.getYesNoConfig().showLabel()).isTrue();
    assertThat(result.getYesNoConfig().options()).hasSize(4);
  }

  @Test
  public void mapNew_scoringEnabledSupportedType_showsScores() {
    QuestionFormPageViewModel result =
        mapper.mapNew(
            new DropdownQuestionForm(),
            ImmutableList.of(),
            null,
            /* apiBridgeEnabled= */ false,
            /* enumeratorImprovementsEnabled= */ false,
            /* answerOptionScoringEnabled= */ true,
            readOnlyQuestionService,
            Optional.empty());

    assertThat(result.isShowScores()).isTrue();
  }

  @Test
  public void mapNew_scoringDisabled_hidesScores() {
    QuestionFormPageViewModel result =
        mapper.mapNew(
            new DropdownQuestionForm(),
            ImmutableList.of(),
            null,
            /* apiBridgeEnabled= */ false,
            /* enumeratorImprovementsEnabled= */ false,
            /* answerOptionScoringEnabled= */ false,
            readOnlyQuestionService,
            Optional.empty());

    assertThat(result.isShowScores()).isFalse();
  }

  @Test
  public void mapNew_scoringEnabledUnsupportedTypes_hidesScores() {
    QuestionFormPageViewModel yesNoResult =
        mapper.mapNew(
            new YesNoQuestionForm(),
            ImmutableList.of(),
            null,
            /* apiBridgeEnabled= */ false,
            /* enumeratorImprovementsEnabled= */ false,
            /* answerOptionScoringEnabled= */ true,
            readOnlyQuestionService,
            Optional.empty());
    QuestionFormPageViewModel textResult =
        mapper.mapNew(
            new TextQuestionForm(),
            ImmutableList.of(),
            null,
            /* apiBridgeEnabled= */ false,
            /* enumeratorImprovementsEnabled= */ false,
            /* answerOptionScoringEnabled= */ true,
            readOnlyQuestionService,
            Optional.empty());

    assertThat(yesNoResult.isShowScores()).isFalse();
    assertThat(textResult.isShowScores()).isFalse();
  }

  @Test
  public void scoreDisplayValue_formatsAndToleratesBlankInvalidAndSparseEntries() {
    QuestionFormPageViewModel model = mapNewTextForm(new TextQuestionForm());

    assertThat(model.scoreDisplayValue(List.of("2.0"), 0)).isEqualTo("2");
    assertThat(model.scoreDisplayValue(List.of("1.50"), 0)).isEqualTo("1.5");
    assertThat(model.scoreDisplayValue(List.of("-0.50"), 0)).isEqualTo("-0.5");
    assertThat(model.scoreDisplayValue(List.of(""), 0)).isNull();
    assertThat(model.scoreDisplayValue(List.of("junk"), 0)).isNull();
    assertThat(model.scoreDisplayValue(List.of(), 0)).isNull();
    assertThat(model.scoreDisplayValue(Arrays.asList((String) null), 0)).isNull();
  }

  @Test
  public void mapNew_setsErrorMessageWithLegacyPrefixAndToastId() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionFormPageViewModel result =
        mapper.mapNew(
            form,
            ImmutableList.of(),
            null,
            /* apiBridgeEnabled= */ false,
            /* enumeratorImprovementsEnabled= */ false,
            /* answerOptionScoringEnabled= */ false,
            readOnlyQuestionService,
            Optional.of("Something went wrong"));

    assertThat(result.getErrorMessage()).contains("Error: Something went wrong");
    assertThat(result.getErrorToastId()).isNotEmpty();
  }

  @Test
  public void mapNew_withoutErrorMessage_hasNoToastId() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionFormPageViewModel result = mapNewTextForm(form);

    assertThat(result.getErrorMessage()).isEmpty();
    assertThat(result.getErrorToastId()).isNull();
  }

  @Test
  public void mapNew_textQuestion_showsDemographicFields() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionFormPageViewModel result = mapNewTextForm(form);

    assertThat(result.isShowDemographicFields()).isTrue();
  }

  @Test
  public void mapNew_displayModeShownWhenApiBridgeEnabled() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionFormPageViewModel result =
        mapper.mapNew(
            form,
            ImmutableList.of(),
            null,
            /* apiBridgeEnabled= */ true,
            /* enumeratorImprovementsEnabled= */ false,
            /* answerOptionScoringEnabled= */ false,
            readOnlyQuestionService,
            Optional.empty());

    assertThat(result.isShowDisplayModeFields()).isTrue();
  }

  @Test
  public void mapNew_displayModeHiddenWhenApiBridgeDisabled() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionFormPageViewModel result = mapNewTextForm(form);

    assertThat(result.isShowDisplayModeFields()).isFalse();
  }

  @Test
  public void mapEdit_isEditMode() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionFormPageViewModel result = mapEditTextForm(form);

    assertThat(result.isEditMode()).isTrue();
  }

  @Test
  public void mapEdit_setsTitle() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionFormPageViewModel result = mapEditTextForm(form);

    assertThat(result.getTitle()).isEqualTo("Edit text question");
  }

  @Test
  public void mapEdit_setsQuestionId() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionFormPageViewModel result =
        mapper.mapEdit(
            42L,
            form,
            Optional.empty(),
            null,
            /* apiBridgeEnabled= */ false,
            /* enumeratorImprovementsEnabled= */ false,
            /* answerOptionScoringEnabled= */ false,
            readOnlyQuestionService,
            Optional.empty());

    assertThat(result.getQuestionId()).isEqualTo(42L);
  }

  @Test
  public void mapEdit_setsFormActionUrl() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionFormPageViewModel result = mapEditTextForm(form);

    assertThat(result.getFormActionUrl()).isNotEmpty();
  }

  @Test
  public void mapEdit_withNoEnumerator_setsDefaultEnumeratorDisplayName() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionFormPageViewModel result = mapEditTextForm(form);

    assertThat(result.getEnumeratorDisplayName()).isEqualTo("does not repeat");
  }

  @Test
  public void mapEdit_withEnumerator_setsEnumeratorDisplayName() {
    TextQuestionForm form = new TextQuestionForm();
    QuestionDefinition enumQuestion = mock(QuestionDefinition.class);
    when(enumQuestion.getName()).thenReturn("household-members");

    QuestionFormPageViewModel result =
        mapper.mapEdit(
            1L,
            form,
            Optional.of(enumQuestion),
            null,
            /* apiBridgeEnabled= */ false,
            /* enumeratorImprovementsEnabled= */ false,
            /* answerOptionScoringEnabled= */ false,
            readOnlyQuestionService,
            Optional.empty());

    assertThat(result.getEnumeratorDisplayName()).isEqualTo("household-members");
  }

  @Test
  public void mapEdit_setsErrorMessageWithLegacyPrefixAndToastId() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionFormPageViewModel result =
        mapper.mapEdit(
            1L,
            form,
            Optional.empty(),
            null,
            /* apiBridgeEnabled= */ false,
            /* enumeratorImprovementsEnabled= */ false,
            /* answerOptionScoringEnabled= */ false,
            readOnlyQuestionService,
            Optional.of("Error occurred"));

    assertThat(result.getErrorMessage()).contains("Error: Error occurred");
    assertThat(result.getErrorToastId()).isNotEmpty();
  }
}
