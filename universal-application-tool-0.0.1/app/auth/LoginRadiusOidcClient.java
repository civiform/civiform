package auth;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

/**
 * LoginRadiusOidcClient is an annotation for AD-flavored OidcClient.
 *
 * <p>See {@link modules.SecurityModule#provideLoginRadiusClient}.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRadiusOidcClient {}
