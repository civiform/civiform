package mapping.admin.ti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import models.AccountModel;
import models.TrustedIntermediaryGroupModel;
import org.junit.Before;
import org.junit.Test;
import views.admin.ti.EditTrustedIntermediaryGroupPageViewModel;
import views.admin.ti.EditTrustedIntermediaryGroupPageViewModel.Member;

public final class EditTrustedIntermediaryGroupPageMapperTest {

  private EditTrustedIntermediaryGroupPageMapper mapper;
  private TrustedIntermediaryGroupModel tiGroup;

  @Before
  public void setup() {
    mapper = new EditTrustedIntermediaryGroupPageMapper();

    tiGroup = mock(TrustedIntermediaryGroupModel.class);
    tiGroup.id = 5L;
    when(tiGroup.getName()).thenReturn("Food Bank");
    when(tiGroup.getDescription()).thenReturn("Helps with food applications");
    when(tiGroup.getTrustedIntermediaries()).thenReturn(ImmutableList.of());
  }

  private static AccountModel account(
      long id, String displayName, String email, ImmutableList<Long> ownedApplicantIds) {
    AccountModel account = mock(AccountModel.class);
    account.id = id;
    when(account.getApplicantDisplayName()).thenReturn(displayName);
    when(account.getEmailAddress()).thenReturn(email);
    when(account.ownedApplicantIds()).thenReturn(ownedApplicantIds);

    return account;
  }

  private EditTrustedIntermediaryGroupPageViewModel map() {
    return mapper.map(
        tiGroup,
        /* providedEmailAddress= */ Optional.empty(),
        /* errorMessage= */ Optional.empty());
  }

  @Test
  public void map_setsGroupNameAndDescription() {
    EditTrustedIntermediaryGroupPageViewModel result = map();

    assertThat(result.getGroupName()).isEqualTo("Food Bank");
    assertThat(result.getGroupDescription()).isEqualTo("Helps with food applications");
  }

  @Test
  public void map_setsUrls() {
    EditTrustedIntermediaryGroupPageViewModel result = map();

    assertThat(result.getBackUrl()).isEqualTo("/admin/tiGroups");
    assertThat(result.getAddIntermediaryUrl()).isEqualTo("/admin/tiGroups/5/addTi");
    assertThat(result.getRemoveIntermediaryUrl()).isEqualTo("/admin/tiGroups/5/removeTi");
  }

  @Test
  public void map_noProvidedEmail_setsEmptyEmailAddress() {
    EditTrustedIntermediaryGroupPageViewModel result = map();

    assertThat(result.getProvidedEmailAddress()).isEmpty();
  }

  @Test
  public void map_providedEmail_keepsEmailAddress() {
    EditTrustedIntermediaryGroupPageViewModel result =
        mapper.map(
            tiGroup,
            /* providedEmailAddress= */ Optional.of("jane@example.com"),
            /* errorMessage= */ Optional.empty());

    assertThat(result.getProvidedEmailAddress()).isEqualTo("jane@example.com");
  }

  @Test
  public void map_noError_setsNullErrorMessage() {
    EditTrustedIntermediaryGroupPageViewModel result = map();

    assertThat(result.getErrorMessage()).isNull();
  }

  @Test
  public void map_error_prefixesErrorMessage() {
    EditTrustedIntermediaryGroupPageViewModel result =
        mapper.map(
            tiGroup,
            /* providedEmailAddress= */ Optional.empty(),
            /* errorMessage= */ Optional.of("Must provide email address."));

    assertThat(result.getErrorMessage()).isEqualTo("Error: Must provide email address.");
  }

  @Test
  public void map_noMembers_setsEmptyMembers() {
    EditTrustedIntermediaryGroupPageViewModel result = map();

    assertThat(result.getMembers()).isEmpty();
  }

  @Test
  public void map_setsMemberDetails() {
    AccountModel jane = account(12L, "Jane Doe", "jane@example.com", ImmutableList.of(3L));
    when(tiGroup.getTrustedIntermediaries()).thenReturn(ImmutableList.of(jane));

    Member result = map().getMembers().get(0);

    assertThat(result.getDisplayName()).isEqualTo("Jane Doe");
    assertThat(result.getEmailAddress()).isEqualTo("jane@example.com");
    assertThat(result.getAccountId()).isEqualTo("12");
  }

  @Test
  public void map_memberWithApplicant_isSignedIn() {
    AccountModel jane = account(12L, "Jane Doe", "jane@example.com", ImmutableList.of(3L));
    when(tiGroup.getTrustedIntermediaries()).thenReturn(ImmutableList.of(jane));

    Member result = map().getMembers().get(0);

    assertThat(result.isSignedIn()).isTrue();
  }

  @Test
  public void map_memberWithoutApplicant_isNotSignedIn() {
    AccountModel newcomer = account(13L, "<Unnamed User>", "new@example.com", ImmutableList.of());
    when(tiGroup.getTrustedIntermediaries()).thenReturn(ImmutableList.of(newcomer));

    Member result = map().getMembers().get(0);

    assertThat(result.isSignedIn()).isFalse();
  }

  @Test
  public void map_keepsMemberOrder() {
    AccountModel jane = account(12L, "Jane Doe", "jane@example.com", ImmutableList.of(3L));
    AccountModel newcomer = account(13L, "<Unnamed User>", "new@example.com", ImmutableList.of());
    when(tiGroup.getTrustedIntermediaries()).thenReturn(ImmutableList.of(jane, newcomer));

    EditTrustedIntermediaryGroupPageViewModel result = map();

    assertThat(result.getMembers()).extracting(Member::getAccountId).containsExactly("12", "13");
  }
}
