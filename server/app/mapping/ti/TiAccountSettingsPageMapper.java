package mapping.ti;

import com.google.common.base.Strings;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import models.AccountModel;
import models.TrustedIntermediaryGroupModel;
import views.trustedintermediary.TiAccountSettingsPageViewModel;
import views.trustedintermediary.TiAccountSettingsPageViewModel.OrgMemberRow;

/** Maps TI group data to the TiAccountSettingsPageViewModel. */
public final class TiAccountSettingsPageMapper {

  public TiAccountSettingsPageViewModel map(TrustedIntermediaryGroupModel tiGroup) {
    List<OrgMemberRow> memberRows =
        tiGroup.getTrustedIntermediaries().stream()
            .sorted(Comparator.comparing(AccountModel::getApplicantDisplayName))
            .map(
                ti ->
                    OrgMemberRow.builder()
                        .name(ti.getApplicantDisplayName())
                        .email(
                            Strings.isNullOrEmpty(ti.getEmailAddress())
                                ? "(no email address)"
                                : ti.getEmailAddress())
                        .accountStatus(
                            ti.ownedApplicantIds().isEmpty() ? "Not yet signed in." : "OK")
                        .build())
            .collect(Collectors.toList());

    return TiAccountSettingsPageViewModel.builder()
        .tiGroupName(tiGroup.getName())
        .orgMembers(memberRows)
        .build();
  }
}
