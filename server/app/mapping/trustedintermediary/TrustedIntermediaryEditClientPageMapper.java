package mapping.trustedintermediary;

import controllers.ti.routes;
import forms.TiClientInfoForm;
import java.util.Optional;
import models.AccountModel;
import models.TrustedIntermediaryGroupModel;
import play.data.Form;
import play.i18n.Messages;
import services.DateConverter;
import views.trustedintermediary.TrustedIntermediaryEditClientPageViewModel;

/**
 * Maps a client and the edit client form state to the TrustedIntermediaryEditClientPageViewModel.
 */
public final class TrustedIntermediaryEditClientPageMapper {

  /**
   * Maps the edit client page.
   *
   * @param tiGroup the group the trusted intermediary belongs to
   * @param account the client being edited
   * @param submittedForm the submitted form, empty when the page is first shown
   * @param showNameSuffix show the name suffix select
   * @param baseUrl the URL of the CiviForm deployment, prefixed to the client list links
   * @param dateConverter formats the client's stored date of birth
   * @param messages localizes the validation errors
   */
  public TrustedIntermediaryEditClientPageViewModel map(
      TrustedIntermediaryGroupModel tiGroup,
      AccountModel account,
      Optional<Form<TiClientInfoForm>> submittedForm,
      boolean showNameSuffix,
      String baseUrl,
      DateConverter dateConverter,
      Messages messages) {
    return TrustedIntermediaryEditClientPageViewModel.builder()
        .tiGroupName(tiGroup.getName())
        .clientListUrl(baseUrl + TrustedIntermediaryClientInfoFormMapper.clientListPath())
        .accountSettingsUrl(baseUrl + routes.TrustedIntermediaryController.accountSettings().url())
        .showSuccessAlert(submittedForm.isPresent() && !submittedForm.get().hasErrors())
        .form(
            new TrustedIntermediaryClientInfoFormMapper()
                .map(
                    "edit-ti",
                    routes.TrustedIntermediaryController.editClient(account.id).url(),
                    Optional.of(account),
                    submittedForm,
                    showNameSuffix,
                    dateConverter,
                    messages))
        .build();
  }
}
