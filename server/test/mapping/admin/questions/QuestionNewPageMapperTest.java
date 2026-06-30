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
  public void map_setsQuestionTypeLabel() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionNewPageViewModel result = mapTextForm(form);

    assertThat(result.getQuestionTypeLabel()).isEqualTo("text");
  }

  @Test
  public void map_setsFormActionUrl() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionNewPageViewModel result = mapTextForm(form);

    assertThat(result.getFormActionUrl()).isNotEmpty();
  }

  @Test
  public void map_withEmptyRedirectUrl_setsCancelUrlToQuestionIndex() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionNewPageViewModel result = mapTextForm(form);

    assertThat(result.getCancelUrl()).isNotEmpty();
  }

  @Test
  public void map_textQuestion_hasQuestionConfigTrue() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionNewPageViewModel result = mapTextForm(form);

    assertThat(result.hasQuestionConfig()).isTrue();
  }

  @Test
  public void map_enumeratorSelectEnabled_defaultsTrue() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionNewPageViewModel result = mapTextForm(form);

    assertThat(result.isEnumeratorSelectEnabled()).isTrue();
  }

  @Test
  public void map_enumeratorSelectDisabled_carriedFromForm() {
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
  public void map_showsHelpTextForNonStaticQuestions() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionNewPageViewModel result = mapTextForm(form);

    assertThat(result.isShowHelpText()).isTrue();
  }

  @Test
  public void map_setsErrorMessage() {
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

    assertThat(result.getErrorMessage()).contains("Something went wrong");
  }

  @Test
  public void map_showsDemographicFieldsForText() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionNewPageViewModel result = mapTextForm(form);

    assertThat(result.isShowDemographicFields()).isTrue();
  }

  @Test
  public void map_showsDisplayModeFieldsWhenApiBridgeEnabled() {
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
  public void map_hidesDisplayModeFieldsWhenApiBridgeDisabled() {
    TextQuestionForm form = new TextQuestionForm();

    QuestionNewPageViewModel result = mapTextForm(form);

    assertThat(result.isShowDisplayModeFields()).isFalse();
  }
}
