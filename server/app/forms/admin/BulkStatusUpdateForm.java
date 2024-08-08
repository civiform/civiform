package forms.admin;

import java.util.ArrayList;
import java.util.List;

public class BulkStatusUpdateForm {
  private List<String> applicationsIds;
  private String status;

  public BulkStatusUpdateForm() {
    this.applicationsIds = new ArrayList<>();
    this.status = "";
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public List<String> getApplicationsIds() {
    return applicationsIds;
  }

  public void setApplicationsIds(List<String> applicationsIds) {
    this.applicationsIds = applicationsIds;
  }
}
