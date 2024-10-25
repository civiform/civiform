package forms.admin;

import java.util.ArrayList;
import java.util.List;

/** A form for selecting multiple applications to be considered for a status update. */
public final class BulkStatusUpdateForm {
  private List<Long> applicationsIds;
  private String statusText;
  private boolean shouldSendEmail;

  public boolean getShouldSendEmail() {
    return shouldSendEmail;
  }

  public void setShouldSendEmail(boolean shouldSendEmail) {
    this.shouldSendEmail = shouldSendEmail;
  }

  public BulkStatusUpdateForm() {
    this.applicationsIds = new ArrayList<>();
    this.statusText = "";
    this.shouldSendEmail = false;
  }

  public String getStatusText() {
    return statusText;
  }

  public void setStatusText(String statusText) {
    this.statusText = statusText;
  }

  public List<Long> getApplicationsIds() {
    return applicationsIds;
  }

  public void setApplicationsIds(List<Long> applicationsIds) {
    this.applicationsIds = applicationsIds;
  }
}
