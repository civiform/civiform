package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.oidc.applicant.ApplicantProfileCreator;
import auth.saml.SamlProfileCreator;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.TypeRef;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import models.ApplicantModel;
import models.ApplicantModel.Suffix;
import services.CfJsonDocumentContext;
import services.LocalizedStrings;
import services.Path;
import services.WellKnownPaths;
import services.applicant.question.Scalar;
import services.geo.ServiceAreaInclusion;

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

  /**
   * Checks if the given input string represents a valid suffix from Suffix enum.
   *
   * @param input The string to be checked for suffix validity.
   * @return 'true' if the input string matches a suffix defined in Suffix enum, 'false' otherwise.
   */
  private static boolean isSuffix(String input) {
    return Arrays.stream(Suffix.values()).anyMatch(suffix -> suffix.getValue().equals(input));
  }

  /**
   * Parses a name string to extract the first, middle, and last names, if they exists, and sets
   * those fields. This function will NOT overwrite any existing name data if the first name already
   * exists. This is because this function is used by {@link ApplicantProfileCreator} and {@link
   * SamlProfileCreator} and we do not want it to overwrite the name upon login.
   *
   * @param displayName A string that contains the applicant's name, with first, middle, last, and
   *     suffix separated by spaces. May provide only first name or only first last.
   */
  public void setUserName(String displayName) {
    String firstName;
    Optional<String> lastName = Optional.empty();
    Optional<String> middleName = Optional.empty();
    Optional<String> nameSuffix = Optional.empty();
    List<String> listSplit = Splitter.on(' ').splitToList(displayName);
    switch (listSplit.size()) {
      case 2:
        firstName = listSplit.get(0);
        lastName = Optional.of(listSplit.get(1));
        break;
      case 4:
        firstName = listSplit.get(0);
        middleName = Optional.of(listSplit.get(1));
        lastName = Optional.of(listSplit.get(2));
        nameSuffix = Optional.of(listSplit.get(3));
        break;
      case 3:
        firstName = listSplit.get(0);
        if (isSuffix(listSplit.get(2))) {
          lastName = Optional.of(listSplit.get(1));
          nameSuffix = Optional.of(listSplit.get(2));
        } else {
          middleName = Optional.of(listSplit.get(1));
          lastName = Optional.of(listSplit.get(2));
        }
        break;
      case 1:
        // fallthrough
      default:
        // Too many names - put them all in first name.
        firstName = displayName;
    }
    setUserName(firstName, middleName, lastName, nameSuffix, false);
  }

  // By default, overwrite name fields if data exists in them
  public void setUserName(
      String firstName,
      Optional<String> middleName,
      Optional<String> lastName,
      Optional<String> nameSuffix) {
    setUserName(firstName, middleName, lastName, nameSuffix, true);
  }

  /**
   * Sets the first, middle, and last name fields.
   *
   * @param firstName First name of applicant
   * @param middleName Middle name of applicant
   * @param lastName Last name of applicant
   * @param overwrite When false, if first name already exists, do not update fields and return
   *     unchanged.
   */
  public void setUserName(
      String firstName,
      Optional<String> middleName,
      Optional<String> lastName,
      Optional<String> nameSuffix,
      boolean overwrite) {
    if (!overwrite && applicant.getFirstName().isPresent()) {
      return;
    }
    applicant.setFirstName(firstName);
    // Empty string will remove it from the model
    applicant.setMiddleName(middleName.orElse(""));
    applicant.setLastName(lastName.orElse(""));
    applicant.setSuffix(nameSuffix.orElse(""));
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

  /**
   * Puts an array at a given path, building parent objects as needed.
   *
   * @param path the {@link Path} where the array should be added.
   * @param entityNames a {@link List} containing service area results.
   */
  public void putServiceAreaInclusionEntities(
      Path path, ImmutableList<ServiceAreaInclusion> entityNames) {
    if (entityNames.isEmpty()) {
      putArray(path, ImmutableList.of());
    } else {
      for (int i = 0; i < entityNames.size(); i++) {
        putString(
            path.atIndex(i).join(Scalar.SERVICE_AREA_ID), entityNames.get(i).getServiceAreaId());
        putString(
            path.atIndex(i).join(Scalar.SERVICE_AREA_STATE), entityNames.get(i).getState().name());
        putLong(path.atIndex(i).join(Scalar.TIMESTAMP), entityNames.get(i).getTimeStamp());
      }
    }
  }

  /**
   * Attempt to read a list at the given {@link Path}. Returns {@code Optional#empty} if the path
   * does not exist or a value other than an {@link ImmutableList} of {@link ServiceAreaInclusion}
   * is found.
   *
   * @param path the {@link Path} to the list
   * @return an Optional containing an ImmutableList<ServiceAreaInclusion>
   */
  public Optional<ImmutableList<ServiceAreaInclusion>> readServiceAreaList(Path path) {
    return readList(
        path.safeWithoutArrayReference(), new TypeRef<ImmutableList<ServiceAreaInclusion>>() {});
  }
}
