package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.OK;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProfileFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import models.AccountModel;
import models.ApplicantModel;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;

public class BackChannelLogoutControllerTest extends WithMockedProfiles {
  private BackChannelLogoutController controller;
  private ProfileFactory profileFactory;
  private ApplicantModel applicant;

  @Before
  public void setupController() {
    controller = instanceOf(BackChannelLogoutController.class);
    profileFactory = instanceOf(ProfileFactory.class);
    applicant = createApplicantWithMockedProfile();
  }

  @Test
  public void testIndexWithNoProfile() {
    Http.Request request = fakeRequestBuilder().header(skipUserProfile, "true").build();
    Result result = controller.index(request).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.redirectLocation()).isEqualTo(Optional.of("/"));
  }

  @Test
  public void testIndexWithProfile_withSession() {
    // Add active session to account
    AccountModel account = applicant.getAccount();
    Clock clock = Clock.fixed(Instant.ofEpochSecond(10), ZoneOffset.UTC);
    account.addActiveSession("fake session", clock);
    account.save();

    Http.Request request = fakeRequestBuilder().header(skipUserProfile, "false").build();
    Result result = controller.index(request).toCompletableFuture().join();

    assertThat(account.getActiveSessionsForTest()).isEmpty();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.redirectLocation()).isEqualTo(Optional.of("/"));
  }

  @Test
  public void testIndexWithProfile_withMultipleSessions() {
    // Add active session to account
    AccountModel account = applicant.getAccount();
    Clock clock = Clock.fixed(Instant.ofEpochSecond(10), ZoneOffset.UTC);
    account.addActiveSession("session1", clock);
    account.addActiveSession("session2", clock);
    account.save();

    Http.Request request = fakeRequestBuilder().header(skipUserProfile, "false").build();
    Result result = controller.index(request).toCompletableFuture().join();

    assertThat(account.getActiveSessionsForTest()).isEmpty();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.redirectLocation()).isEqualTo(Optional.of("/"));
  }
}
