package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.PathNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nullable;
import services.CfJsonDocumentContext;
import services.LocalizedStrings;
import services.Path;
import services.WellKnownPaths;

/**
 * Brokers access to the answer data for a specific applicant across versions.
 *
 * <p>Instances are hydrated and persisted through {@code models.Applicant}.
 *
 * <p>While the underlying storage format is JSON, this class presents a read/write interface in
 * terms of CiviForm's domain semantics, such as {@code Path}, rather than raw JSON paths and
 * values.
 *
 * <p>When extending this class, seek to avoid leaking details of the JSON format to the code that
 * consumes it and prefer higher-level objects over primitives in method signatures.
 */
public class ApplicantData extends CfJsonDocumentContext {

  private static final String APPLICANT = "applicant";
  public static final Path APPLICANT_PATH = Path.create(APPLICANT);
  private static final String EMPTY_APPLICANT_DATA_JSON =
      String.format("{ \"%s\": {} }", APPLICANT);
  private Optional<Locale> preferredLocale;

  private Optional<ImmutableMap<Path, String>> failedUpdates;

  public ApplicantData() {
    this(EMPTY_APPLICANT_DATA_JSON);
  }

  public ApplicantData(String jsonData) {
    this(Optional.empty(), jsonData);
  }

  public ApplicantData(Optional<Locale> preferredLocale, String jsonData) {
    super(JsonPathProvider.getJsonPath().parse(checkNotNull(jsonData)));
    this.preferredLocale = preferredLocale;
    this.failedUpdates = Optional.empty();
  }

  /** Returns true if this applicant has set their preferred locale, and false otherwise. */
  public boolean hasPreferredLocale() {
    return this.preferredLocale.isPresent();
  }

  /** Returns this applicant's preferred locale if it is set, or the default locale if not set. */
  public Locale preferredLocale() {
    return this.preferredLocale.orElse(LocalizedStrings.DEFAULT_LOCALE);
  }

  public void setPreferredLocale(Locale locale) {
    checkLocked();
    this.preferredLocale = Optional.of(locale);
  }

  public Optional<String> getApplicantName() {
    if (!hasPath(WellKnownPaths.APPLICANT_FIRST_NAME)) {
      return Optional.empty();
    }
    String firstName = readString(WellKnownPaths.APPLICANT_FIRST_NAME).get();
    if (hasPath(WellKnownPaths.APPLICANT_LAST_NAME)) {
      String lastName = readString(WellKnownPaths.APPLICANT_LAST_NAME).get();
      return Optional.of(String.format("%s, %s", lastName, firstName));
    }
    return Optional.of(firstName);
  }

  public Optional<String> getApplicantFirstName() {
    return readString(WellKnownPaths.APPLICANT_FIRST_NAME);
  }

  public Optional<String> getApplicantMiddleName() {
    return readString(WellKnownPaths.APPLICANT_MIDDLE_NAME);
  }

  public Optional<String> getApplicantLastName() {
    return readString(WellKnownPaths.APPLICANT_LAST_NAME);
  }

  /** Updates the TI client name in the Applicant table */
  public void updateUserName(
      String firstName, @Nullable String middleName, @Nullable String lastName) {
    putString(WellKnownPaths.APPLICANT_FIRST_NAME, firstName);
    if (middleName != null) {
      putString(WellKnownPaths.APPLICANT_MIDDLE_NAME, middleName);
    }
    if (lastName != null) {
      putString(WellKnownPaths.APPLICANT_LAST_NAME, lastName);
    }
  }

  public Optional<String> getPhoneNumber() {
    return readString(WellKnownPaths.APPLICANT_PHONE_NUMBER);
  }

  public void setPhoneNumber(String phoneNumber) {
    putPhoneNumber(WellKnownPaths.APPLICANT_PHONE_NUMBER, phoneNumber);
  }

  public void setUserName(String displayName) {
    String firstName;
    String lastName = null;
    String middleName = null;
    List<String> listSplit = Splitter.on(' ').splitToList(displayName);
    switch (listSplit.size()) {
      case 2:
        firstName = listSplit.get(0);
        lastName = listSplit.get(1);
        break;
      case 3:
        firstName = listSplit.get(0);
        middleName = listSplit.get(1);
        lastName = listSplit.get(2);
        break;
      case 1:
        // fallthrough
      default:
        // Too many names - put them all in first name.
        firstName = displayName;
    }
    setUserName(firstName, middleName, lastName);
  }

  public void setUserName(
      String firstName, @Nullable String middleName, @Nullable String lastName) {
    if (!hasPath(WellKnownPaths.APPLICANT_FIRST_NAME)) {
      putString(WellKnownPaths.APPLICANT_FIRST_NAME, firstName);
    }
    if (middleName != null && !hasPath(WellKnownPaths.APPLICANT_MIDDLE_NAME)) {
      putString(WellKnownPaths.APPLICANT_MIDDLE_NAME, middleName);
    }
    if (lastName != null && !hasPath(WellKnownPaths.APPLICANT_LAST_NAME)) {
      putString(WellKnownPaths.APPLICANT_LAST_NAME, lastName);
    }
  }

  @Override
  public String asJsonString() {
    if (!getFailedUpdates().isEmpty()) {
      throw new IllegalStateException("data cannot be serialized since there were failed updates");
    }
    return super.asJsonString();
  }

  /**
   * Sets updates that couldn't be applied to the {@link ApplicantData}.
   *
   * @param updates Keys are the paths that couldn't be updated and values are the raw input that
   *     couldn't be applied.
   */
  public void setFailedUpdates(ImmutableMap<Path, String> updates) {
    checkLocked();
    failedUpdates = Optional.of(checkNotNull(updates));
  }

  /**
   * A map of updates that couldn't be applied to the {@link ApplicantData}. Keys are the paths that
   * couldn't be updated and values are the raw input that couldn't be applied.
   */
  public ImmutableMap<Path, String> getFailedUpdates() {
    return failedUpdates.orElse(ImmutableMap.of());
  }

  public boolean updateDidFailAt(Path path) {
    return getFailedUpdates().containsKey(path);
  }

  public Optional<LocalDate> getDateOfBirth() {
    Path dobPath = WellKnownPaths.APPLICANT_DOB;
    if (!hasPath(dobPath)) {
      return getDeprecatedDateOfBirth();
    }
    return readDate(dobPath);
  }

  public Optional<LocalDate> getDeprecatedDateOfBirth() {
    return readDate(WellKnownPaths.APPLICANT_DOB_DEPRECATED);
  }

  public void setDateOfBirth(String dateOfBirth) {
    Path deprecatedDobPath = WellKnownPaths.APPLICANT_DOB_DEPRECATED;
    if (hasPath(deprecatedDobPath)) {
      putDate(deprecatedDobPath, dateOfBirth);
    } else {
      putDate(WellKnownPaths.APPLICANT_DOB, dateOfBirth);
    }
  }

  /**
   * Returns `true` if the answers in `other` match the answers in the current object. Ignores
   * `updated_at` timestamps when comparing answers.
   */
  public boolean isDuplicateOf(ApplicantData other) {
    // Copy data and clear fields not required for comparison.
    ApplicantData thisApplicantData = new ApplicantData(this.preferredLocale, this.asJsonString());
    clearFieldsNotRequiredForComparison(thisApplicantData);
    ApplicantData otherApplicantData =
        new ApplicantData(other.preferredLocale, other.asJsonString());
    clearFieldsNotRequiredForComparison(otherApplicantData);

    return thisApplicantData.asJsonString().equals(otherApplicantData.asJsonString());
  }

  private static void clearFieldsNotRequiredForComparison(ApplicantData applicantData) {
    // The `updated_at` timestamp for an answer should not be considered when
    // comparing answers.
    try {
      // The ".." in the path scans the entire document.
      applicantData.getDocumentContext().set("$..updated_at", 0);
    } catch (PathNotFoundException unused) {
      // Metadata may be missing in unit tests. No harm, no foul.
    }
  }
}
