package mapping.admin.ti;

import controllers.FlashKey;
import java.util.List;
import java.util.stream.Collectors;
import models.TrustedIntermediaryGroupModel;
import play.mvc.Http;
import views.admin.ti.TiGroupEditPageViewModel;
import views.admin.ti.TiGroupEditPageViewModel.TiMemberRow;

/** Maps TI group data to the TiGroupEditPageViewModel. */
public final class TiGroupEditPageMapper {

  public TiGroupEditPageViewModel map(TrustedIntermediaryGroupModel tiGroup, Http.Request request) {
    List<TiMemberRow> memberRows =
        tiGroup.getTrustedIntermediaries().stream()
            .map(
                account ->
                    TiMemberRow.builder()
                        .displayName(account.getApplicantDisplayName())
                        .emailAddress(account.getEmailAddress())
                        .status(account.ownedApplicantIds().isEmpty() ? "Not yet signed in." : "OK")
                        .accountId(account.id)
                        .tiGroupId(tiGroup.id)
                        .build())
            .collect(Collectors.toList());

    return TiGroupEditPageViewModel.builder()
        .groupName(tiGroup.getName())
        .groupDescription(tiGroup.getDescription())
        .tiGroupId(tiGroup.id)
        .providedEmailAddress(request.flash().get(FlashKey.PROVIDED_EMAIL_ADDRESS).orElse(""))
        .errorMessage(request.flash().get(FlashKey.ERROR))
        .members(memberRows)
        .build();
  }
}
