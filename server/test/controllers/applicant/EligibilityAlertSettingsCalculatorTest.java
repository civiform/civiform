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
import java.util.Optional;
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
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.program.ProgramType;
import services.program.StatusDefinitions;

@RunWith(JUnitParamsRunner.class)
public class EligibilityAlertSettingsCalculatorTest {
  private MessagesApi getMessagesApiMock() {
    Langs langs = new Langs(new play.api.i18n.DefaultLangs());

    Map<String, String> messagesMap =
        ImmutableMap.<String, String>builder()
            .put(
                MessageKey.ALERT_ELIGIBILITY_APPLICANT_FASTFORWARDED_ELIGIBLE_TITLE.getKeyName(),
                "A11")
            .put(
                MessageKey.ALERT_ELIGIBILITY_APPLICANT_FASTFORWARDED_ELIGIBLE_TEXT.getKeyName(),
                "A12")
            .put(
                MessageKey.ALERT_ELIGIBILITY_APPLICANT_FASTFORWARDED_NOT_ELIGIBLE_TITLE
                    .getKeyName(),
                "A21")
            .put(
                MessageKey.ALERT_ELIGIBILITY_APPLICANT_FASTFORWARDED_NOT_ELIGIBLE_TEXT.getKeyName(),
                "A22")
            .put(MessageKey.ALERT_ELIGIBILITY_APPLICANT_ELIGIBLE_TITLE.getKeyName(), "A31")
            .put(MessageKey.ALERT_ELIGIBILITY_APPLICANT_ELIGIBLE_TEXT.getKeyName(), "A32")
            .put(MessageKey.ALERT_ELIGIBILITY_APPLICANT_NOT_ELIGIBLE_TITLE.getKeyName(), "A41")
            .put(MessageKey.ALERT_ELIGIBILITY_APPLICANT_NOT_ELIGIBLE_TEXT.getKeyName(), "A42")
            .put(MessageKey.ALERT_ELIGIBILITY_TI_FASTFORWARDED_ELIGIBLE_TITLE.getKeyName(), "T11")
            .put(MessageKey.ALERT_ELIGIBILITY_TI_FASTFORWARDED_ELIGIBLE_TEXT.getKeyName(), "T12")
            .put(
                MessageKey.ALERT_ELIGIBILITY_TI_FASTFORWARDED_NOT_ELIGIBLE_TITLE.getKeyName(),
                "T21")
            .put(
                MessageKey.ALERT_ELIGIBILITY_TI_FASTFORWARDED_NOT_ELIGIBLE_TEXT.getKeyName(), "T22")
            .put(MessageKey.ALERT_ELIGIBILITY_TI_ELIGIBLE_TITLE.getKeyName(), "T31")
            .put(MessageKey.ALERT_ELIGIBILITY_TI_ELIGIBLE_TEXT.getKeyName(), "T32")
            .put(MessageKey.ALERT_ELIGIBILITY_TI_NOT_ELIGIBLE_TITLE.getKeyName(), "T41")
            .put(MessageKey.ALERT_ELIGIBILITY_TI_NOT_ELIGIBLE_TEXT.getKeyName(), "T42")
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
        .setStatusDefinitions(new StatusDefinitions())
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
      AlertType expectedAlertType,
      String expectedText) {}

  public static ImmutableList<ParamValue> getTestData() {
    return ImmutableList.of(
        // Applicant
        new ParamValue(false, true, true, AlertType.SUCCESS, "A1"),
        new ParamValue(false, true, false, AlertType.WARNING, "A2"),
        new ParamValue(false, false, true, AlertType.SUCCESS, "A3"),
        new ParamValue(false, false, false, AlertType.WARNING, "A4"),

        // TI
        new ParamValue(true, true, true, AlertType.SUCCESS, "T1"),
        new ParamValue(true, true, false, AlertType.WARNING, "T2"),
        new ParamValue(true, false, true, AlertType.SUCCESS, "T3"),
        new ParamValue(true, false, false, AlertType.WARNING, "T4"));
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
            request, value.isTi, value.isApplicationEligible, /* programId */ 1L);

    assertThat(result.show()).isEqualTo(isEligibilityGating);
    assertThat(result.alertType()).isEqualTo(value.expectedAlertType);
    assertThat(result.title()).isEqualTo(Optional.of(String.format("%s1", value.expectedText)));
    assertThat(result.text()).isEqualTo(String.format("%s2", value.expectedText));
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
            fakeRequest(), false, true, /* programId */ 1L);

    assertThat(result.show()).isEqualTo(isEligibilityGating);
  }
}
