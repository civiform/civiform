package filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileUtils;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import models.AccountModel;
import org.junit.Before;
import org.junit.Test;
import play.libs.streams.Accumulator;
import play.mvc.EssentialAction;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.test.WithApplication;
import repository.DatabaseExecutionContext;
import services.settings.SettingsManifest;

public class ValidAccountFilterTest extends WithApplication {

  private ProfileUtils profileUtils;
  private SettingsManifest settingsManifest;
  private ValidAccountFilter filter;
  private CiviFormProfile mockProfile;
  private CiviFormProfileData mockProfileData;
  private AccountModel mockAccount;

  @Before
  public void setUp() {
    profileUtils = mock(ProfileUtils.class);
    settingsManifest = mock(SettingsManifest.class);

    filter =
        new ValidAccountFilter(
            profileUtils,
            () -> settingsManifest, // Provider<SettingsManifest>
            mat,
            () -> instanceOf(DatabaseExecutionContext.class) // Provider<DatabaseExecutionContext>
            );

    mockProfile = mock(CiviFormProfile.class);
    mockProfileData = mock(CiviFormProfileData.class);
    mockAccount = mock(AccountModel.class);
    when(mockProfile.getProfileData()).thenReturn(mockProfileData);
    when(mockProfile.getAccount()).thenReturn(CompletableFuture.completedFuture(mockAccount));
  }

  @Test
  public void testValidProfile_sessionTimeoutDisabled_noUpdatesLastActivityTime() throws Exception {
    RequestHeader request = fakeRequestBuilder().method("GET").uri("/programs/1").build();
    when(profileUtils.optionalCurrentUserProfile(request)).thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile))
        .thenReturn(CompletableFuture.completedFuture(true));
    when(settingsManifest.getSessionTimeoutEnabled(request)).thenReturn(false);
    when(settingsManifest.getSessionReplayProtectionEnabled()).thenReturn(false);

    Result result = executeFilter(request);

    verify(mockProfileData, never()).updateLastActivityTime(any());
    assertThat(result.status()).isEqualTo(200);
  }

  @Test
  public void testInvalidSession_sessionTimeoutDisabled_redirectsToLogout() throws Exception {
    RequestHeader request = fakeRequestBuilder().method("GET").uri("/programs/1").build();
    when(profileUtils.optionalCurrentUserProfile(request)).thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile))
        .thenReturn(CompletableFuture.completedFuture(true));
    when(settingsManifest.getSessionTimeoutEnabled(request)).thenReturn(false);
    when(settingsManifest.getSessionReplayProtectionEnabled()).thenReturn(true);

    // Session ID not found in active sessions
    when(mockAccount.getActiveSession(anyString())).thenReturn(Optional.empty());
    when(mockProfileData.getSessionId()).thenReturn("session123");

    Result result = executeFilter(request);

    assertThat(result.status()).isEqualTo(303); // Redirect status
    assertThat(result.redirectLocation()).hasValue("/logout");
  }

  @Test
  public void testInvalidProfile_redirectsToLogout() throws Exception {
    RequestHeader request = fakeRequestBuilder().method("GET").uri("/programs/1").build();
    when(profileUtils.optionalCurrentUserProfile(request)).thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile))
        .thenReturn(CompletableFuture.completedFuture(false));

    Result result = executeFilter(request);

    assertThat(result.status()).isEqualTo(303);
    assertThat(result.redirectLocation()).hasValue("/logout");
  }

  @Test
  public void testAllowedEndpoint_bypassesFilter() throws Exception {
    RequestHeader request = fakeRequestBuilder().method("GET").uri("/playIndex").build();
    when(profileUtils.optionalCurrentUserProfile(request)).thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile))
        .thenReturn(CompletableFuture.completedFuture(false));

    Result result = executeFilter(request);

    assertThat(result.status()).isEqualTo(200);
  }

  @Test
  public void testLogoutRequest_bypassesFilter() throws Exception {
    RequestHeader request = fakeRequestBuilder().method("GET").uri("/logout").build();
    when(profileUtils.optionalCurrentUserProfile(request)).thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile))
        .thenReturn(CompletableFuture.completedFuture(false));

    Result result = executeFilter(request);

    assertThat(result.status()).isEqualTo(200);
  }

  @Test
  public void testNoProfile_bypassesFilter() throws Exception {
    RequestHeader request = fakeRequestBuilder().method("GET").uri("/programs/1").build();
    when(profileUtils.optionalCurrentUserProfile(request)).thenReturn(Optional.empty());

    Result result = executeFilter(request);

    assertThat(result.status()).isEqualTo(200);
  }

  private Result executeFilter(RequestHeader request) throws Exception {
    EssentialAction action =
        filter.apply(
            EssentialAction.of(requestHeader -> Accumulator.done(play.mvc.Results.ok("Success"))));
    return action.apply(request).run(mat).toCompletableFuture().get();
  }
}
