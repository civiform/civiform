package views.admin.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import forms.CheckboxQuestionForm;
import forms.DateQuestionForm;
import forms.QuestionForm;
import forms.QuestionFormBuilder;
import forms.YesNoQuestionForm;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.i18n.Lang;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.question.types.DateQuestionDefinition.DateValidationOption.DateType;
import services.question.types.QuestionType;
import services.settings.SettingsManifest;
import views.admin.questions.QuestionConfig;

@RunWith(JUnitParamsRunner.class)
public class QuestionConfigTest {

  private final Messages messages =
      stubMessagesApi().preferred(ImmutableSet.of(Lang.defaultLang()));
  private SettingsManifest settingsManifest;
  private Request request;

  @Before
  public void setUp() {
    settingsManifest = mock(SettingsManifest.class);
    request = fakeRequest().build();
    when(settingsManifest.getDateValidationEnabled(request)).thenReturn(true);
  }

  @Test
  @Parameters(source = QuestionType.class)
  public void resultForAllQuestions(QuestionType questionType) throws Exception {
    // A null question type is not allowed to be created and won't show in the list
    if (questionType == QuestionType.NULL_QUESTION) {
      return;
    }

    QuestionForm questionForm = QuestionFormBuilder.create(questionType);
    Optional<DivTag> maybeConfig =
        QuestionConfig.buildQuestionConfig(questionForm, messages, settingsManifest, request);
    switch (questionType) {
      case ADDRESS:
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted()).contains("Disallow post office boxes");
        break;
      case CHECKBOX:
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted())
            .contains("Minimum number of choices required");
        assertThat(maybeConfig.get().renderFormatted()).contains("Add answer option");
        break;
      case CURRENCY:
        assertThat(maybeConfig).isEmpty();
        break;
      case DATE:
        assertThat(maybeConfig).isPresent();
        String dateConfig = maybeConfig.get().renderFormatted();
        assertThat(dateConfig).contains("Validation parameters");
        assertThat(dateConfig).contains("Start date");
        assertThat(dateConfig).contains("End date");
        break;
      case DROPDOWN:
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted()).contains("Add answer option");
        break;
      case EMAIL:
        assertThat(maybeConfig).isEmpty();
        break;
      case PHONE:
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted())
            .contains(
                "This supports only US and CA phone numbers. If you need other international"
                    + " numbers, please use a Text question.");
        break;
      case ENUMERATOR:
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted()).contains("What are we enumerating");
        break;
      case FILEUPLOAD:
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted()).contains("Maximum number of file uploads");
        break;
      case ID:
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted()).contains("Minimum length");
        break;
      case MAP:
        assertThat(maybeConfig).isEmpty();
        break;
      case NAME:
        assertThat(maybeConfig).isEmpty();
        break;
      case NUMBER:
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted()).contains("Minimum value");
        break;
      case RADIO_BUTTON:
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted()).contains("Add answer option");
        break;
      case STATIC:
        assertThat(maybeConfig).isEmpty();
        break;
      case TEXT:
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted()).contains("Minimum length");
        break;
      case YES_NO:
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted()).contains("Yes");
        break;
      default:
        fail(
            "Unhandled question type: %s. Please add a configuration in"
                + " QuestionConfig.buildQuestionConfig and add an explicit case statement for the"
                + " type.",
            questionType);
    }
  }

  @Test
  public void checkboxForm_preservesNewOptions() {
    CheckboxQuestionForm form = new CheckboxQuestionForm();
    form.setOptions(ImmutableList.of("existing-option-a", "existing-option-b"));
    form.setOptionAdminNames(
        ImmutableList.of("existing-option-admin-a", "existing-option-admin-b"));
    form.setOptionIds(ImmutableList.of(1L, 2L));
    form.setNewOptions(ImmutableList.of("new-option-c", "new-option-d"));
    form.setNewOptionAdminNames(ImmutableList.of("new-option-admin-c", "new-option-admin-d"));

    Optional<DivTag> maybeConfig =
        QuestionConfig.buildQuestionConfig(form, messages, settingsManifest, request);
    assertThat(maybeConfig).isPresent();
    String result = maybeConfig.get().renderFormatted();

    assertThat(result).contains("existing-option-a");
    assertThat(result).contains("existing-option-b");
    assertThat(result).contains("new-option-c");
    assertThat(result).contains("new-option-d");
    assertThat(result).contains("existing-option-admin-a");
    assertThat(result).contains("existing-option-admin-b");
    assertThat(result).contains("new-option-admin-c");
    assertThat(result).contains("new-option-admin-d");
  }

  @Test
  public void yesNoForm_prepopulatesOptions() {
    YesNoQuestionForm form = new YesNoQuestionForm();
    Optional<DivTag> maybeConfig =
        QuestionConfig.buildQuestionConfig(form, messages, settingsManifest, request);
    assertThat(maybeConfig).isPresent();
    String result = maybeConfig.get().renderFormatted();

    assertThat(result).contains("yes");
    assertThat(result).contains("no");
    assertThat(result).contains("not-sure");
    assertThat(result).contains("maybe");
  }

  @Test
  public void buildDateConfig_dateValidationDisabled_isEmpty() throws Exception {
    when(settingsManifest.getDateValidationEnabled(request)).thenReturn(false);
    QuestionForm questionForm = QuestionFormBuilder.create(QuestionType.DATE);

    Optional<DivTag> maybeConfig =
        QuestionConfig.buildQuestionConfig(questionForm, messages, settingsManifest, request);
    assertThat(maybeConfig).isEmpty();
  }

  @Test
  public void buildDateConfig_rendersDefaultMinMaxDateOptions() throws Exception {
    QuestionForm questionForm = QuestionFormBuilder.create(QuestionType.DATE);
    Optional<DivTag> maybeConfig =
        QuestionConfig.buildQuestionConfig(questionForm, messages, settingsManifest, request);
    assertThat(maybeConfig).isPresent();
    String result = maybeConfig.get().renderFormatted();

    // Verify date type options are rendered
    assertThat(result).contains("Any past date");
    assertThat(result).contains("Any future date");
    assertThat(result).contains("Current date of application");
    assertThat(result).contains("Custom date");
    // Verify ANY is selected by default
    Pattern dateTypeAnySelectedPattern = Pattern.compile("<option[^>]*value=\"ANY\" selected>");
    Matcher matcher = dateTypeAnySelectedPattern.matcher(result);
    assertThat(matcher.results().count()).isEqualTo(2);
    // Verify custom date pickers are present
    assertThat(result).contains("min-custom-date-month");
    assertThat(result).contains("max-custom-date-month");
  }

  @Test
  public void buildDateConfig_prepopulatesFormMinMaxDates() throws Exception {
    DateQuestionForm form = new DateQuestionForm();
    form.setMinDateType(DateType.APPLICATION_DATE.toString());
    form.setMaxDateType(DateType.CUSTOM.toString());
    form.setMaxCustomDay("1");
    form.setMaxCustomMonth("2");
    form.setMaxCustomYear("2025");

    Optional<DivTag> maybeConfig =
        QuestionConfig.buildQuestionConfig(form, messages, settingsManifest, request);
    assertThat(maybeConfig).isPresent();
    String result = maybeConfig.get().renderFormatted();

    assertThat(result).containsPattern("<option[^>]*value=\"APPLICATION_DATE\" selected>");
    assertThat(result).containsPattern("<option[^>]*value=\"CUSTOM\" selected>");
    assertThat(result).containsPattern("input[^>]*id=\"max-custom-date-day\"[^>]*value=\"1\"");
    assertThat(result).containsPattern("<option value=\"2\" selected>");
    assertThat(result).containsPattern("input[^>]*id=\"max-custom-date-year\"[^>]*value=\"2025\"");
  }
}
