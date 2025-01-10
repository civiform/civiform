package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.CiviFormProfile;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import models.AccountModel;
import models.ApplicantModel;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import repository.AccountRepository;

public class BackChannelLogoutControllerTest extends WithMockedProfiles {
  private BackChannelLogoutController controller;
  private CiviFormProfile testProfile;
  private AccountRepository accountRepository;

  @Before
  public void setup() {
    controller = instanceOf(BackChannelLogoutController.class);
    accountRepository = instanceOf(AccountRepository.class);

    testProfile = mock(CiviFormProfile.class);
  }

  @Test
  public void testIndexWithNoProfile() {
    Http.Request request = fakeRequestBuilder().header(skipUserProfile, "true").build();
    Result result = controller.index(request).toCompletableFuture().join();
    assertThat(result.redirectLocation()).isEqualTo(Optional.of("/"));
  }

  @Test
  public void testIndexWithProfile_withSession() {
    // Add active session to account
    Clock clock = Clock.fixed(Instant.ofEpochSecond(10), ZoneOffset.UTC);
    ApplicantModel applicant = createApplicantWithMockedProfile();
    AccountModel account = applicant.getAccount();
    account.addActiveSession("fake session", clock);
    account.save();

    when(testProfile.getAccount()).thenReturn(CompletableFuture.completedFuture(account));

    Http.Request request = fakeRequestBuilder().header(skipUserProfile, "false").build();
    Result result = controller.index(request).toCompletableFuture().join();

    // Wait because the active session clearing is async and non-blocking
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    AccountModel updatedAccount = accountRepository.lookupAccount(account.id).get();
    assertThat(updatedAccount.getActiveSessions()).isEmpty();
    assertThat(result.redirectLocation()).isEqualTo(Optional.of("/"));
  }

  @Test
  public void testIndexWithProfile_withMultipleSessions() {
    // Add active session to account
    Clock clock = Clock.fixed(Instant.ofEpochSecond(10), ZoneOffset.UTC);
    ApplicantModel applicant = createApplicantWithMockedProfile();
    AccountModel account = applicant.getAccount();
    account.addActiveSession("session1", clock);
    account.addActiveSession("session2", clock);
    account.save();

    when(testProfile.getAccount()).thenReturn(CompletableFuture.completedFuture(account));

    Http.Request request = fakeRequestBuilder().header(skipUserProfile, "false").build();
    Result result = controller.index(request).toCompletableFuture().join();

    // Wait because the active session clearing is async and non-blocking
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    AccountModel updatedAccount = accountRepository.lookupAccount(account.id).get();
    assertThat(updatedAccount.getActiveSessions()).isEmpty();
    assertThat(result.redirectLocation()).isEqualTo(Optional.of("/"));
  }
}
