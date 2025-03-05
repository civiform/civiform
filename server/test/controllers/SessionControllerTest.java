package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileUtils;
import java.time.Clock;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import services.settings.SettingsManifest;

public class SessionControllerTest {
  private SessionController controller;
  private ProfileUtils profileUtils;
  private CiviFormProfileData mockProfileData;
  private CiviFormProfile mockProfile;
  private SettingsManifest mockSettingsManifest;
  private Clock clock;

  @Before
  public void setUp() {
    profileUtils = mock(ProfileUtils.class);
    mockSettingsManifest = mock(SettingsManifest.class);
    clock = mock(Clock.class);
    mockProfile = mock(CiviFormProfile.class);
    mockProfileData = mock(CiviFormProfileData.class);

    when(mockProfile.getProfileData()).thenReturn(mockProfileData);
    when(profileUtils.optionalCurrentUserProfile(any(Http.Request.class)))
        .thenReturn(Optional.of(mockProfile));
    controller = new SessionController(profileUtils, mockSettingsManifest, clock);
  }

  @Test
  public void extendSession_withValidRequest_updatesLastActivityTime() {
    when(mockSettingsManifest.getSessionTimeoutEnabled()).thenReturn(true);

    Http.Request request = fakeRequestBuilder().build();

    Result result = controller.extendSession(request);

    assertThat(result.status()).isEqualTo(Http.Status.OK);
    verify(mockProfileData).updateLastActivityTime(clock);
  }

  @Test
  public void extendSession_whenTimeoutDisabled_returnsBadRequest() {
    when(mockSettingsManifest.getSessionTimeoutEnabled()).thenReturn(false);
    Http.Request request = fakeRequestBuilder().build();

    Result result = controller.extendSession(request);

    assertThat(result.status()).isEqualTo(Http.Status.BAD_REQUEST);
    verify(mockProfileData, never()).updateLastActivityTime(any());
  }

  @Test
  public void extendSession_withNoProfile_returnsUnauthorized() {
    when(mockSettingsManifest.getSessionTimeoutEnabled()).thenReturn(true);

    when(profileUtils.optionalCurrentUserProfile(any(Http.Request.class)))
        .thenReturn(Optional.empty());
    Http.Request request = fakeRequestBuilder().build();

    Result result = controller.extendSession(request);

    assertThat(result.status()).isEqualTo(Http.Status.UNAUTHORIZED);
    verify(mockProfileData, never()).updateLastActivityTime(any());
  }
}
