package views.trustedintermediary;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;

/**
 * The client information form shared by the trusted intermediary add client and edit client pages
 * (Thymeleaf), rendered by {@code trustedintermediary/fragments/ClientInfoForm.html}.
 */
@Data
@Builder
public final class TrustedIntermediaryClientInfoForm {

  // Id of the <form>, "add-ti" or "edit-ti"
  private final String formId;

  // Target of the form's POST
  private final String actionUrl;

  // Show the name suffix select (NAME_SUFFIX_DROPDOWN_ENABLED)
  private final boolean showNameSuffix;

  private final Field firstName;
  private final Field middleName;
  private final Field lastName;
  private final Field nameSuffix;
  private final Field phoneNumber;
  private final Field emailAddress;

  // ISO date
  private final Field dateOfBirth;

  private final Field tiNote;

  // Options of the name suffix select, in ApplicantModel.Suffix order
  private final ImmutableList<SuffixOption> suffixOptions;

  /**
   * The current value of one form field and its validation errors.
   *
   * @param value Value shown in the field, empty when unset
   * @param errors Localized validation error messages, without the "Error: " prefix
   */
  public record Field(String value, ImmutableList<String> errors) {

    public boolean hasErrors() {
      return !errors.isEmpty();
    }
  }

  /**
   * One option of the name suffix select.
   *
   * @param value Submitted value, the ApplicantModel.Suffix name
   * @param label Shown text, the suffix as written ("Jr.")
   * @param selected This is the suffix currently in the field
   */
  public record SuffixOption(String value, String label, boolean selected) {}
}
