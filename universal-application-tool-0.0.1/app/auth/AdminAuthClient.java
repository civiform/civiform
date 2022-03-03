package auth;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

/**
 * AdminAuthClient is the annotation for the auth client responsible for admin authentication.
 * This client will implement IndirectClient.
 *
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminAuthClient {}
