package mapping.admin.ti;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import models.AccountModel;
import models.TrustedIntermediaryGroupModel;
import views.admin.ti.EditTrustedIntermediaryGroupPageViewModel;
import views.admin.ti.EditTrustedIntermediaryGroupPageViewModel.Member;

/** Maps a trusted intermediary group to the EditTrustedIntermediaryGroupPageViewModel. */
public final class EditTrustedIntermediaryGroupPageMapper {

  /**
   * Maps the group and the flash state of the last add-member attempt to the view model.
   *
   * @param tiGroup the group being edited
   * @param providedEmailAddress the email address submitted by a failed add, kept sticky in the
   *     form
   * @param errorMessage the flash error of a failed add or remove, shown as an error toast
   */
  public EditTrustedIntermediaryGroupPageViewModel map(
      TrustedIntermediaryGroupModel tiGroup,
      Optional<String> providedEmailAddress,
      Optional<String> errorMessage) {
    ImmutableList<Member> members =
        tiGroup.getTrustedIntermediaries().stream()
            .map(this::buildMember)
            .collect(ImmutableList.toImmutableList());

    return EditTrustedIntermediaryGroupPageViewModel.builder()
        .groupName(tiGroup.getName())
        .groupDescription(tiGroup.getDescription())
        .backUrl(controllers.admin.routes.TrustedIntermediaryManagementController.index().url())
        .addIntermediaryUrl(
            controllers.admin.routes.TrustedIntermediaryManagementController.addIntermediary(
                    tiGroup.id)
                .url())
        .removeIntermediaryUrl(
            controllers.admin.routes.TrustedIntermediaryManagementController.removeIntermediary(
                    tiGroup.id)
                .url())
        .providedEmailAddress(providedEmailAddress.orElse(""))
        // The legacy ToastMessage.errorNonLocalized prefixed the flash text.
        .errorMessage(errorMessage.map(error -> "Error: " + error).orElse(null))
        .members(members)
        .build();
  }

  private Member buildMember(AccountModel account) {
    return Member.builder()
        .displayName(account.getApplicantDisplayName())
        .emailAddress(account.getEmailAddress())
        .signedIn(!account.ownedApplicantIds().isEmpty())
        .accountId(account.id.toString())
        .build();
  }
}
