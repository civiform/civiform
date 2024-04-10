package views.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static views.questiontypes.ApplicantQuestionRendererParams.AutoFocusTarget.FIRST_ERROR;
import static views.questiontypes.ApplicantQuestionRendererParams.AutoFocusTarget.FIRST_FIELD;
import static views.questiontypes.ApplicantQuestionRendererParams.AutoFocusTarget.NONE;

import controllers.applicant.ApplicantRoutes;
import java.util.Optional;
import org.junit.Test;
import org.mockito.Mockito;
import repository.ResetPostgres;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;

public class NorthStarApplicantProgramBlockEditViewTest extends ResetPostgres {

  @Test
  public void
      calculateAutoFocusTarget_formHasErrors_displayErrors_isFirstQuestionWithErrors_shouldAutofocusFirstError() {
    assertThat(
        NorthStarApplicantProgramBlockEditView.calculateAutoFocusTarget(
                ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS,
                /* formHasErrors */ true,
                /* ordinalErrorCount= */ 1))
        .isEqualTo(FIRST_ERROR);
  }

  @Test
  public void
      calculateAutoFocusTarget_formHasErrors_displayErrors_isSecondQuestionWithErrors_shouldNotAutofocus() {
    assertThat(
        NorthStarApplicantProgramBlockEditView.calculateAutoFocusTarget(
                ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS,
                /* formHasErrors */ true,
                /* ordinalErrorCount= */ 2))
        .isEqualTo(NONE);
  }

  @Test
  public void
      calculateAutoFocusTarget_formHasErrors_hideErrors_isFirstQuestionWithErrors_shouldNotAutofocus() {
    assertThat(
        NorthStarApplicantProgramBlockEditView.calculateAutoFocusTarget(
                ApplicantQuestionRendererParams.ErrorDisplayMode.HIDE_ERRORS,
                /* formHasErrors */ true,
                /* ordinalErrorCount= */ 1))
        .isEqualTo(NONE);
  }

  @Test
  public void
      calculateAutoFocusTarget_formHasNoErrors_shouldNotAutofocus() {
    assertThat(
        NorthStarApplicantProgramBlockEditView.calculateAutoFocusTarget(
                ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS,
                /* formHasErrors */ false,
                /* ordinalErrorCount= */ 1))
        .isEqualTo(NONE);
  }
}
