package forms.admin;

import java.util.ArrayList;
import java.util.List;

public final class BulkStatusUpdateForm {
  private List<String> applicationsIds;
  private String statusText;

  public BulkStatusUpdateForm() {
    this.applicationsIds = new ArrayList<>();
    this.statusText = "";
  }

  public String getStatusText() {
    return statusText;
  }

  public void setStatusText(String statusText) {

    this.statusText = statusText;
  }

  public List<String> getApplicationsIds() {
    return applicationsIds;
  }

  public void setApplicationsIds(List<String> applicationsIds) {
    this.applicationsIds = applicationsIds;
  }
}
