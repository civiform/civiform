package forms;

public class AddTrustedIntermediaryForm {
  private String emailAddress;

  public AddTrustedIntermediaryForm(String emailAddress) {
    this.emailAddress = emailAddress;
  }

  public AddTrustedIntermediaryForm() {
    emailAddress = "";
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  public void setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
  }
}
