package views.admin.shared;

import java.util.Optional;
import lombok.Builder;
import views.admin.AdminLayout;

/** This record contains values used by the {@code admin/shared/AdminCommonHeader.html} file. */
@Builder
public record AdminCommonHeader(
    AdminLayout.NavPage activeNavPage, Boolean isOnlyProgramAdmin, Boolean isApiBridgeEnabled) {

  public String programsUrl() {
    if (isOnlyProgramAdmin()) {
      return controllers.admin.routes.ProgramAdminController.index().url();
    }

    return controllers.admin.routes.AdminProgramController.index().url();
  }

  public Boolean showProgramsUrl() {
    return true;
  }

  public String questionsUrl() {
    return controllers.admin.routes.AdminQuestionController.index(Optional.empty()).url();
  }

  public Boolean showQuestionsUrl() {
    return !isOnlyProgramAdmin();
  }

  public String intermediaryUrl() {
    return controllers.admin.routes.TrustedIntermediaryManagementController.index().url();
  }

  public Boolean showIntermediaryUrl() {
    return !isOnlyProgramAdmin();
  }

  public String reportingUrl() {
    return controllers.admin.routes.AdminReportingController.index().url();
  }

  public Boolean showReportingUrl() {
    return true;
  }

  public String apiKeysUrl() {
    return controllers.admin.routes.AdminApiKeysController.index().url();
  }

  public Boolean showApiKeysUrl() {
    return !isOnlyProgramAdmin();
  }

  public String apiDocsUrl() {
    return controllers.docs.routes.ApiDocsController.index().url();
  }

  public Boolean showApiDocsUrl() {
    return true;
  }

  public String apiBridgeDiscoveryUrl() {
    return controllers.admin.apibridge.routes.DiscoveryController.discovery().url();
  }

  public Boolean showApiBridgeDiscoveryUrl() {
    return isApiBridgeEnabled && !isOnlyProgramAdmin();
  }

  public String settingsUrl() {
    return controllers.admin.routes.AdminSettingsController.index().url();
  }

  public Boolean showSettingsUrl() {
    return !isOnlyProgramAdmin();
  }

  public String logoutUrl() {
    return org.pac4j.play.routes.LogoutController.logout().url();
  }

  public Boolean showApi() {
    return showApiKeysUrl() || showApiDocsUrl() || showApiBridgeDiscoveryUrl();
  }

  public Boolean isApiPage() {
    return switch (activeNavPage()) {
      case API_DOCS, API_KEYS, API_BRIDGE_DISCOVERY -> true;
      default -> false;
    };
  }
}
