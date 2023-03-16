package auth.oidc.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.Test;
import org.pac4j.oidc.profile.OidcProfile;

public class AdfsGroupAccessorTest {

  private static final String AD_GROUPS_ATTRIBUTE_NAME = "ad-groups-attribute";

  @Test
  public void getGroups_getsGroups_asList() {
    OidcProfile oidcProfile = new OidcProfile();
    List<String> groups = List.of("aaa", "bbb", "ccc");
    oidcProfile.addAttribute(AD_GROUPS_ATTRIBUTE_NAME, groups);

    List<String> groupsResult = AdfsGroupAccessor.getGroups(oidcProfile, AD_GROUPS_ATTRIBUTE_NAME);
    assertThat(groupsResult).isEqualTo(groups);
  }

  @Test
  public void getGroups_getsGroups_asString() {
    OidcProfile oidcProfile = new OidcProfile();
    String groups = "aaa";
    oidcProfile.addAttribute(AD_GROUPS_ATTRIBUTE_NAME, groups);

    List<String> groupsResult = AdfsGroupAccessor.getGroups(oidcProfile, AD_GROUPS_ATTRIBUTE_NAME);
    assertThat(groupsResult).containsExactly(groups);
  }

  @Test
  public void getGroups_returnsEmptyList_whenAttributeIsEmptyString() {
    OidcProfile oidcProfile = new OidcProfile();
    String groups = "";
    oidcProfile.addAttribute(AD_GROUPS_ATTRIBUTE_NAME, groups);

    List<String> groupsResult = AdfsGroupAccessor.getGroups(oidcProfile, AD_GROUPS_ATTRIBUTE_NAME);
    assertThat(groupsResult).isEmpty();
  }

  @Test
  public void getGroups_returnsEmptyList_whenAttributeNotPresent() {
    // No attributes added to OidcProfile.
    OidcProfile oidcProfile = new OidcProfile();

    List<String> groupsResult = AdfsGroupAccessor.getGroups(oidcProfile, AD_GROUPS_ATTRIBUTE_NAME);
    assertThat(groupsResult).isEmpty();
  }

  @Test
  public void getGroups_throwsException_whenAdGroupsValueIsNotAStringOrList() {
    int groups = 12345;
    OidcProfile oidcProfile = new OidcProfile();

    oidcProfile.addAttribute(AD_GROUPS_ATTRIBUTE_NAME, groups);

    assertThatThrownBy(() -> AdfsGroupAccessor.getGroups(oidcProfile, AD_GROUPS_ATTRIBUTE_NAME), "")
        .isInstanceOf(ClassCastException.class);
  }
}
