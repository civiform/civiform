package views.admin.shared;

import lombok.Builder;

/**
 * This record contains feature flags used in admin thymeleaf templates
 *
 * @param isAdminUiMigrationScEnabled Value from {@link
 *     services.settings.SettingsManifest#getAdminUiMigrationScEnabled}
 * @param isAdminUiMigrationScExtendedEnabled Value from {@link
 *     services.settings.SettingsManifest#getAdminUiMigrationScExtendedEnabled}
 */
@Builder
public record FeatureFlag(
    Boolean isAdminUiMigrationScEnabled, Boolean isAdminUiMigrationScExtendedEnabled) {}
