package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.FlashKey;
import java.util.Collections;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.DisplayMode;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.i18n.Lang;
import play.i18n.Langs;
import play.i18n.MessagesApi;
import play.mvc.Http;
import services.AlertSettings;
import services.AlertType;
import services.MessageKey;
import services.applicant.question.ApplicantQuestion;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.program.ProgramType;

@RunWith(JUnitParamsRunner.class)
public class EligibilityAlertSettingsCalculatorTest {

  static String QUESTION_TEXT = "Question text";

  private MessagesApi getMessagesApiMock() {
    Langs langs = new Langs(new play.api.i18n.DefaultLangs());

    Map<String, String> messagesMap =
        ImmutableMap.<String, String>builder()
            .put(
                MessageKey.ALERT_ELIGIBILITY_APPLICANT_FASTFORWARDED_ELIGIBLE_TITLE.getKeyName(),
                "APPLICANT_FASTFORWARDED_ELIGIBLE_TITLE")
            .put(
                MessageKey.ALERT_ELIGIBILITY_APPLICANT_FASTFORWARDED_ELIGIBLE_TEXT.getKeyName(),
                "APPLICANT_FASTFORWARDED_ELIGIBLE_TEXT")
            .put(
                MessageKey.ALERT_ELIGIBILITY_APPLICANT_FASTFORWARDED_NOT_ELIGIBLE_TITLE
                    .getKeyName(),
                "APPLICANT_FASTFORWARDED_NOT_ELIGIBLE_TITLE")
            .put(
                MessageKey.ALERT_ELIGIBILITY_APPLICANT_FASTFORWARDED_NOT_ELIGIBLE_TEXT.getKeyName(),
                "APPLICANT_FASTFORWARDED_NOT_ELIGIBLE_TEXT")
            .put(
                MessageKey.ALERT_ELIGIBILITY_APPLICANT_ELIGIBLE_TITLE.getKeyName(),
                "APPLICANT_ELIGIBLE_TITLE")
            .put(
                MessageKey.ALERT_ELIGIBILITY_APPLICANT_ELIGIBLE_TEXT.getKeyName(),
                "APPLICANT_ELIGIBLE_TEXT")
            .put(
                MessageKey.ALERT_ELIGIBILITY_APPLICANT_NOT_ELIGIBLE_TITLE.getKeyName(),
                "APPLICANT_NOT_ELIGIBLE_TITLE")
            .put(
                MessageKey.ALERT_ELIGIBILITY_APPLICANT_NOT_ELIGIBLE_TEXT.getKeyName(),
                "APPLICANT_NOT_ELIGIBLE_TEXT")
            .put(
                MessageKey.ALERT_ELIGIBILITY_APPLICANT_NOT_ELIGIBLE_TEXT_SHORT.getKeyName(),
                "APPLICANT_NOT_ELIGIBLE_TEXT_SHORT")
            .put(
                MessageKey.ALERT_ELIGIBILITY_TI_FASTFORWARDED_ELIGIBLE_TITLE.getKeyName(),
                "TI_FASTFORWARDED_ELIGIBLE_TITLE")
            .put(
                MessageKey.ALERT_ELIGIBILITY_TI_FASTFORWARDED_ELIGIBLE_TEXT.getKeyName(),
                "TI_FASTFORWARDED_ELIGIBLE_TEXT")
            .put(
                MessageKey.ALERT_ELIGIBILITY_TI_FASTFORWARDED_NOT_ELIGIBLE_TITLE.getKeyName(),
                "TI_FASTFORWARDED_NOT_ELIGIBLE_TITLE")
            .put(
                MessageKey.ALERT_ELIGIBILITY_TI_FASTFORWARDED_NOT_ELIGIBLE_TEXT.getKeyName(),
                "TI_FASTFORWARDED_NOT_ELIGIBLE_TEXT")
            .put(MessageKey.ALERT_ELIGIBILITY_TI_ELIGIBLE_TITLE.getKeyName(), "TI_ELIGIBLE_TITLE")
            .put(MessageKey.ALERT_ELIGIBILITY_TI_ELIGIBLE_TEXT.getKeyName(), "TI_ELIGIBLE_TEXT")
            .put(
                MessageKey.ALERT_ELIGIBILITY_TI_NOT_ELIGIBLE_TITLE.getKeyName(),
                "TI_NOT_ELIGIBLE_TITLE")
            .put(
                MessageKey.ALERT_ELIGIBILITY_TI_NOT_ELIGIBLE_TEXT.getKeyName(),
                "TI_NOT_ELIGIBLE_TEXT")
            .put(
                MessageKey.ALERT_ELIGIBILITY_TI_NOT_ELIGIBLE_TEXT_SHORT.getKeyName(),
                "TI_NOT_ELIGIBLE_TEXT_SHORT")
            .build();

    Map<String, Map<String, String>> langMap =
        Collections.singletonMap(Lang.defaultLang().code(), messagesMap);

    return play.test.Helpers.stubMessagesApi(langMap, langs);
  }

  private ProgramDefinition createProgramDefinition(boolean isEligibilityGating) {
    return ProgramDefinition.builder()
        .setId(1L)
        .setAdminName("")
        .setAdminDescription("")
        .setExternalLink("")
        .setDisplayMode(DisplayMode.PUBLIC)
        .setProgramType(ProgramType.DEFAULT)
        .setEligibilityIsGating(isEligibilityGating)
        .setAcls(new ProgramAcls())
        .setCategories(ImmutableList.of())
        .build();
  }

  private Http.Request createFakeRequest(boolean isFastForwarded) {
    if (isFastForwarded) {
      return fakeRequestBuilder().flash(FlashKey.SHOW_FAST_FORWARDED_MESSAGE, "true").build();
    }

    return fakeRequest();
  }

  private record ParamValue(
      boolean isTi,
      boolean isFastForwarded,
      boolean isApplicationEligible,
      boolean isNorthStarEnabled,
      boolean pageHasSupplementalInformation,
      AlertType expectedAlertType,
      String expectedTitle,
      String expectedText) {}

  public static ImmutableList<ParamValue> getTestData() {
    return ImmutableList.of(
        // Applicant
        new ParamValue(
            false,
            true,
            true,
            false,
            false,
            AlertType.SUCCESS,
            "APPLICANT_FASTFORWARDED_ELIGIBLE_TITLE",
            "APPLICANT_FASTFORWARDED_ELIGIBLE_TEXT"),
        new ParamValue(
            false,
            true,
            false,
            false,
            false,
            AlertType.WARNING,
            "APPLICANT_FASTFORWARDED_NOT_ELIGIBLE_TITLE",
            "APPLICANT_FASTFORWARDED_NOT_ELIGIBLE_TEXT"),
        new ParamValue(
            false,
            false,
            true,
            false,
            false,
            AlertType.SUCCESS,
            "APPLICANT_ELIGIBLE_TITLE",
            "APPLICANT_ELIGIBLE_TEXT"),
        new ParamValue(
            false,
            false,
            false,
            false,
            false,
            AlertType.WARNING,
            "APPLICANT_NOT_ELIGIBLE_TITLE",
            "APPLICANT_NOT_ELIGIBLE_TEXT"),

        // TI
        new ParamValue(
            true,
            true,
            true,
            false,
            false,
            AlertType.SUCCESS,
            "TI_FASTFORWARDED_ELIGIBLE_TITLE",
            "TI_FASTFORWARDED_ELIGIBLE_TEXT"),
        new ParamValue(
            true,
            true,
            false,
            false,
            false,
            AlertType.WARNING,
            "TI_FASTFORWARDED_NOT_ELIGIBLE_TITLE",
            "TI_FASTFORWARDED_NOT_ELIGIBLE_TEXT"),
        new ParamValue(
            true,
            false,
            true,
            false,
            false,
            AlertType.SUCCESS,
            "TI_ELIGIBLE_TITLE",
            "TI_ELIGIBLE_TEXT"),
        new ParamValue(
            true,
            false,
            false,
            false,
            false,
            AlertType.WARNING,
            "TI_NOT_ELIGIBLE_TITLE",
            "TI_NOT_ELIGIBLE_TEXT"),

        // North Star, pageHasSupplementalInformation==false
        new ParamValue(
            false,
            false,
            false,
            true,
            false,
            AlertType.WARNING,
            "APPLICANT_NOT_ELIGIBLE_TITLE",
            "APPLICANT_NOT_ELIGIBLE_TEXT"),
        new ParamValue(
            true,
            false,
            false,
            true,
            false,
            AlertType.WARNING,
            "TI_NOT_ELIGIBLE_TITLE",
            "TI_NOT_ELIGIBLE_TEXT"),

        // North Star, , pageHasSupplementalInformation==true
        new ParamValue(
            false,
            false,
            false,
            true,
            true,
            AlertType.WARNING,
            "APPLICANT_NOT_ELIGIBLE_TITLE",
            "APPLICANT_NOT_ELIGIBLE_TEXT_SHORT"),
        new ParamValue(
            true,
            false,
            false,
            true,
            true,
            AlertType.WARNING,
            "TI_NOT_ELIGIBLE_TITLE",
            "TI_NOT_ELIGIBLE_TEXT_SHORT"));
  }

  @Test
  @Parameters(method = "getTestData")
  public void build_expected_eligibility_alert_settings_when_gating_is_on(ParamValue value)
      throws ProgramNotFoundException {
    boolean isEligibilityGating = true;

    MessagesApi messagesApiMock = getMessagesApiMock();
    ProgramService programServiceMock = mock(ProgramService.class);
    when(programServiceMock.getFullProgramDefinition(any(Long.class)))
        .thenReturn(createProgramDefinition(isEligibilityGating));

    EligibilityAlertSettingsCalculator eligibilityAlertSettingsCalculator =
        new EligibilityAlertSettingsCalculator(programServiceMock, messagesApiMock);

    Http.Request request = createFakeRequest(value.isFastForwarded);

    AlertSettings result =
        eligibilityAlertSettingsCalculator.calculate(
            request,
            value.isTi,
            value.isApplicationEligible,
            value.isNorthStarEnabled, /* programId */
            value.pageHasSupplementalInformation,
            1L,
            ImmutableList.of());

    assertThat(result.show()).isEqualTo(isEligibilityGating);
    assertThat(result.alertType()).isEqualTo(value.expectedAlertType);
    assertThat(result.title().get()).isEqualTo(value.expectedTitle);
    assertThat(result.text()).isEqualTo(value.expectedText);
  }

  @Test
  public void formats_questions() throws ProgramNotFoundException {
    boolean isEligibilityGating = true;

    MessagesApi messagesApiMock = getMessagesApiMock();
    ProgramService programServiceMock = mock(ProgramService.class);
    when(programServiceMock.getFullProgramDefinition(any(Long.class)))
        .thenReturn(createProgramDefinition(isEligibilityGating));

    EligibilityAlertSettingsCalculator eligibilityAlertSettingsCalculator =
        new EligibilityAlertSettingsCalculator(programServiceMock, messagesApiMock);

    Http.Request request = createFakeRequest(false);

    ApplicantQuestion question = mock(ApplicantQuestion.class);
    when(question.getQuestionText()).thenReturn(QUESTION_TEXT);
    ImmutableList<ApplicantQuestion> questions = ImmutableList.of(question);

    AlertSettings result =
        eligibilityAlertSettingsCalculator.calculate(
            request, false, false, true, true, 1L, questions);

    assertThat(result.additionalText().size()).isEqualTo(1);
    assertThat(result.additionalText().get(0)).isEqualTo(QUESTION_TEXT);
  }

  @Test
  public void build_expected_eligibility_alert_settings_when_gating_is_off()
      throws ProgramNotFoundException {
    boolean isEligibilityGating = false;

    MessagesApi messagesApiMock = getMessagesApiMock();
    ProgramService programServiceMock = mock(ProgramService.class);
    when(programServiceMock.getFullProgramDefinition(any(Long.class)))
        .thenReturn(createProgramDefinition(isEligibilityGating));

    EligibilityAlertSettingsCalculator eligibilityAlertSettingsCalculator =
        new EligibilityAlertSettingsCalculator(programServiceMock, messagesApiMock);

    AlertSettings result =
        eligibilityAlertSettingsCalculator.calculate(
            fakeRequest(), false, true, false, false, /* programId */ 1L, ImmutableList.of());

    assertThat(result.show()).isEqualTo(isEligibilityGating);
  }
}
