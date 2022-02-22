package auth;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

/**
 * LoginRadiusSamlClient is an annotation for SAML client customized for LoginRadius.
 *
 * <p>See {@link modules.SecurityModule#provideLoginRadiusClient}.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRadiusSamlClient {}
