package forms;

import java.util.ArrayList;
import java.util.List;

public class AddProgramAdminForm {

  private List<String> adminEmails;

  public AddProgramAdminForm() {
    this.adminEmails = new ArrayList<>();
  }

  public List<String> getAdminEmails() {
    return adminEmails;
  }

  public void setAdminEmails(List<String> adminEmails) {
    this.adminEmails = adminEmails;
  }
}
