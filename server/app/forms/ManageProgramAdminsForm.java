package forms;

import java.util.ArrayList;
import java.util.List;

/** A form for adding and removing program admins by email for a specific program. */
public class ManageProgramAdminsForm {

  private List<String> addAdminEmails;
  private List<String> removeAdminEmails;

  public ManageProgramAdminsForm() {
    this.addAdminEmails = new ArrayList<>();
    this.removeAdminEmails = new ArrayList<>();
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
}
