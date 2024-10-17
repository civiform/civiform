package forms.admin;

import java.util.ArrayList;
import java.util.List;

public final class BulkStatusUpdateForm {
  private List<String> applicationsIds;
  private String statusText;
  private Boolean maybeSendEmail;

  public Boolean isMaybeSendEmail() {
    return maybeSendEmail;
  }

  public void setMaybeSendEmail(Boolean maybeSendEmail) {
    this.maybeSendEmail = maybeSendEmail;
  }

  public BulkStatusUpdateForm() {
    this.applicationsIds = new ArrayList<>();
    this.statusText = "";
    this.maybeSendEmail = false;
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
