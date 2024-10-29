package models;

import auth.oidc.applicant.ApplicantProfileCreator;
import auth.saml.SamlProfileCreator;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import io.ebean.annotation.DbEnumType;
import io.ebean.annotation.DbEnumValue;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.WhenCreated;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import play.data.validation.Constraints;
import services.CfJsonDocumentContext;
import services.applicant.ApplicantData;

/**
 * An EBean mapped class that represents a single applicant.
 *
 * <p>This class primarily exists to handle persistence for its {@code object} property which it
 * uses to hydrate an {@code ApplicantData} instance. See {@code Applicant#getApplicantData}.
 *
 * <p>Note that an {@code Applicant} is one-to-one with an actual human applicant in practice:
 * {@code Account}s and therefore human applicants only have one {@code Applicant} record despite
 * the code specifying a one-to-many relationship. This is technical debt that stems from earlier
 * reasoning about the approach wherein we expected we'd need to create multiple versions of the
 * resident's {@code ApplicantData} for each version they interact with. That isn't the case and
 * their {@code ApplicantData} migrates seamlessly with each additional version but the database
 * schema remains.
 */
@Entity
@Table(name = "applicants")
public class ApplicantModel extends BaseModel {
  public enum Suffix {
    JR("Jr."),
    SR("Sr."),
    I("I"),
    II("II"),
    III("III"),
    IV("IV"),
    V("V");

    private final String suffixName;

    Suffix(String suffixName) {
      this.suffixName = suffixName;
    }

    @DbEnumValue(storage = DbEnumType.VARCHAR)
    public String getValue() {
      return this.suffixName;
    }
  }

  private static final long serialVersionUID = 1L;
  private ApplicantData applicantData;

  @WhenCreated private Instant whenCreated;

  private String preferredLocale;

  @Constraints.Required @DbJson private String object;
  @ManyToOne private AccountModel account;

  @OneToMany(mappedBy = "applicant")
  private List<ApplicationModel> applications;

  // Primary applicant information
  private String firstName;
  private String middleName;
  private String lastName;
  private String suffix;
  private String emailAddress;
  private String countryCode;
  private String phoneNumber;
  private LocalDate dateOfBirth;

  public ApplicantModel() {
    super();
  }

  /** Sets a new applicant data, overwriting any previous value. */
  public void setApplicantData(ApplicantData applicantData) {
    this.applicantData = applicantData;
  }

  public ApplicantData getApplicantData() {
    // This is called both before and after serialization, so we need to handle
    // all three cases - loading from the database, where `object` contains the
    // data and `applicantData` is null, first initialization, where `object`
    // is null and the `applicantData` is also `null`, and in-memory use, where
    // `object` is out-of-date but non-null, and `applicantData` is already valid.

    // Play Ebeans starting at v6.2.0 includes updated Ebeans that fixes a bug we
    // had relied on to mark the json fields as dirty. We now need to manually
    // trigger the dirty flag or the @PrePersist/@PreUpdate annotations don't
    // get triggered.
    io.ebean.DB.markAsDirty(this);

    if (this.applicantData == null && (object != null && !object.isEmpty())) {
      if (preferredLocale == null || preferredLocale.isEmpty()) {
        // Default to English until the applicant specifies their preferred language.
        this.applicantData = new ApplicantData(object);
      } else {
        this.applicantData =
            new ApplicantData(Optional.of(Locale.forLanguageTag(preferredLocale)), object);
      }
    } else if (this.applicantData == null) {
      this.applicantData = new ApplicantData();
    }
    return applicantData;
  }

  @PrePersist
  @PreUpdate
  public void synchronizeObject() {
    this.preferredLocale =
        getApplicantData().hasPreferredLocale()
            ? getApplicantData().preferredLocale().toLanguageTag()
            : null;
    this.object = objectAsJsonString();
  }

  public ApplicantModel setFirstName(String firstName) {
    this.firstName = firstName.isEmpty() || firstName.isBlank() ? null : firstName;
    return this;
  }

  public Optional<String> getFirstName() {
    return Optional.ofNullable(this.firstName);
  }

  public ApplicantModel setMiddleName(String middleName) {
    this.middleName = middleName.isEmpty() || middleName.isBlank() ? null : middleName;
    return this;
  }

  public Optional<String> getMiddleName() {
    return Optional.ofNullable(middleName);
  }

  public ApplicantModel setLastName(String lastName) {
    this.lastName = lastName.isEmpty() || lastName.isBlank() ? null : lastName;
    return this;
  }

  public Optional<String> getLastName() {
    return Optional.ofNullable(lastName);
  }

  public Optional<String> getApplicantName() {
    if (this.firstName == null) {
      return Optional.empty();
    }
    return this.lastName == null
        ? Optional.of(this.firstName)
        : Optional.of(String.format("%s, %s", this.lastName, this.firstName));
  }

  public Optional<String> getApplicantDisplayName() {
    return getApplicantName().or(() -> getEmailAddress());
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
    if (!overwrite && getFirstName().isPresent()) {
      return;
    }
    setFirstName(firstName);
    // Empty string will remove it from the model
    setMiddleName(middleName.orElse(""));
    setLastName(lastName.orElse(""));
    setSuffix(nameSuffix.orElse(""));
  }

  public ApplicantModel setSuffix(String suffix) {
    this.suffix = suffix.isEmpty() || suffix.isBlank() ? null : suffix;
    return this;
  }

  public Optional<String> getSuffix() {
    return Optional.ofNullable(suffix);
  }

  public ApplicantModel setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress.isEmpty() || emailAddress.isBlank() ? null : emailAddress;
    return this;
  }

  public Optional<String> getEmailAddress() {
    return Optional.ofNullable(emailAddress);
  }

  public ApplicantModel setCountryCode(String countryCode) {
    this.countryCode = countryCode.isEmpty() || countryCode.isBlank() ? null : countryCode;
    return this;
  }

  private void setCountryCodeFromPhoneNumber() {
    PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();
    if (PHONE_NUMBER_UTIL.isPossibleNumber(this.phoneNumber, "US")) {
      setCountryCode("US");
    } else if (PHONE_NUMBER_UTIL.isPossibleNumber(this.phoneNumber, "CA")) {
      setCountryCode("CA");
    }
    // Intentionally don't throw an exception if it doesn't match one of the above,
    // since we are not currently using the country code for anything.
  }

  public Optional<String> getCountryCode() {
    return Optional.ofNullable(countryCode);
  }

  /** Save in a similar way to {@link CfJsonDocumentContext#putPhoneNumber} */
  public ApplicantModel setPhoneNumber(String phoneNumber) {
    this.phoneNumber =
        phoneNumber.isEmpty() || phoneNumber.isBlank()
            ? null
            : phoneNumber.replaceAll("[^0-9]", "");
    setCountryCodeFromPhoneNumber();
    return this;
  }

  public Optional<String> getPhoneNumber() {
    return Optional.ofNullable(phoneNumber);
  }

  public ApplicantModel setDateOfBirth(LocalDate dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
    return this;
  }

  public ApplicantModel setDateOfBirth(String dateOfBirth) {
    this.dateOfBirth =
        dateOfBirth.isEmpty() || dateOfBirth.isBlank()
            ? null
            : LocalDate.parse(dateOfBirth, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    return this;
  }

  public Optional<LocalDate> getDateOfBirth() {
    return Optional.ofNullable(dateOfBirth);
  }

  private String objectAsJsonString() {
    return getApplicantData().asJsonString();
  }

  public AccountModel getAccount() {
    return account;
  }

  public ApplicantModel setAccount(AccountModel account) {
    this.account = account;
    return this;
  }

  public ImmutableList<ApplicationModel> getApplications() {
    return ImmutableList.copyOf(this.applications);
  }

  public Instant getWhenCreated() {
    return this.whenCreated;
  }

  @VisibleForTesting
  public ApplicantModel setWhenCreated(Instant whenCreated) {
    this.whenCreated = whenCreated;
    return this;
  }

  /** Convenience to save the model and return it. */
  public ApplicantModel saveAndReturn() {
    save();
    return this;
  }

  @Override
  public void refresh() {
    expireApplicantDataCache();
    super.refresh();
  }

  /**
   * Clears {@code applicantData} so that a new {@link ApplicantData} object will be returned from
   * {@code getApplicantData}.
   *
   * <p>Useful in tests when {@code applicantData} has been stored as an empty object and needs to
   * be udpated with the data stored in {@code object}.
   */
  @VisibleForTesting
  public void expireApplicantDataCache() {
    this.applicantData = null;
  }
}
