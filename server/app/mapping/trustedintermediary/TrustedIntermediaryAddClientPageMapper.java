package mapping.trustedintermediary;

import com.google.common.collect.ImmutableList;
import controllers.ti.routes;
import forms.TiClientInfoForm;
import java.util.Optional;
import models.TrustedIntermediaryGroupModel;
import play.data.Form;
import play.i18n.Messages;
import services.DateConverter;
import views.trustedintermediary.TrustedIntermediaryAddClientPageViewModel;

/** Maps the add client form state to the TrustedIntermediaryAddClientPageViewModel. */
public final class TrustedIntermediaryAddClientPageMapper {

  /**
   * Maps the add client page.
   *
   * @param tiGroup the group the trusted intermediary belongs to
   * @param submittedForm the submitted form, empty when the page is first shown
   * @param newClientApplicantId the applicant created for the saved client, empty until saved
   * @param showNameSuffix show the name suffix select
   * @param baseUrl the URL of the CiviForm deployment, prefixed to the client list links
   * @param dateConverter passed to the shared form mapper
   * @param messages localizes the validation errors
   */
  public TrustedIntermediaryAddClientPageViewModel map(
      TrustedIntermediaryGroupModel tiGroup,
      Optional<Form<TiClientInfoForm>> submittedForm,
      Optional<Long> newClientApplicantId,
      boolean showNameSuffix,
      String baseUrl,
      DateConverter dateConverter,
      Messages messages) {
    boolean isSuccessfulSave = submittedForm.isPresent() && !submittedForm.get().hasErrors();

    // The legacy view named the new client by the submitted first and last name.
    String newClientName =
        isSuccessfulSave
            ? submittedForm.get().value().get().getFirstName()
                + " "
                + submittedForm.get().value().get().getLastName()
            : null;

    String startApplicationUrl =
        isSuccessfulSave
            ? newClientApplicantId
                .map(
                    applicantId ->
                        controllers.applicant.routes.ApplicantProgramsController
                            .indexWithApplicantId(applicantId, /* categories= */ ImmutableList.of())
                            .url())
                .orElse(null)
            : null;

    return TrustedIntermediaryAddClientPageViewModel.builder()
        .tiGroupName(tiGroup.getName())
        .clientListUrl(baseUrl + TrustedIntermediaryClientInfoFormMapper.clientListPath())
        .accountSettingsUrl(baseUrl + routes.TrustedIntermediaryController.accountSettings().url())
        .showSuccessAlert(isSuccessfulSave)
        .newClientName(newClientName)
        .startApplicationUrl(startApplicationUrl)
        .form(
            new TrustedIntermediaryClientInfoFormMapper()
                .map(
                    "add-ti",
                    routes.TrustedIntermediaryController.addClient(tiGroup.id).url(),
                    /* account= */ Optional.empty(),
                    submittedForm,
                    showNameSuffix,
                    dateConverter,
                    messages))
        .build();
  }
}
