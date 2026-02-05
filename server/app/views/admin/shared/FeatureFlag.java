package views.admin.shared;

import lombok.Builder;

/** This record contains feature flags used in admin thymeleaf templates */
@Builder
public record FeatureFlag(
    // settingsManifest.getAdminUiMigrationScExtendedEnabled
    Boolean isExtendedAdminUi) {}
