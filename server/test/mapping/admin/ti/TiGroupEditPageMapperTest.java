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
import play.mvc.Http;
import views.admin.ti.TiGroupEditPageViewModel;

public final class TiGroupEditPageMapperTest {

  private TiGroupEditPageMapper mapper;
  private Http.Request mockRequest;
  private Http.Flash mockFlash;

  @Before
  public void setup() {
    mapper = new TiGroupEditPageMapper();
    mockRequest = mock(Http.Request.class);
    mockFlash = mock(Http.Flash.class);
    when(mockRequest.flash()).thenReturn(mockFlash);
    when(mockFlash.get("providedEmailAddress")).thenReturn(Optional.empty());
    when(mockFlash.get("error")).thenReturn(Optional.empty());
  }

  private TrustedIntermediaryGroupModel buildMockTiGroup() {
    TrustedIntermediaryGroupModel tiGroup = mock(TrustedIntermediaryGroupModel.class);
    tiGroup.id = 10L;
    when(tiGroup.getName()).thenReturn("Test Group");
    when(tiGroup.getDescription()).thenReturn("Test Description");
    when(tiGroup.getTrustedIntermediaries()).thenReturn(ImmutableList.of());
    return tiGroup;
  }

  @Test
  public void map_setsGroupNameAndDescription() {
    TrustedIntermediaryGroupModel tiGroup = buildMockTiGroup();

    TiGroupEditPageViewModel result = mapper.map(tiGroup, mockRequest);

    assertThat(result.getGroupName()).isEqualTo("Test Group");
    assertThat(result.getGroupDescription()).isEqualTo("Test Description");
  }

  @Test
  public void map_setsUrls() {
    TrustedIntermediaryGroupModel tiGroup = buildMockTiGroup();

    TiGroupEditPageViewModel result = mapper.map(tiGroup, mockRequest);

    assertThat(result.getBackUrl()).isNotEmpty();
    assertThat(result.getAddMemberActionUrl()).isNotEmpty();
  }

  @Test
  public void map_noFlash_setsDefaults() {
    TrustedIntermediaryGroupModel tiGroup = buildMockTiGroup();

    TiGroupEditPageViewModel result = mapper.map(tiGroup, mockRequest);

    assertThat(result.getProvidedEmailAddress()).isEmpty();
    assertThat(result.getErrorMessage()).isEmpty();
  }

  @Test
  public void map_withFlashValues_setsFlashData() {
    when(mockFlash.get("providedEmailAddress")).thenReturn(Optional.of("test@example.com"));
    when(mockFlash.get("error")).thenReturn(Optional.of("Email already exists"));
    TrustedIntermediaryGroupModel tiGroup = buildMockTiGroup();

    TiGroupEditPageViewModel result = mapper.map(tiGroup, mockRequest);

    assertThat(result.getProvidedEmailAddress()).isEqualTo("test@example.com");
    assertThat(result.getErrorMessage()).contains("Email already exists");
  }

  @Test
  public void map_noMembers_emptyMembersList() {
    TrustedIntermediaryGroupModel tiGroup = buildMockTiGroup();

    TiGroupEditPageViewModel result = mapper.map(tiGroup, mockRequest);

    assertThat(result.getMembers()).isEmpty();
  }

  @Test
  public void map_withMembers_buildsMemberRows() {
    TrustedIntermediaryGroupModel tiGroup = buildMockTiGroup();
    AccountModel account = mock(AccountModel.class);
    account.id = 99L;
    when(account.getApplicantDisplayName()).thenReturn("Jane Doe");
    when(account.getEmailAddress()).thenReturn("jane@example.com");
    when(account.ownedApplicantIds()).thenReturn(ImmutableList.of(1L));
    when(tiGroup.getTrustedIntermediaries()).thenReturn(ImmutableList.of(account));

    TiGroupEditPageViewModel result = mapper.map(tiGroup, mockRequest);

    assertThat(result.getMembers()).hasSize(1);
    TiGroupEditPageViewModel.TiMemberRow row = result.getMembers().get(0);
    assertThat(row.getDisplayName()).isEqualTo("Jane Doe");
    assertThat(row.getEmailAddress()).isEqualTo("jane@example.com");
    assertThat(row.getStatus()).isEqualTo("OK");
    assertThat(row.getAccountId()).isEqualTo(99L);
    assertThat(row.getRemoveActionUrl()).isNotEmpty();
  }

  @Test
  public void map_memberNotSignedIn_setsNotYetSignedInStatus() {
    TrustedIntermediaryGroupModel tiGroup = buildMockTiGroup();
    AccountModel account = mock(AccountModel.class);
    account.id = 100L;
    when(account.getApplicantDisplayName()).thenReturn("New User");
    when(account.getEmailAddress()).thenReturn("new@example.com");
    when(account.ownedApplicantIds()).thenReturn(ImmutableList.of());
    when(tiGroup.getTrustedIntermediaries()).thenReturn(ImmutableList.of(account));

    TiGroupEditPageViewModel result = mapper.map(tiGroup, mockRequest);

    assertThat(result.getMembers().get(0).getStatus()).isEqualTo("Not yet signed in.");
  }
}
