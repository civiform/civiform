package views.shared;

import lombok.Builder;
import play.mvc.Http;
import services.JsonUtils;
import services.ObjectMapperSingleton;
import services.settings.SettingsManifest;

/**
 * This record contains feature flags used in thymeleaf templates. They come from {@link
 * SettingsManifest}
 *
 * <ul>
 *   <li>Only include feature flags which already have guards in server side code
 *   <li>Only include feature flags that are actually needed in thymeleaf or client side typescript
 * </ul>
 */
@Builder
public record FeatureFlags(
    boolean isAdminUiMigrationScEnabled, boolean isAdminUiMigrationScExtendedEnabled) {

  /** Create an instance of this record from the {@link SettingsManifest} */
  public static FeatureFlags fromSettingsManifest(
      SettingsManifest settingsManifest, Http.RequestHeader request) {
    return FeatureFlags.builder()
        .isAdminUiMigrationScEnabled(settingsManifest.getAdminUiMigrationScEnabled(request))
        .isAdminUiMigrationScExtendedEnabled(
            settingsManifest.getAdminUiMigrationScExtendedEnabled(request))
        .build();
  }

  public String toJson() {
    return JsonUtils.writeValueAsString(ObjectMapperSingleton.instance(), this);
  }
}
