package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
  private SettingsManifest mockSettingsManifest;
  private Clock clock;

  @Before
  public void setUp() {
    profileUtils = mock(ProfileUtils.class);
    mockSettingsManifest = mock(SettingsManifest.class);
    clock = mock(Clock.class);
    CiviFormProfile mockProfile = mock(CiviFormProfile.class);
    mockProfileData = mock(CiviFormProfileData.class);

    when(mockProfile.getProfileData()).thenReturn(mockProfileData);
    when(profileUtils.optionalCurrentUserProfile(any(Http.Request.class)))
        .thenReturn(Optional.of(mockProfile));
    controller = new SessionController(profileUtils, mockSettingsManifest, clock);
  }

  @Test
  public void extendSession_withValidRequest_updatesLastActivityTime() {
    Http.Request request = fakeRequestBuilder().build();
    when(mockSettingsManifest.getSessionTimeoutEnabled(request)).thenReturn(true);

    Result result = controller.extendSession(request);

    assertThat(result.status()).isEqualTo(Http.Status.OK);
    verify(mockProfileData).updateLastActivityTime(clock);
  }

  @Test
  public void extendSession_whenTimeoutDisabled_returnsBadRequest() {
    Http.Request request = fakeRequestBuilder().build();
    when(mockSettingsManifest.getSessionTimeoutEnabled(request)).thenReturn(false);

    Result result = controller.extendSession(request);

    assertThat(result.status()).isEqualTo(Http.Status.BAD_REQUEST);
    verify(mockProfileData, never()).updateLastActivityTime(any());
  }

  @Test
  public void extendSession_withNoProfile_returnsUnauthorized() {
    Http.Request request = fakeRequestBuilder().build();

    when(mockSettingsManifest.getSessionTimeoutEnabled(request)).thenReturn(true);

    when(profileUtils.optionalCurrentUserProfile(any(Http.Request.class)))
        .thenReturn(Optional.empty());

    Result result = controller.extendSession(request);

    assertThat(result.status()).isEqualTo(Http.Status.UNAUTHORIZED);
    verify(mockProfileData, never()).updateLastActivityTime(any());
  }
}
