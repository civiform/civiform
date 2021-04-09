package auth;

import java.util.List;
import org.pac4j.core.authorization.authorizer.ProfileAuthorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.UserProfile;

/** Authorizes an applicant to only view data for their profile. */
public class ApplicantAuthorizer extends ProfileAuthorizer {

  @Override
  public boolean isAuthorized(
      WebContext context, SessionStore sessionStore, List<UserProfile> profiles) {
    return profiles.stream()
        .anyMatch(profile -> isProfileAuthorized(context, sessionStore, profile));
  }

  @Override
  public boolean isProfileAuthorized(
      WebContext context, SessionStore sessionStore, UserProfile profile) {
    // TODO: parse the applicantId from the request URL
    String id = context.getPath();
    return id.equals(profile.getId());
  }
}
