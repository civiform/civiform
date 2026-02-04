package views.admin.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import forms.CheckboxQuestionForm;
import forms.DateQuestionForm;
import forms.MapQuestionForm;
import forms.QuestionForm;
import forms.QuestionFormBuilder;
import forms.YesNoQuestionForm;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import modules.ThymeleafModule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.thymeleaf.TemplateEngine;
import play.i18n.Lang;
import play.i18n.Messages;
import play.mvc.Http.Request;
import repository.ResetPostgres;
import services.question.types.DateQuestionDefinition.DateValidationOption.DateType;
import services.question.types.QuestionType;
import services.settings.SettingsManifest;
import support.FakeRequestBuilder;
import views.admin.questions.MapQuestionSettingsPartialView;
import views.admin.questions.MapQuestionSettingsPartialViewModel;
import views.admin.questions.QuestionConfig;

@RunWith(JUnitParamsRunner.class)
public class QuestionConfigTest extends ResetPostgres {

  private final Messages messages =
      stubMessagesApi().preferred(ImmutableSet.of(Lang.defaultLang()));
  private SettingsManifest settingsManifest;
  private Request request;
  private ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory;

  @Before
  public void setUp() {
    settingsManifest = mock(SettingsManifest.class);
    request = FakeRequestBuilder.fakeRequestBuilder().cspNonce("nonce-value").build();
    playThymeleafContextFactory = mock(ThymeleafModule.PlayThymeleafContextFactory.class);
    when(playThymeleafContextFactory.create(request))
        .thenReturn(new ThymeleafModule.PlayThymeleafContext());
  }

  @Test
  @Parameters(source = QuestionType.class)
  public void resultForAllQuestions(QuestionType questionType) throws Exception {
    // A null question type is not allowed to be created and won't show in the list
    if (questionType == QuestionType.NULL_QUESTION) {
      return;
    }
    if (questionType == QuestionType.MAP) {
      MapQuestionSettingsPartialViewModel model =
          MapQuestionSettingsPartialViewModel.builder()
              .maxLocationSelections(OptionalInt.of(5))
              .locationName(new MapQuestionForm.Setting("name_key", "Location Name"))
              .locationAddress(new MapQuestionForm.Setting("address_key", "Location Address"))
              .locationDetailsUrl(new MapQuestionForm.Setting("url_key", "Details URL"))
              .filters(ImmutableList.of())
              .possibleKeys(ImmutableList.of("name_key", "address_key", "url_key"))
              .build();
      MapQuestionSettingsPartialView view =
          new MapQuestionSettingsPartialView(
              mock(TemplateEngine.class), playThymeleafContextFactory, settingsManifest);

      Optional<DivTag> mapConfig =
          QuestionConfig.buildQuestionConfigUsingThymeleaf(request, view, model);
      assertThat(mapConfig).isPresent();
      return;
    }

    QuestionForm questionForm = QuestionFormBuilder.create(questionType);
    Optional<DivTag> maybeConfig =
        QuestionConfig.buildQuestionConfig(questionForm, messages, settingsManifest, request);
    switch (questionType) {
      case ADDRESS -> {
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted()).contains("Disallow post office boxes");
      }
      case CHECKBOX -> {
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted())
            .contains("Minimum number of choices required");
        assertThat(maybeConfig.get().renderFormatted()).contains("Add answer option");
      }
      case CURRENCY, STATIC -> assertThat(maybeConfig).isEmpty();
      case DATE -> {
        assertThat(maybeConfig).isPresent();
        String dateConfig = maybeConfig.get().renderFormatted();
        assertThat(dateConfig).contains("Validation parameters");
        assertThat(dateConfig).contains("Start date");
        assertThat(dateConfig).contains("End date");
      }
      case DROPDOWN, RADIO_BUTTON -> {
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted()).contains("Add answer option");
      }
      case EMAIL -> assertThat(maybeConfig).isEmpty();
      case PHONE -> {
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted())
            .contains(
                "This supports only US and CA phone numbers. If you need other international"
                    + " numbers, please use a Text question.");
      }
      case ENUMERATOR -> {
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted()).contains("What are we enumerating");
      }
      case FILEUPLOAD -> {
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted()).contains("Maximum number of file uploads");
      }
      case ID, TEXT -> {
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted()).contains("Minimum length");
      }
      case NAME -> assertThat(maybeConfig).isEmpty();
      case NUMBER -> {
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted()).contains("Minimum value");
      }
      case YES_NO -> {
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted()).contains("Yes");
      }
      default -> {
        var unused =
            fail(
                "Unhandled question type: %s. Please add a configuration in"
                    + " QuestionConfig.buildQuestionConfig and add an explicit case statement for"
                    + " the type.",
                questionType);
      }
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

    assertThat(result).contains("\"optionIds[]\" value=\"1\"");
    assertThat(result).contains("\"optionIds[]\" value=\"0\"");
    assertThat(result).contains("\"optionIds[]\" value=\"2\"");
    assertThat(result).contains("\"optionIds[]\" value=\"3\"");
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

  @Test
  public void yesNoForm_yesAndNoCheckboxesAreDisabled() {
    YesNoQuestionForm form = new YesNoQuestionForm();
    Optional<DivTag> maybeConfig =
        QuestionConfig.buildQuestionConfig(form, messages, settingsManifest, request);
    assertThat(maybeConfig).isPresent();
    String result = maybeConfig.get().renderFormatted();

    // Verify YES and NO checkboxes are disabled and checked
    assertThat(result).containsPattern("id=\"yes\"[^>]*disabled=\"disabled\"");
    assertThat(result).containsPattern("id=\"no\"[^>]*disabled=\"disabled\"");
    assertThat(result).containsPattern("id=\"yes\"[^>]*checked=\"checked\"");
    assertThat(result).containsPattern("id=\"no\"[^>]*checked=\"checked\"");

    // Verify NOT-SURE and MAYBE checkboxes are NOT disabled
    assertThat(result).doesNotContain("id=\"not-sure\"[^>]*disabled");
    assertThat(result).doesNotContain("id=\"maybe\"[^>]*disabled");

    // Verify hidden inputs exist for YES and NO to ensure values are submitted
    // Should have 2 inputs with name="displayedOptionIds[]" and value="1" (one checkbox, one
    // hidden)
    Pattern yesPattern = Pattern.compile("name=\"displayedOptionIds\\[\\]\"[^>]*value=\"1\"");
    Matcher yesMatcher = yesPattern.matcher(result);
    assertThat(yesMatcher.results().count()).isEqualTo(2);

    // Should have 2 inputs with name="displayedOptionIds[]" and value="0" (one checkbox, one
    // hidden)
    Pattern noPattern = Pattern.compile("name=\"displayedOptionIds\\[\\]\"[^>]*value=\"0\"");
    Matcher noMatcher = noPattern.matcher(result);
    assertThat(noMatcher.results().count()).isEqualTo(2);
  }

  @Test
  public void mapQuestionForm_withInvalidFilterKey_showsError() {
    MapQuestionSettingsPartialViewModel model =
        MapQuestionSettingsPartialViewModel.builder()
            .maxLocationSelections(OptionalInt.of(5))
            .locationName(new MapQuestionForm.Setting("name_key", "Location Name"))
            .locationAddress(new MapQuestionForm.Setting("address_key", "Location Address"))
            .locationDetailsUrl(new MapQuestionForm.Setting("url_key", "Details URL"))
            .filters(ImmutableList.of(new MapQuestionForm.Setting("invalidKey", "Invalid Filter")))
            .locationTag(MapQuestionForm.Setting.emptySetting())
            .possibleKeys(ImmutableList.of("name_key", "address_key", "url_key"))
            .build();
    MapQuestionSettingsPartialView view =
        new MapQuestionSettingsPartialView(
            instanceOf(TemplateEngine.class),
            instanceOf(ThymeleafModule.PlayThymeleafContextFactory.class),
            instanceOf(SettingsManifest.class));

    Optional<DivTag> maybeConfig =
        QuestionConfig.buildQuestionConfigUsingThymeleaf(request, view, model);
    assertThat(maybeConfig).isPresent();
    String result = maybeConfig.get().renderFormatted();

    assertThat(result).contains("Error: Key not found. Please select a different key.");
  }
}
