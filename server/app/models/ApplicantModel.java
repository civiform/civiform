package models;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.WhenCreated;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import play.data.validation.Constraints;
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

  private static final long serialVersionUID = 1L;
  private ApplicantData applicantData;

  @WhenCreated private Instant whenCreated;

  private String preferredLocale;

  @Constraints.Required @DbJson private String object;
  @ManyToOne private AccountModel account;

  @OneToMany(mappedBy = "applicant")
  private List<ApplicationModel> applications;

  // Applicant information data
  private String firstName;
  private String middleName;
  private String lastName;
  private String emailAddress;
  private String countryCode;
  private String phoneNumber;
  private LocalDate dateOfBirth;

  public ApplicantModel() {
    super();
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
    this.firstName = firstName;
    return this;
  }

  public Optional<String> getFirstName() {
    return Optional.ofNullable(this.firstName);
  }

  public ApplicantModel setMiddleName(String middleName) {
    this.middleName = middleName;
    return this;
  }

  public Optional<String> getMiddleName() {
    return Optional.ofNullable(middleName);
  }

  public ApplicantModel setLastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  public Optional<String> getLastName() {
    return Optional.ofNullable(lastName);
  }

  public ApplicantModel setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
    return this;
  }

  public Optional<String> getEmailAddress() {
    return Optional.ofNullable(emailAddress);
  }

  public ApplicantModel setCountryCode(String countryCode) {
    this.countryCode = countryCode;
    return this;
  }

  public Optional<String> getCountryCode() {
    return Optional.ofNullable(countryCode);
  }

  public ApplicantModel setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
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
    this.dateOfBirth = LocalDate.parse(dateOfBirth, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
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
