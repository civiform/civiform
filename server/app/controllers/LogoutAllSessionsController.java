package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.AccountModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import repository.AccountRepository;

/**
 * This controller handles back channel logout requests from the identity provider. For example, if
 * a user resets their password, the government can choose to send a back channel logout request to
 * CiviForm.
 */
public class LogoutAllSessionsController extends Controller {

  private static final Logger logger = LoggerFactory.getLogger(LogoutAllSessionsController.class);
  private final ProfileUtils profileUtils;
  private final AccountRepository accountRepository;

  @Inject
  public LogoutAllSessionsController(
      ProfileUtils profileUtils, AccountRepository accountRepository) {

    this.profileUtils = checkNotNull(profileUtils);
    this.accountRepository = checkNotNull(accountRepository);
  }

  public CompletionStage<Result> index(Http.Request request) {
    Optional<CiviFormProfile> optionalProfile = profileUtils.optionalCurrentUserProfile(request);
    try {
      if (optionalProfile.isPresent()) {
        CiviFormProfile profile = optionalProfile.get();
        profile
            .getAccount()
            .thenAccept(
                account -> {
                  logger.debug("Found account for back channel logout: {}", account.id);
                  account.clearActiveSessions();
                  account.save();
                })
            .exceptionally(
                e -> {
                  logger.error(e.getMessage(), e);
                  return null;
                });
      } else {
        logger.warn("No account found for back channel logout");
      }
    } catch (RuntimeException e) {
      logger.error("Error clearing session from account", e);
    }

    // Redirect to the landing page
    return CompletableFuture.completedFuture(redirect(routes.HomeController.index().url()));
  }

  public CompletionStage<Result> logoutFromEmail(Http.Request request, String email) {
    logger.info("Received back channel logout request from remote address: {}", request.remoteAddress());
    accountRepository
        .lookupAccountByEmailAsync(email)
        .thenAccept(
            maybeAccount -> {
              if (maybeAccount.isPresent()) {
                AccountModel account = maybeAccount.get();
                logger.debug("Found account for back channel logout: {}", account.id);
                logger.info("Found authority id for back channel logout: {}", account.getAuthorityId());
                account.clearActiveSessions();
                account.save();
              } else {
                logger.warn("No account found for back channel logout");
              }
            })
        .exceptionally(
            e -> {
              logger.error(e.getMessage(), e);
              return null;
            });

    // Redirect to the landing page
    return CompletableFuture.completedFuture(redirect(routes.HomeController.index().url()));
  }
}
