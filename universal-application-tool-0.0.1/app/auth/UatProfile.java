package auth;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.base.Preconditions;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import models.Account;
import models.Applicant;
import play.libs.concurrent.HttpExecutionContext;
import repository.DatabaseExecutionContext;

/**
 * This is a "pure" wrapper of UatProfileData. Since UatProfileData is the serialized data about a
 * profile, this class should not store any data that should be serialized. It should contain only
 * server-local information, like execution contexts, database connections, etc.
 */
public class UatProfile {
  private DatabaseExecutionContext dbContext;
  private HttpExecutionContext httpContext;
  private UatProfileData profileData;

  @Inject
  public UatProfile(
      DatabaseExecutionContext dbContext,
      HttpExecutionContext httpContext,
      UatProfileData profileData) {
    this.dbContext = Preconditions.checkNotNull(dbContext);
    this.httpContext = Preconditions.checkNotNull(httpContext);
    this.profileData = Preconditions.checkNotNull(profileData);
  }

  public CompletableFuture<Applicant> getApplicant() {
    return this.getAccount()
        .thenApplyAsync(
            (a) ->
                a.getApplicants().stream()
                    .sorted(
                        Comparator.comparing(
                            (applicant) -> applicant.getApplicantData().getCreatedTime()))
                    .findFirst()
                    .orElseThrow(),
            httpContext.current());
  }

  public CompletableFuture<Account> getAccount() {
    return supplyAsync(
        () -> {
          Account account = new Account();
          account.id = Long.valueOf(this.profileData.getId());
          account.refresh();
          return account;
        },
        dbContext);
  }

  public String getClientName() {
    return profileData.getClientName();
  }

  public Set<String> getRoles() {
    return profileData.getRoles();
  }

  public boolean isUatAdmin() {
    return profileData.getRoles().contains(Roles.ROLE_UAT_ADMIN.toString());
  }

  public String getId() {
    return profileData.getId();
  }
}
