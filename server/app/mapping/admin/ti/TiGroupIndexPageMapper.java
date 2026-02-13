package mapping.admin.ti;

import controllers.FlashKey;
import java.util.List;
import java.util.stream.Collectors;
import models.TrustedIntermediaryGroupModel;
import play.mvc.Http;
import views.admin.ti.TiGroupIndexPageViewModel;
import views.admin.ti.TiGroupIndexPageViewModel.TiGroupRow;

/** Maps TI group data to the TiGroupIndexPageViewModel. */
public final class TiGroupIndexPageMapper {

  public TiGroupIndexPageViewModel map(
      List<TrustedIntermediaryGroupModel> tiGroups, Http.Request request) {
    List<TiGroupRow> groupRows =
        tiGroups.stream()
            .map(
                ti ->
                    TiGroupRow.builder()
                        .id(ti.id)
                        .name(ti.getName())
                        .description(ti.getDescription())
                        .memberCount(ti.getTrustedIntermediaries().size())
                        .clientCount(ti.getManagedAccountsCount())
                        .build())
            .collect(Collectors.toList());

    return TiGroupIndexPageViewModel.builder()
        .providedName(request.flash().get(FlashKey.PROVIDED_NAME).orElse(""))
        .providedDescription(request.flash().get(FlashKey.PROVIDED_DESCRIPTION).orElse(""))
        .errorMessage(request.flash().get(FlashKey.ERROR))
        .groups(groupRows)
        .build();
  }
}
