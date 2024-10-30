package forms.admin;

/** A form for setting the eligibility message of a block. */
public final class BlockEligibilityMessageForm {
  public static final String ELIGIBILITY_MESSAGE = "eligibilityMessage";

  private String eligibilityMessage;

  public BlockEligibilityMessageForm() {
    this.eligibilityMessage = "";
  }

  public BlockEligibilityMessageForm(String eligibilityMessage) {
    this.eligibilityMessage = eligibilityMessage;
  }

  public String getEligibilityMessage() {
    return this.eligibilityMessage;
  }

  public void setEligibilityMessage(String eligibilityMessage) {
    this.eligibilityMessage = eligibilityMessage;
  }
}
