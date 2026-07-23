package mapping.admin.questions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import forms.questions.TextQuestionForm;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import services.question.ReadOnlyQuestionService;
import services.question.types.QuestionDefinition;
import views.admin.questions.QuestionEditPageViewModel;

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
}
