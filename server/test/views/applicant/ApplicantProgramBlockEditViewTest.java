package views.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static views.questiontypes.ApplicantQuestionRendererParams.AutoFocusTarget.FIRST_ERROR;
import static views.questiontypes.ApplicantQuestionRendererParams.AutoFocusTarget.FIRST_FIELD;
import static views.questiontypes.ApplicantQuestionRendererParams.AutoFocusTarget.NONE;

import controllers.applicant.ApplicantRoutes;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import repository.ResetPostgres;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;

public class ApplicantProgramBlockEditViewTest extends ResetPostgres {

  private static QuestionDefinition ADDRESS_QD =
      testQuestionBank.applicantAddress().getQuestionDefinition();
  private static SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
  private static ApplicantRoutes applicantRoutes = new ApplicantRoutes(mockSettingsManifest);

  private static ApplicantProgramBlockEditView EMPTY_VIEW =
      new ApplicantProgramBlockEditView(
          Mockito.mock(ApplicantLayout.class),
          Mockito.mock(ApplicantFileUploadRenderer.class),
          Mockito.mock(ApplicantQuestionRendererFactory.class),
          applicantRoutes);

  @BeforeClass
  public static void setupMock() {
    when(mockSettingsManifest.getNewApplicantUrlSchemaEnabled()).thenReturn(true);
  }

  @Test
  public void
      calculateAutoFocusTarget_formHasErrors_displayErrors_isFirstQuestionWithErrors_shouldAutofocusFirstError() {
    assertThat(
            EMPTY_VIEW.calculateAutoFocusTarget(
                ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS,
                ADDRESS_QD,
                /* formHasErrors */ true,
                /* ordinalErrorCount= */ 1,
                /* applicantSelectedQuestionName= */ Optional.empty()))
        .isEqualTo(FIRST_ERROR);
  }

  @Test
  public void
      calculateAutoFocusTarget_formHasErrors_hideErrors_isFirstQuestionWithErrors_shouldNotAutofocus() {
    assertThat(
            EMPTY_VIEW.calculateAutoFocusTarget(
                ApplicantQuestionRendererParams.ErrorDisplayMode.HIDE_ERRORS,
                ADDRESS_QD,
                /* formHasErrors */ true,
                /* ordinalErrorCount= */ 1,
                /* applicantSelectedQuestionName= */ Optional.empty()))
        .isEqualTo(NONE);
  }

  @Test
  public void
      calculateAutoFocusTarget_formHasErrors_displayErrors_isSecondQuestionWithErrors_shouldNotAutofocus() {
    assertThat(
            EMPTY_VIEW.calculateAutoFocusTarget(
                ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS,
                ADDRESS_QD,
                /* formHasErrors */ true,
                /* ordinalErrorCount= */ 2,
                /* applicantSelectedQuestionName= */ Optional.empty()))
        .isEqualTo(NONE);
  }

  @Test
  public void
      calculateAutoFocusTarget_noErrors_displayErrors_noApplicantSelectedQuestionName_shouldNotAutofocus() {
    assertThat(
            EMPTY_VIEW.calculateAutoFocusTarget(
                ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS,
                ADDRESS_QD,
                /* formHasErrors */ false,
                /* ordinalErrorCount= */ 0,
                /* applicantSelectedQuestionName= */ Optional.empty()))
        .isEqualTo(NONE);
  }

  @Test
  public void
      calculateAutoFocusTarget_noErrors_hasApplicantSelectedQuestionName_questionSelected_shouldAutofocusFirstField() {
    assertThat(
            EMPTY_VIEW.calculateAutoFocusTarget(
                ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS,
                ADDRESS_QD,
                /* formHasErrors */ false,
                /* ordinalErrorCount= */ 0,
                /* applicantSelectedQuestionName= */ Optional.of(ADDRESS_QD.getName())))
        .isEqualTo(FIRST_FIELD);
  }

  @Test
  public void
      calculateAutoFocusTarget_hasErrors_hideErrors_hasApplicantSelectedQuestionName_questionSelected_shouldAutofocusFirstField() {
    assertThat(
            EMPTY_VIEW.calculateAutoFocusTarget(
                ApplicantQuestionRendererParams.ErrorDisplayMode.HIDE_ERRORS,
                ADDRESS_QD,
                /* formHasErrors */ true,
                /* ordinalErrorCount= */ 1,
                /* applicantSelectedQuestionName= */ Optional.of(ADDRESS_QD.getName())))
        .isEqualTo(FIRST_FIELD);
  }

  @Test
  public void
      calculateAutoFocusTarget_noErrors_hasApplicantSelectedQuestionName_differentQuestionSelected_shouldNotAutofocus() {
    assertThat(
            EMPTY_VIEW.calculateAutoFocusTarget(
                ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS,
                ADDRESS_QD,
                /* formHasErrors */ false,
                /* ordinalErrorCount= */ 0,
                /* applicantSelectedQuestionName= */ Optional.of("other question")))
        .isEqualTo(NONE);
  }
}
