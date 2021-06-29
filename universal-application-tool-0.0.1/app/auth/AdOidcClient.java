package auth;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

/**
 * AdOidcClient is an annotation for AD-flavored OidcClient.
 *
 * <p>See {@link modules.SecurityModule#provideAdClient}.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface AdOidcClient {}
