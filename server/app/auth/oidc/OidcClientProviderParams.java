package auth.oidc;

import auth.ProfileFactory;
import com.google.auto.value.AutoValue;
import com.google.errorprone.annotations.RestrictedApi;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import javax.inject.Provider;
import repository.AccountRepository;

/** Class that holds parameters required by OidcClientProvider and its subclasses. */
@AutoValue
public abstract class OidcClientProviderParams {
  public static OidcClientProviderParams create(
      Config configuration,
      ProfileFactory profileFactory,
      Provider<AccountRepository> accountRepositoryProvider) {
    return new AutoValue_OidcClientProviderParams(
        configuration, profileFactory, accountRepositoryProvider);
  }

  // Our tests have paths like:
  //   /usr/src/server/test/auth/ProfileMergeTest.java
  @RestrictedApi(explanation = "Only allow for tests", allowedOnPath = ".*/test/.*")
  public static OidcClientProviderParams create(
      ProfileFactory profileFactory, Provider<AccountRepository> accountRepositoryProvider) {
    return create(ConfigFactory.empty(), profileFactory, accountRepositoryProvider);
  }

  public abstract Config configuration();

  abstract ProfileFactory profileFactory();

  abstract Provider<AccountRepository> accountRepositoryProvider();
}
