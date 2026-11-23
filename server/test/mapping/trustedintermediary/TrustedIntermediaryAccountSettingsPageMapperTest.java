package mapping.trustedintermediary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import controllers.ti.routes;
import java.util.Optional;
import models.AccountModel;
import models.TrustedIntermediaryGroupModel;
import org.junit.Before;
import org.junit.Test;
import views.trustedintermediary.TrustedIntermediaryAccountSettingsPageViewModel;
import views.trustedintermediary.TrustedIntermediaryAccountSettingsPageViewModel.Member;

public final class TrustedIntermediaryAccountSettingsPageMapperTest {

  private static final String BASE_URL = "https://civiform.example.com";

  private TrustedIntermediaryAccountSettingsPageMapper mapper;
  private TrustedIntermediaryGroupModel tiGroup;

  @Before
  public void setup() {
    mapper = new TrustedIntermediaryAccountSettingsPageMapper();

    tiGroup = mock(TrustedIntermediaryGroupModel.class);
    when(tiGroup.getName()).thenReturn("Food Bank");
    when(tiGroup.getTrustedIntermediaries()).thenReturn(ImmutableList.of());
  }

  private static AccountModel account(
      String displayName, String emailAddress, ImmutableList<Long> ownedApplicantIds) {
    AccountModel account = mock(AccountModel.class);
    when(account.getApplicantDisplayName()).thenReturn(displayName);
    when(account.getEmailAddress()).thenReturn(emailAddress);
    when(account.ownedApplicantIds()).thenReturn(ownedApplicantIds);

    return account;
  }

  private TrustedIntermediaryAccountSettingsPageViewModel mapWithMembers(AccountModel... accounts) {
    when(tiGroup.getTrustedIntermediaries()).thenReturn(ImmutableList.copyOf(accounts));

    return mapper.map(tiGroup, BASE_URL, /* errorMessage= */ Optional.empty());
  }

  private Member onlyMember(AccountModel account) {
    ImmutableList<Member> members = mapWithMembers(account).getMembers();
    assertThat(members).hasSize(1);

    return members.get(0);
  }

  @Test
  public void map_setsTiGroupName() {
    TrustedIntermediaryAccountSettingsPageViewModel model =
        mapper.map(tiGroup, BASE_URL, /* errorMessage= */ Optional.empty());

    assertThat(model.getTiGroupName()).isEqualTo("Food Bank");
  }

  @Test
  public void map_setsClientListUrlWithBaseUrlAndFirstPage() {
    TrustedIntermediaryAccountSettingsPageViewModel model =
        mapper.map(tiGroup, BASE_URL, /* errorMessage= */ Optional.empty());

    assertThat(model.getClientListUrl())
        .isEqualTo(
            BASE_URL
                + routes.TrustedIntermediaryController.dashboard(
                        /* nameQuery= */ Optional.empty(),
                        /* dayQuery= */ Optional.empty(),
                        /* monthQuery= */ Optional.empty(),
                        /* yearQuery= */ Optional.empty(),
                        /* page= */ Optional.of(1))
                    .url());
  }

  @Test
  public void map_setsAccountSettingsUrlWithBaseUrl() {
    TrustedIntermediaryAccountSettingsPageViewModel model =
        mapper.map(tiGroup, BASE_URL, /* errorMessage= */ Optional.empty());

    assertThat(model.getAccountSettingsUrl())
        .isEqualTo(BASE_URL + routes.TrustedIntermediaryController.accountSettings().url());
  }

  @Test
  public void map_withNoMembers_setsEmptyMembers() {
    TrustedIntermediaryAccountSettingsPageViewModel model =
        mapper.map(tiGroup, BASE_URL, /* errorMessage= */ Optional.empty());

    assertThat(model.getMembers()).isEmpty();
  }

  @Test
  public void map_sortsMembersByDisplayName() {
    TrustedIntermediaryAccountSettingsPageViewModel model =
        mapWithMembers(
            account("Carol", "carol@example.com", ImmutableList.of(1L)),
            account("Alice", "alice@example.com", ImmutableList.of(2L)),
            account("Bob", "bob@example.com", ImmutableList.of(3L)));

    assertThat(model.getMembers())
        .extracting(Member::getDisplayName)
        .containsExactly("Alice", "Bob", "Carol");
  }

  @Test
  public void map_setsMemberEmailAddress() {
    Member member = onlyMember(account("Alice", "alice@example.com", ImmutableList.of(1L)));

    assertThat(member.getEmailAddress()).isEqualTo("alice@example.com");
  }

  @Test
  public void map_withNullEmailAddress_setsNullMemberEmailAddress() {
    Member member = onlyMember(account("Alice", /* emailAddress= */ null, ImmutableList.of(1L)));

    assertThat(member.getEmailAddress()).isNull();
  }

  @Test
  public void map_withEmptyEmailAddress_setsNullMemberEmailAddress() {
    Member member = onlyMember(account("Alice", "", ImmutableList.of(1L)));

    assertThat(member.getEmailAddress()).isNull();
  }

  @Test
  public void map_withOwnedApplicant_setsMemberSignedIn() {
    Member member = onlyMember(account("Alice", "alice@example.com", ImmutableList.of(1L)));

    assertThat(member.isSignedIn()).isTrue();
  }

  @Test
  public void map_withNoOwnedApplicants_setsMemberNotSignedIn() {
    Member member = onlyMember(account("Alice", "alice@example.com", ImmutableList.of()));

    assertThat(member.isSignedIn()).isFalse();
  }

  @Test
  public void map_withoutErrorMessage_setsNoErrorToast() {
    TrustedIntermediaryAccountSettingsPageViewModel model =
        mapper.map(tiGroup, BASE_URL, /* errorMessage= */ Optional.empty());

    assertThat(model.getErrorMessage()).isNull();
    assertThat(model.getErrorToastId()).isNull();
  }

  @Test
  public void map_withErrorMessage_setsPrefixedErrorMessage() {
    TrustedIntermediaryAccountSettingsPageViewModel model =
        mapper.map(tiGroup, BASE_URL, Optional.of("Something went wrong"));

    assertThat(model.getErrorMessage()).isEqualTo("Error: Something went wrong");
  }

  @Test
  public void map_withErrorMessage_setsErrorToastId() {
    TrustedIntermediaryAccountSettingsPageViewModel model =
        mapper.map(tiGroup, BASE_URL, Optional.of("Something went wrong"));

    assertThat(model.getErrorToastId()).isNotBlank();
  }
}
