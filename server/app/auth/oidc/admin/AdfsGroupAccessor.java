package auth.oidc.admin;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.pac4j.oidc.profile.OidcProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AdfsGroupAccessor {
  private static final Logger logger = LoggerFactory.getLogger(AdfsGroupAccessor.class);

  public static List<String> getGroups(OidcProfile oidcProfile, String adGroupsAttributeName) {
    if (!oidcProfile.containsAttribute(adGroupsAttributeName)) {
      return List.of();
    }

    try {
      // This line is unchecked so that setting the `clazz` argument to a raw type (List) that is
      // later converted to a generic type (List<String>) is allowed by the compiler.
      @SuppressWarnings("unchecked")
      List<String> groups = oidcProfile.getAttribute(adGroupsAttributeName, List.class);
      return groups;
    } catch (ClassCastException e) {
      // The attribute value may not be a List in some cases (such as the single group case, when
      // it is a String).
      logger.info("AD group for attribute {} was not a list.", adGroupsAttributeName);
    }

    try {
      String group = oidcProfile.getAttribute(adGroupsAttributeName, String.class);
      return StringUtils.isEmpty(group) ? List.of() : List.of(group);
    } catch (ClassCastException e) {
      logger.info("AD group for attribute {} was not a String nor List.", adGroupsAttributeName);
      throw e;
    }
  }
}
