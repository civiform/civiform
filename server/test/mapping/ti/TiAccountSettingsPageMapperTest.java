package mapping.ti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import models.AccountModel;
import models.TrustedIntermediaryGroupModel;
import org.junit.Before;
import org.junit.Test;
import views.trustedintermediary.TiAccountSettingsPageViewModel;

public final class TiAccountSettingsPageMapperTest {

  private TiAccountSettingsPageMapper mapper;

  @Before
  public void setup() {
    mapper = new TiAccountSettingsPageMapper();
  }

  @Test
  public void map_setsGroupName() {
    TrustedIntermediaryGroupModel tiGroup = mock(TrustedIntermediaryGroupModel.class);
    when(tiGroup.getName()).thenReturn("Test TI Org");
    when(tiGroup.getTrustedIntermediaries()).thenReturn(ImmutableList.of());

    TiAccountSettingsPageViewModel result = mapper.map(tiGroup);

    assertThat(result.getTiGroupName()).isEqualTo("Test TI Org");
  }

  @Test
  public void map_setsAccountSettingsUrl() {
    TrustedIntermediaryGroupModel tiGroup = mock(TrustedIntermediaryGroupModel.class);
    when(tiGroup.getName()).thenReturn("Org");
    when(tiGroup.getTrustedIntermediaries()).thenReturn(ImmutableList.of());

    TiAccountSettingsPageViewModel result = mapper.map(tiGroup);

    assertThat(result.getAccountSettingsUrl()).isNotEmpty();
  }

  @Test
  public void map_noMembers_returnsEmptyList() {
    TrustedIntermediaryGroupModel tiGroup = mock(TrustedIntermediaryGroupModel.class);
    when(tiGroup.getName()).thenReturn("Org");
    when(tiGroup.getTrustedIntermediaries()).thenReturn(ImmutableList.of());

    TiAccountSettingsPageViewModel result = mapper.map(tiGroup);

    assertThat(result.getOrgMembers()).isEmpty();
  }

  @Test
  public void map_withMembers_buildsMemberRows() {
    TrustedIntermediaryGroupModel tiGroup = mock(TrustedIntermediaryGroupModel.class);
    when(tiGroup.getName()).thenReturn("Org");

    AccountModel member = mock(AccountModel.class);
    when(member.getApplicantDisplayName()).thenReturn("Alice");
    when(member.getEmailAddress()).thenReturn("alice@example.com");
    when(member.ownedApplicantIds()).thenReturn(ImmutableList.of(1L));
    when(tiGroup.getTrustedIntermediaries()).thenReturn(ImmutableList.of(member));

    TiAccountSettingsPageViewModel result = mapper.map(tiGroup);

    assertThat(result.getOrgMembers()).hasSize(1);
    TiAccountSettingsPageViewModel.OrgMemberRow row = result.getOrgMembers().get(0);
    assertThat(row.getName()).isEqualTo("Alice");
    assertThat(row.getEmail()).isEqualTo("alice@example.com");
    assertThat(row.getAccountStatus()).isEqualTo("OK");
  }

  @Test
  public void map_memberNotSignedIn_setsNotYetSignedInStatus() {
    TrustedIntermediaryGroupModel tiGroup = mock(TrustedIntermediaryGroupModel.class);
    when(tiGroup.getName()).thenReturn("Org");

    AccountModel member = mock(AccountModel.class);
    when(member.getApplicantDisplayName()).thenReturn("Bob");
    when(member.getEmailAddress()).thenReturn("bob@example.com");
    when(member.ownedApplicantIds()).thenReturn(ImmutableList.of());
    when(tiGroup.getTrustedIntermediaries()).thenReturn(ImmutableList.of(member));

    TiAccountSettingsPageViewModel result = mapper.map(tiGroup);

    assertThat(result.getOrgMembers().get(0).getAccountStatus()).isEqualTo("Not yet signed in.");
  }

  @Test
  public void map_memberWithNullEmail_showsPlaceholder() {
    TrustedIntermediaryGroupModel tiGroup = mock(TrustedIntermediaryGroupModel.class);
    when(tiGroup.getName()).thenReturn("Org");

    AccountModel member = mock(AccountModel.class);
    when(member.getApplicantDisplayName()).thenReturn("Charlie");
    when(member.getEmailAddress()).thenReturn(null);
    when(member.ownedApplicantIds()).thenReturn(ImmutableList.of());
    when(tiGroup.getTrustedIntermediaries()).thenReturn(ImmutableList.of(member));

    TiAccountSettingsPageViewModel result = mapper.map(tiGroup);

    assertThat(result.getOrgMembers().get(0).getEmail()).isEqualTo("(no email address)");
  }

  @Test
  public void map_memberWithEmptyEmail_showsPlaceholder() {
    TrustedIntermediaryGroupModel tiGroup = mock(TrustedIntermediaryGroupModel.class);
    when(tiGroup.getName()).thenReturn("Org");

    AccountModel member = mock(AccountModel.class);
    when(member.getApplicantDisplayName()).thenReturn("Dave");
    when(member.getEmailAddress()).thenReturn("");
    when(member.ownedApplicantIds()).thenReturn(ImmutableList.of());
    when(tiGroup.getTrustedIntermediaries()).thenReturn(ImmutableList.of(member));

    TiAccountSettingsPageViewModel result = mapper.map(tiGroup);

    assertThat(result.getOrgMembers().get(0).getEmail()).isEqualTo("(no email address)");
  }
}
