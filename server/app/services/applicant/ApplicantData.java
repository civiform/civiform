package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.PathNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import models.ApplicantModel;
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
  private ApplicantModel applicant;

  public ApplicantData() {
    this(EMPTY_APPLICANT_DATA_JSON, null);
  }

  public ApplicantData(ApplicantModel applicant) {
    this(EMPTY_APPLICANT_DATA_JSON, applicant);
  }

  public ApplicantData(String jsonData, ApplicantModel applicant) {
    this(Optional.empty(), jsonData, applicant);
  }

  public ApplicantData(
      Optional<Locale> preferredLocale, String jsonData, ApplicantModel applicant) {
    super(JsonPathProvider.getJsonPath().parse(checkNotNull(jsonData)));
    this.preferredLocale = preferredLocale;
    this.failedUpdates = Optional.empty();
    this.applicant = applicant;
  }

  public ApplicantModel getApplicant() {
    return applicant;
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
    Optional<String> firstName = applicant.getFirstName();
    Optional<String> lastName = applicant.getLastName();
    if (firstName.isEmpty()) {
      // TODO (#5503): Return Optional.empty() when removing the feature flag
      return getApplicantNameAtWellKnownPath();
    }
    /* TODO (#5503): Probably remove this.
     * When the OIDC provider doesn't include the user's name, it inserts
     * the email address as the name. When the CiviFormProfile is merged with
     * the OidcProfile, the applicant name gets set to the OidcProfile name,
     * which can be the email address.
     * https://github.com/civiform/civiform/blob/eaa46a7edb4628b56e298e88aeb2dcfd8ffebeb2/server/app/auth/oidc/applicant/ApplicantProfileCreator.java#L112
     * In this case, we want to check if there is a real name at the
     * Well Known Path (answer to the preseeded question) and use that. If there
     * isn't one there, then go ahead and use the email address as that's all we have.
     * When we remove Well Known Paths in favor of Primary Applicant Info, we can
     * probably remove this, since we expect there to be a name PAI question
     * that will overwrite the email address in the PAI first name column.
     * Additionally, the DurableJob we will create to move data from the WKP
     * to the PAI columns will do this check and overwrite an email address
     * in the first name field.
     */
    if (firstName.get().equals(applicant.getAccount().getEmailAddress())) {
      return Optional.of(getApplicantNameAtWellKnownPath().orElse(firstName.get()));
    }
    return lastName.isEmpty()
        ? Optional.of(firstName.get())
        : Optional.of(String.format("%s, %s", lastName.get(), firstName.get()));
  }

  public Optional<String> getApplicantNameAtWellKnownPath() {
    Optional<String> firstName =
        hasPath(WellKnownPaths.APPLICANT_FIRST_NAME)
            ? readString(WellKnownPaths.APPLICANT_FIRST_NAME)
            : Optional.empty();
    Optional<String> lastName =
        hasPath(WellKnownPaths.APPLICANT_LAST_NAME)
            ? readString(WellKnownPaths.APPLICANT_LAST_NAME)
            : Optional.empty();
    if (firstName.isPresent() && lastName.isPresent()) {
      return Optional.of(String.format("%s, %s", lastName.get(), firstName.get()));
    }
    return firstName;
  }

  // TODO (#5503): Fix up all call sites using these functions to operate on the
  // ApplicantModel fields directly instead. It is still encapsulated here so
  // that we can modify both the Primary Applicant Info columns and
  // Well Known Paths at the same time.
  public Optional<String> getApplicantFirstName() {
    return applicant.getFirstName().or(() -> readString(WellKnownPaths.APPLICANT_FIRST_NAME));
  }

  public Optional<String> getApplicantMiddleName() {
    return applicant.getMiddleName().or(() -> readString(WellKnownPaths.APPLICANT_MIDDLE_NAME));
  }

  public Optional<String> getApplicantLastName() {
    return applicant.getLastName().or(() -> readString(WellKnownPaths.APPLICANT_LAST_NAME));
  }

  public Optional<String> getPhoneNumber() {
    return applicant.getPhoneNumber().or(() -> readString(WellKnownPaths.APPLICANT_PHONE_NUMBER));
  }

  public void setPhoneNumber(String phoneNumber) {
    applicant.setPhoneNumber(phoneNumber);
    putPhoneNumber(WellKnownPaths.APPLICANT_PHONE_NUMBER, phoneNumber);
  }

  public Optional<LocalDate> getDateOfBirth() {
    return applicant
        .getDateOfBirth()
        .or(
            () -> {
              Path dobPath = WellKnownPaths.APPLICANT_DOB;
              Path deprecatedDobPath = WellKnownPaths.APPLICANT_DOB_DEPRECATED;
              return hasPath(dobPath) ? readDate(dobPath) : readDate(deprecatedDobPath);
            });
  }

  public void setDateOfBirth(String dateOfBirth) {
    applicant.setDateOfBirth(dateOfBirth);

    // The new path is underneath the old path, so we have to
    // specifically check the old path exists but not the new one.
    Path newPath = WellKnownPaths.APPLICANT_DOB;
    Path oldPath = WellKnownPaths.APPLICANT_DOB_DEPRECATED;
    Path dobPath = hasPath(oldPath) && !hasPath(newPath) ? oldPath : newPath;
    putDate(dobPath, dateOfBirth);
  }

  public void setUserName(String displayName) {
    String firstName;
    Optional<String> lastName = Optional.empty();
    Optional<String> middleName = Optional.empty();
    List<String> listSplit = Splitter.on(' ').splitToList(displayName);
    switch (listSplit.size()) {
      case 2:
        firstName = listSplit.get(0);
        lastName = Optional.of(listSplit.get(1));
        break;
      case 3:
        firstName = listSplit.get(0);
        middleName = Optional.of(listSplit.get(1));
        lastName = Optional.of(listSplit.get(2));
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
      String firstName, Optional<String> middleName, Optional<String> lastName) {
    applicant.setFirstName(firstName);
    // Empty string will remove it from the model
    applicant.setMiddleName(middleName.orElse(""));
    applicant.setLastName(lastName.orElse(""));

    putString(WellKnownPaths.APPLICANT_FIRST_NAME, firstName);
    putString(WellKnownPaths.APPLICANT_MIDDLE_NAME, middleName.orElse(""));
    putString(WellKnownPaths.APPLICANT_LAST_NAME, lastName.orElse(""));
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

  /**
   * Returns `true` if the answers in `other` match the answers in the current object. Ignores
   * `updated_at` timestamps when comparing answers.
   */
  public boolean isDuplicateOf(ApplicantData other) {
    // Copy data and clear fields not required for comparison.
    ApplicantData thisApplicantData =
        new ApplicantData(this.preferredLocale, this.asJsonString(), this.applicant);
    clearFieldsNotRequiredForComparison(thisApplicantData);
    ApplicantData otherApplicantData =
        new ApplicantData(other.preferredLocale, other.asJsonString(), other.applicant);
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

  public Optional<LocalDate> getDeprecatedDateOfBirth() {
    return readDate(WellKnownPaths.APPLICANT_DOB_DEPRECATED);
  }
}
