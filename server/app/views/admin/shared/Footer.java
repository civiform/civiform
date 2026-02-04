package views.admin.shared;

import lombok.Builder;

/** This record contains values used by the {@code admin/shared/Footer.html} file. */
@Builder
public record Footer(String technicalSupportEmail) {

  public String adminLoginUrl() {
    return controllers.routes.LoginController.adminLogin().url();
  }
}
