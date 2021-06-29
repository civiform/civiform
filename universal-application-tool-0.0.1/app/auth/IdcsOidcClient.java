package auth;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

/**
 * IdcsOidcClient is an annotation for IDCS-flavored OidcClient.
 *
 * <p>See {@link modules.SecurityModule#provideIDCSClient}.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface IdcsOidcClient {}
