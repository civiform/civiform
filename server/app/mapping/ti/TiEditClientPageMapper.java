package mapping.ti;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import models.AccountModel;
import models.ApplicantModel;
import models.TrustedIntermediaryGroupModel;
import play.mvc.Http;
import services.DateConverter;
import services.settings.SettingsManifest;
import views.trustedintermediary.TiEditClientPageViewModel;
import views.trustedintermediary.TiEditClientPageViewModel.SuffixOption;

/** Maps TI client data to the TiEditClientPageViewModel. */
public final class TiEditClientPageMapper {

  private final DateConverter dateConverter;
  private final SettingsManifest settingsManifest;

  public TiEditClientPageMapper(DateConverter dateConverter, SettingsManifest settingsManifest) {
    this.dateConverter = dateConverter;
    this.settingsManifest = settingsManifest;
  }

  public TiEditClientPageViewModel map(
      TrustedIntermediaryGroupModel tiGroup,
      boolean isEdit,
      String formActionUrl,
      Optional<ApplicantModel> optionalApplicant,
      Optional<AccountModel> optionalAccount,
      Http.Request request) {
    List<SuffixOption> suffixOptions =
        Stream.of(ApplicantModel.Suffix.values())
            .map(
                s ->
                    SuffixOption.builder()
                        .label(s.getValue().toString())
                        .value(s.toString())
                        .build())
            .collect(Collectors.toList());

    return TiEditClientPageViewModel.builder()
        .tiGroupName(tiGroup.getName())
        .isEdit(isEdit)
        .formActionUrl(formActionUrl)
        .isNameSuffixEnabled(settingsManifest.getNameSuffixDropdownEnabled(request))
        .firstName(optionalApplicant.flatMap(ApplicantModel::getFirstName).orElse(""))
        .middleName(optionalApplicant.flatMap(ApplicantModel::getMiddleName).orElse(""))
        .lastName(optionalApplicant.flatMap(ApplicantModel::getLastName).orElse(""))
        .nameSuffix(optionalApplicant.flatMap(ApplicantModel::getSuffix).orElse(""))
        .phoneNumber(optionalApplicant.flatMap(ApplicantModel::getPhoneNumber).orElse(""))
        .emailAddress(optionalApplicant.flatMap(ApplicantModel::getEmailAddress).orElse(""))
        .dateOfBirth(
            optionalApplicant
                .flatMap(ApplicantModel::getDateOfBirth)
                .map(dateConverter::formatIso8601Date)
                .orElse(""))
        .tiNote(optionalAccount.map(AccountModel::getTiNote).orElse(""))
        .suffixOptions(suffixOptions)
        .build();
  }
}
