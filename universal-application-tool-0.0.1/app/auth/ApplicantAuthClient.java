package auth;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

/**
 * ApplicantAuthClient is the annotation for the auth client responsible for applicant
 * authentication. This client must implement IndirectClient -> {@link
 * org.pac4j.core.client.IndirectClient}.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface ApplicantAuthClient {}
