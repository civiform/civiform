package mapping.trustedintermediary;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import controllers.ti.routes;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import models.AccountModel;
import models.TrustedIntermediaryGroupModel;
import views.trustedintermediary.TrustedIntermediaryAccountSettingsPageViewModel;
import views.trustedintermediary.TrustedIntermediaryAccountSettingsPageViewModel.Member;

/** Maps a trusted intermediary group to the TrustedIntermediaryAccountSettingsPageViewModel. */
public final class TrustedIntermediaryAccountSettingsPageMapper {

  /**
   * Maps the trusted intermediary group and its members to the view model.
   *
   * @param tiGroup the group the trusted intermediary belongs to
   * @param baseUrl the URL of the CiviForm deployment, prefixed to the tab links
   * @param errorMessage the flash error, shown as an error toast
   */
  public TrustedIntermediaryAccountSettingsPageViewModel map(
      TrustedIntermediaryGroupModel tiGroup, String baseUrl, Optional<String> errorMessage) {
    ImmutableList<Member> members =
        tiGroup.getTrustedIntermediaries().stream()
            .sorted(Comparator.comparing(AccountModel::getApplicantDisplayName))
            .map(TrustedIntermediaryAccountSettingsPageMapper::buildMember)
            .collect(ImmutableList.toImmutableList());

    return TrustedIntermediaryAccountSettingsPageViewModel.builder()
        .tiGroupName(tiGroup.getName())
        .clientListUrl(
            baseUrl
                + routes.TrustedIntermediaryController.dashboard(
                        /* nameQuery= */ Optional.empty(),
                        /* dayQuery= */ Optional.empty(),
                        /* monthQuery= */ Optional.empty(),
                        /* yearQuery= */ Optional.empty(),
                        /* page= */ Optional.of(1))
                    .url())
        .accountSettingsUrl(baseUrl + routes.TrustedIntermediaryController.accountSettings().url())
        .members(members)
        // The legacy ToastMessage.errorNonLocalized prefixed the flash text and gave each toast a
        // random id.
        .errorToastId(errorMessage.map(error -> UUID.randomUUID().toString()).orElse(null))
        .errorMessage(errorMessage.map(error -> "Error: " + error).orElse(null))
        .build();
  }

  private static Member buildMember(AccountModel account) {
    String emailAddress = account.getEmailAddress();

    return Member.builder()
        .displayName(account.getApplicantDisplayName())
        .emailAddress(Strings.isNullOrEmpty(emailAddress) ? null : emailAddress)
        // An account owns no applicant until its first sign in.
        .signedIn(!account.ownedApplicantIds().isEmpty())
        .build();
  }
}
