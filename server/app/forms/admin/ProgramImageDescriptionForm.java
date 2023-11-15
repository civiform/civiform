package forms.admin;

import com.google.common.collect.ImmutableList;

/**
 * A form for setting the description of the program's summary image. (The description is used for
 * alt text.)
 */
public final class ProgramImageDescriptionForm {
  public static final String SUMMARY_IMAGE_DESCRIPTION = "summaryImageDescription";

  public static final ImmutableList<String> FIELD_NAMES =
      ImmutableList.of(SUMMARY_IMAGE_DESCRIPTION);

  private String summaryImageDescription;

  public ProgramImageDescriptionForm() {
    this.summaryImageDescription = "";
  }

  public ProgramImageDescriptionForm(String summaryImageDescription) {
    this.summaryImageDescription = summaryImageDescription;
  }

  public String getSummaryImageDescription() {
    return this.summaryImageDescription;
  }

  @SuppressWarnings("unused") // Used by FormFactory
  public void setSummaryImageDescription(String summaryImageDescription) {
    this.summaryImageDescription = summaryImageDescription;
  }
}
