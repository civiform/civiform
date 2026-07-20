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
import views.admin.questions.QuestionNewPageViewModel;

public final class QuestionNewPageMapperTest {

  private QuestionNewPageMapper mapper;
  private ReadOnlyQuestionService readOnlyQuestionService;

  @Before
  public void setup() {
    mapper = new QuestionNewPageMapper();
    readOnlyQuestionService = mock(ReadOnlyQuestionService.class);
    when(readOnlyQuestionService.getUpToDateQuestions()).thenReturn(ImmutableList.of());
  }

  private QuestionNewPageViewModel mapTextForm(TextQuestionForm form) {
    return mapper.map(
        form,
        ImmutableList.of(),
        null,
        /* apiBridgeEnabled= */ false,
        /* enumeratorImprovementsEnabled= */ false,
        readOnlyQuestionService,
        Optional.empty());
  }

  @Test
  public void map_setsQuestionTypeName() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionNewPageViewModel result = mapTextForm(form);

    assertThat(result.getQuestionTypeName()).isEqualTo("TEXT");
  }

  @Test
  public void map_setsQuestionTypeLabelAndTitle() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionNewPageViewModel result = mapTextForm(form);

    // The label keeps the original casing (used for the preview URL); the
    // title lowercases it.
    assertThat(result.getQuestionTypeLabel()).isEqualTo("Text");
    assertThat(result.getTitle()).isEqualTo("New text question");
  }

  @Test
  public void map_setsFormActionUrl() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionNewPageViewModel result = mapTextForm(form);

    assertThat(result.getFormActionUrl()).isNotEmpty();
  }

  @Test
  public void map_withoutRedirectUrl_cancelGoesToQuestionIndex() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionNewPageViewModel result = mapTextForm(form);

    assertThat(result.getCancelUrl()).isNotEmpty();
  }

  @Test
  public void map_withRedirectUrl_cancelUsesRedirectUrl() {
    TextQuestionForm form = new TextQuestionForm();
    form.setRedirectUrl("/some/redirect/url");

    QuestionNewPageViewModel result = mapTextForm(form);

    assertThat(result.getCancelUrl()).isEqualTo("/some/redirect/url");
  }

  @Test
  public void map_buildsEnumeratorOptionsWithDefaultFirst() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionNewPageViewModel result = mapTextForm(form);

    assertThat(result.getEnumeratorOptions()).hasSize(1);
    assertThat(result.getEnumeratorOptions().get(0).getLabel()).isEqualTo("does not repeat");
    assertThat(result.getEnumeratorOptions().get(0).getValue()).isEmpty();
  }

  @Test
  public void map_enumeratorSelectEnabledByDefault() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionNewPageViewModel result = mapTextForm(form);

    assertThat(result.isEnumeratorSelectEnabled()).isTrue();
  }

  @Test
  public void map_enumeratorSelectDisabledWhenFormDisablesIt() {
    TextQuestionForm form = new TextQuestionForm();
    form.setEnumeratorSelectEnabled(false);

    QuestionNewPageViewModel result = mapTextForm(form);

    assertThat(result.isEnumeratorSelectEnabled()).isFalse();
  }

  @Test
  public void map_textQuestion_isNotMapQuestion() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionNewPageViewModel result = mapTextForm(form);

    assertThat(result.isMapQuestion()).isFalse();
  }

  @Test
  public void map_showsHelpTextForTextQuestion() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionNewPageViewModel result = mapTextForm(form);

    assertThat(result.isShowHelpText()).isTrue();
  }

  @Test
  public void map_textQuestion_hasQuestionConfigTrue() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionNewPageViewModel result = mapTextForm(form);

    assertThat(result.hasQuestionConfig()).isTrue();
    assertThat(result.getYesNoConfig()).isNull();
  }

  @Test
  public void map_yesNoQuestion_buildsYesNoConfig() {
    QuestionNewPageViewModel result =
        mapper.map(
            new YesNoQuestionForm(),
            ImmutableList.of(),
            null,
            /* apiBridgeEnabled= */ false,
            /* enumeratorImprovementsEnabled= */ false,
            readOnlyQuestionService,
            Optional.empty());

    assertThat(result.getYesNoConfig()).isNotNull();
    assertThat(result.getYesNoConfig().showLabel()).isTrue();
    assertThat(result.getYesNoConfig().options()).hasSize(4);
  }

  @Test
  public void map_setsErrorMessageWithLegacyPrefixAndToastId() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionNewPageViewModel result =
        mapper.map(
            form,
            ImmutableList.of(),
            null,
            /* apiBridgeEnabled= */ false,
            /* enumeratorImprovementsEnabled= */ false,
            readOnlyQuestionService,
            Optional.of("Something went wrong"));

    assertThat(result.getErrorMessage()).contains("Error: Something went wrong");
    assertThat(result.getErrorToastId()).isNotEmpty();
  }

  @Test
  public void map_textQuestion_showsDemographicFields() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionNewPageViewModel result = mapTextForm(form);

    assertThat(result.isShowDemographicFields()).isTrue();
  }

  @Test
  public void map_displayModeShownWhenApiBridgeEnabled() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionNewPageViewModel result =
        mapper.map(
            form,
            ImmutableList.of(),
            null,
            /* apiBridgeEnabled= */ true,
            /* enumeratorImprovementsEnabled= */ false,
            readOnlyQuestionService,
            Optional.empty());

    assertThat(result.isShowDisplayModeFields()).isTrue();
  }

  @Test
  public void map_displayModeHiddenWhenApiBridgeDisabled() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionNewPageViewModel result = mapTextForm(form);

    assertThat(result.isShowDisplayModeFields()).isFalse();
  }
}
