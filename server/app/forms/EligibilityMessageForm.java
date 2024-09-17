package forms;

import com.google.common.collect.ImmutableList;

/**
 * A form for setting the description of the program's summary image. (The description is used for
 * alt text.)
 */
public final class EligibilityMessageForm {
  public static final String ELIGIBILITY_MESSAGE = "eligibilityMessage";

  public static final ImmutableList<String> FIELD_NAMES = ImmutableList.of(ELIGIBILITY_MESSAGE);

  private String eligibilityMessage;

  public EligibilityMessageForm() {
    this.eligibilityMessage = "";
  }

  public EligibilityMessageForm(String eligibilityMessage) {
    this.eligibilityMessage = eligibilityMessage;
  }

  public String getEligibilityMessage() {
    return this.eligibilityMessage;
  }

  @SuppressWarnings("unused") // Used by FormFactory
  public void setEligibilityMessage(String eligibilityMessage) {
    this.eligibilityMessage = eligibilityMessage;
  }
}
