package forms;

import java.util.ArrayList;
import java.util.List;

/** A form for adding and removing program admins by email for a specific program. */
public final class ManageProgramAdminsForm {

  private String adminEmail;
  // TODO: remove the fields below in favor of adminEmail. We want the interaction to operate on a
  // single email address at a time.
  private List<String> addAdminEmails;
  private List<String> removeAdminEmails;

  public ManageProgramAdminsForm() {
    this.addAdminEmails = new ArrayList<>();
    this.removeAdminEmails = new ArrayList<>();
    this.adminEmail = "";
  }

  public List<String> getAdminEmails() {
    return addAdminEmails;
  }

  public void setAdminEmails(List<String> adminEmails) {
    this.addAdminEmails = adminEmails;
  }

  public List<String> getRemoveAdminEmails() {
    return removeAdminEmails;
  }

  public void setRemoveAdminEmails(List<String> removeAdminEmails) {
    this.removeAdminEmails = removeAdminEmails;
  }

  public String getAdminEmail() {
    return adminEmail;
  }

  public void setAdminEmail(String adminEmail) {
    this.adminEmail = adminEmail;
  }
}
