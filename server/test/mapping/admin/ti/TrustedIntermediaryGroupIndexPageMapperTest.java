package mapping.admin.ti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import models.TrustedIntermediaryGroupModel;
import org.junit.Before;
import org.junit.Test;
import views.admin.ti.TrustedIntermediaryGroupIndexPageViewModel;
import views.admin.ti.TrustedIntermediaryGroupIndexPageViewModel.Group;
import views.admin.ti.TrustedIntermediaryGroupIndexPageViewModel.SortOption;

public final class TrustedIntermediaryGroupIndexPageMapperTest {

  private TrustedIntermediaryGroupIndexPageMapper mapper;

  @Before
  public void setup() {
    mapper = new TrustedIntermediaryGroupIndexPageMapper();
  }

  private static TrustedIntermediaryGroupModel tiGroup(
      long id, String name, String description, int memberCount, int clientCount) {
    TrustedIntermediaryGroupModel tiGroup = mock(TrustedIntermediaryGroupModel.class);
    tiGroup.id = id;
    when(tiGroup.getName()).thenReturn(name);
    when(tiGroup.getDescription()).thenReturn(description);
    when(tiGroup.getMembersCount()).thenReturn(memberCount);
    when(tiGroup.getManagedAccountsCount()).thenReturn(clientCount);

    return tiGroup;
  }

  private TrustedIntermediaryGroupIndexPageViewModel map(
      List<TrustedIntermediaryGroupModel> tiGroups) {
    return mapper.map(
        tiGroups,
        /* providedName= */ Optional.empty(),
        /* providedDescription= */ Optional.empty(),
        /* errorMessage= */ Optional.empty());
  }

  @Test
  public void map_setsCreateGroupUrl() {
    TrustedIntermediaryGroupIndexPageViewModel result = map(ImmutableList.of());

    assertThat(result.getCreateGroupUrl())
        .isEqualTo(controllers.admin.routes.TrustedIntermediaryManagementController.create().url());
  }

  @Test
  public void map_noProvidedValues_setsEmptyNameAndDescription() {
    TrustedIntermediaryGroupIndexPageViewModel result = map(ImmutableList.of());

    assertThat(result.getProvidedName()).isEmpty();
    assertThat(result.getProvidedDescription()).isEmpty();
  }

  @Test
  public void map_providedValues_keepsNameAndDescription() {
    TrustedIntermediaryGroupIndexPageViewModel result =
        mapper.map(
            ImmutableList.of(),
            /* providedName= */ Optional.of("Food Bank"),
            /* providedDescription= */ Optional.of("Helps with food applications"),
            /* errorMessage= */ Optional.empty());

    assertThat(result.getProvidedName()).isEqualTo("Food Bank");
    assertThat(result.getProvidedDescription()).isEqualTo("Helps with food applications");
  }

  @Test
  public void map_noError_setsNullErrorMessage() {
    TrustedIntermediaryGroupIndexPageViewModel result = map(ImmutableList.of());

    assertThat(result.getErrorMessage()).isNull();
  }

  @Test
  public void map_error_prefixesErrorMessage() {
    TrustedIntermediaryGroupIndexPageViewModel result =
        mapper.map(
            ImmutableList.of(),
            /* providedName= */ Optional.empty(),
            /* providedDescription= */ Optional.empty(),
            /* errorMessage= */ Optional.of("Must provide group name."));

    assertThat(result.getErrorMessage()).isEqualTo("Error: Must provide group name.");
  }

  @Test
  public void map_setsSortOptionsInOrder() {
    TrustedIntermediaryGroupIndexPageViewModel result = map(ImmutableList.of());

    assertThat(result.getSortOptions())
        .extracting(SortOption::getValue)
        .containsExactly("tiname-asc", "tiname-desc", "nummember-asc", "nummember-desc");
    assertThat(result.getSortOptions())
        .extracting(SortOption::getLabel)
        .containsExactly("Name A-Z", "Name Z-A", "Fewest members", "Most members");
  }

  @Test
  public void map_selectsOnlyNameAscendingSortOption() {
    TrustedIntermediaryGroupIndexPageViewModel result = map(ImmutableList.of());

    assertThat(result.getSortOptions())
        .extracting(SortOption::isSelected)
        .containsExactly(true, false, false, false);
  }

  @Test
  public void map_noGroups_setsEmptyGroups() {
    TrustedIntermediaryGroupIndexPageViewModel result = map(ImmutableList.of());

    assertThat(result.getGroups()).isEmpty();
  }

  @Test
  public void map_setsGroupDetails() {
    TrustedIntermediaryGroupModel foodBank =
        tiGroup(5L, "Food Bank", "Helps with food applications", 3, 7);

    Group result = map(ImmutableList.of(foodBank)).getGroups().get(0);

    assertThat(result.getName()).isEqualTo("Food Bank");
    assertThat(result.getDescription()).isEqualTo("Helps with food applications");
    assertThat(result.getMemberCount()).isEqualTo(3);
    assertThat(result.getClientCount()).isEqualTo(7);
  }

  @Test
  public void map_setsGroupUrls() {
    TrustedIntermediaryGroupModel foodBank =
        tiGroup(5L, "Food Bank", "Helps with food applications", 3, 7);

    Group result = map(ImmutableList.of(foodBank)).getGroups().get(0);

    assertThat(result.getEditUrl())
        .isEqualTo(controllers.admin.routes.TrustedIntermediaryManagementController.edit(5L).url());
    assertThat(result.getDeleteUrl())
        .isEqualTo(
            controllers.admin.routes.TrustedIntermediaryManagementController.delete(5L).url());
  }

  @Test
  public void map_keepsGroupOrder() {
    TrustedIntermediaryGroupModel foodBank = tiGroup(5L, "Food Bank", "Food", 3, 7);
    TrustedIntermediaryGroupModel clinic = tiGroup(6L, "Clinic", "Health", 0, 0);

    TrustedIntermediaryGroupIndexPageViewModel result = map(ImmutableList.of(foodBank, clinic));

    assertThat(result.getGroups())
        .extracting(Group::getName)
        .containsExactly("Food Bank", "Clinic");
  }
}
