package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import models.AccountModel;
import models.ApplicantModel;
import org.junit.Test;
import repository.ResetPostgres;
import services.Path;
import services.WellKnownPaths;
import services.applicant.question.Scalar;

public class ApplicantDataTest extends ResetPostgres {

  @Test
  public void preferredLocale_defaultsToEnglish() {
    ApplicantData data = new ApplicantData();
    assertThat(data.preferredLocale()).isEqualTo(Locale.US);
  }

  @Test
  public void hasPreferredLocale_onlyReturnsTrueIfPreferredLocaleIsSet() {
    ApplicantData data = new ApplicantData();
    assertThat(data.hasPreferredLocale()).isFalse();

    data = new ApplicantData("{\"applicant\":{}}", null);
    assertThat(data.hasPreferredLocale()).isFalse();

    data = new ApplicantData(Optional.empty(), "{\"applicant\":{}}", null);
    assertThat(data.hasPreferredLocale()).isFalse();

    data = new ApplicantData(Optional.of(Locale.FRENCH), "{\"applicant\":{}}", null);
    assertThat(data.hasPreferredLocale()).isTrue();
  }

  private ApplicantData createNewApplicantData() {
    ApplicantModel applicant = new ApplicantModel();

    // TODO (#5503): See the big block comment in ApplicantData#getApplicantName.
    // Once we stop checking the first name against the account email address,
    // we can remove this.
    AccountModel account = new AccountModel();
    account.setEmailAddress("test@email.com");
    account.save();
    applicant.setAccount(account);

    applicant.save();
    return new ApplicantData(applicant);
  }

  @Test
  public void getApplicantName_exists() {
    ApplicantData data = createNewApplicantData();
    data.setUserName("First Last");
    assertThat(data.getApplicantName()).isEqualTo(Optional.of("Last, First"));
  }

  // TODO (#5503): Remove this when we remove this check from ApplicantData#getApplicantName
  @Test
  public void getApplicantName_fallsBackToWKPWhenNameIsEmail() {
    ApplicantData data = createNewApplicantData();
    data.setUserName("test@email.com");
    data.putString(WellKnownPaths.APPLICANT_FIRST_NAME, "first");
    assertThat(data.getApplicantName().get()).isEqualTo("first");
  }

  @Test
  public void getApplicantName_withMiddleName_exists() {
    ApplicantData data = createNewApplicantData();
    data.setUserName("First Middle Last");
    assertThat(data.getApplicantName()).isEqualTo(Optional.of("Last, First"));
    assertThat(data.getApplicant().getMiddleName().get()).isEqualTo("Middle");
  }

  @Test
  public void getApplicantName_noName() {
    ApplicantData data = createNewApplicantData();
    assertThat(data.getApplicantName()).isEmpty();
  }

  @Test
  public void asJsonString() {
    String blob =
        "{\"applicant\":{\"name\":{\"first_name\":\"First\",\"last_name\":\"Last\",\"program_updated_in\":2,\"updated_at\":1690288712068}}}";
    ApplicantData data = new ApplicantData(blob, new ApplicantModel());
    assertThat(data.asJsonString()).isEqualTo(blob);
  }

  @Test
  public void withFailedUpdates() {
    ApplicantData data = createNewApplicantData();
    Path samplePath = Path.create("samplepath").join(Scalar.FIRST_NAME);
    data.setFailedUpdates(ImmutableMap.of(samplePath, "invalid_value"));

    assertThat(data.getFailedUpdates()).isEqualTo(ImmutableMap.of(samplePath, "invalid_value"));
    assertThatThrownBy(data::asJsonString).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void setDateOfBirth_isSuccessful() {
    ApplicantData data = createNewApplicantData();
    String sampleDob = "2022-01-05";
    data.setDateOfBirth(sampleDob);
    assertThat(data.getDateOfBirth().get()).isEqualTo(sampleDob);
    assertThat(data.asJsonString())
        .isEqualTo("{\"applicant\":{\"applicant_date_of_birth\":{\"date\":1641340800000}}}");
  }

  @Test
  public void setDateOfBirth_updatesAtNewPath() {
    ApplicantData data = createNewApplicantData();
    data.putDate(WellKnownPaths.APPLICANT_DOB, "1999-01-01");
    data.setDateOfBirth("2022-01-05");
    assertThat(data.asJsonString())
        .isEqualTo("{\"applicant\":{\"applicant_date_of_birth\":{\"date\":1641340800000}}}");
  }

  @Test
  public void setDateOfBirth_updatesAtOldPath() {
    ApplicantData data = createNewApplicantData();
    data.putDate(WellKnownPaths.APPLICANT_DOB_DEPRECATED, "1999-01-01");
    data.setDateOfBirth("2022-01-05");
    assertThat(data.asJsonString())
        .isEqualTo("{\"applicant\":{\"applicant_date_of_birth\":1641340800000}}");
  }

  @Test
  public void getDateOfBirth_canHandleDeprecatedDobPath() {
    ApplicantData data = createNewApplicantData();
    String sampleDob = "2022-01-05";
    data.putDate(WellKnownPaths.APPLICANT_DOB_DEPRECATED, sampleDob);
    assertThat(data.getDateOfBirth().get()).isEqualTo(sampleDob);
    assertThat(data.asJsonString())
        .isEqualTo("{\"applicant\":{\"applicant_date_of_birth\":1641340800000}}");
  }

  @Test
  public void getDateOfBirth_isEmptyWhenNotSet() {
    ApplicantData applicantData = createNewApplicantData();
    assertThat(applicantData.getDateOfBirth()).isEqualTo(Optional.empty());
  }

  @Test
  public void isDuplicate_returnsTrue() {
    ApplicantData data1 =
        new ApplicantData(
            "{\"applicant\":{\"name\":{\"first_name\":\"First\",\"last_name\":\"Last\",\"program_updated_in\":2,\"updated_at\":1690288712068}}}",
            new ApplicantModel());
    ApplicantData data2 =
        new ApplicantData(
            "{\"applicant\":{\"name\":{\"first_name\":\"First\",\"last_name\":\"Last\",\"program_updated_in\":2,\"updated_at\":1690288712068}}}",
            new ApplicantModel());

    assertThat(data1.isDuplicateOf(data2)).isTrue();
    assertThat(data2.isDuplicateOf(data1)).isTrue();
  }

  @Test
  public void isDuplicate_returnsFalse() {
    ApplicantData data1 =
        new ApplicantData(
            "{\"applicant\":{\"name\":{\"first_name\":\"First\",\"last_name\":\"Last\",\"program_updated_in\":2,\"updated_at\":1690288712068}}}",
            new ApplicantModel());
    ApplicantData data2 =
        new ApplicantData(
            "{\"applicant\":{\"name\":{\"first_name\":\"User\",\"last_name\":\"Name\",\"program_updated_in\":2,\"updated_at\":1690293297676}}}",
            new ApplicantModel());

    assertThat(data1.isDuplicateOf(data2)).isFalse();
    assertThat(data2.isDuplicateOf(data1)).isFalse();
  }

  @Test
  public void isDuplicateWithMetadata_returnsTrue() {
    // The only difference is the timestamp in the `updated_at` field.
    //
    // Since this field is not settable by any API, we use the JSON representation to specify the
    // applicant data.
    ApplicantData data1 =
        new ApplicantData(
            "{\"applicant\":{\"name\":{\"first_name\":\"First\",\"last_name\":\"Last\",\"program_updated_in\":2,\"updated_at\":1690288712068}}}",
            new ApplicantModel());
    ApplicantData data2 =
        new ApplicantData(
            "{\"applicant\":{\"name\":{\"first_name\":\"First\",\"last_name\":\"Last\",\"program_updated_in\":2,\"updated_at\":1690293297676}}}",
            new ApplicantModel());

    assertThat(data1.isDuplicateOf(data2)).isTrue();
    assertThat(data2.isDuplicateOf(data1)).isTrue();
  }

  // TODO (#5503): Remove this when we remove the feature flag
  @Test
  public void fallsBackToWellKnownPathWhenPrimaryApplicantInfoDoesNotExist() {
    ApplicantData data = createNewApplicantData();
    ApplicantModel applicant = data.getApplicant();
    data.putString(WellKnownPaths.APPLICANT_FIRST_NAME, "Jean");
    data.putString(WellKnownPaths.APPLICANT_MIDDLE_NAME, "Luc");
    data.putString(WellKnownPaths.APPLICANT_LAST_NAME, "Picard");
    data.putPhoneNumber(WellKnownPaths.APPLICANT_PHONE_NUMBER, "5038234000");
    data.putDate(WellKnownPaths.APPLICANT_DOB, "2305-07-13");
    applicant.save();

    assertThat(data.getApplicantFirstName().get()).isEqualTo("Jean");
    assertThat(data.getApplicantMiddleName().get()).isEqualTo("Luc");
    assertThat(data.getApplicantLastName().get()).isEqualTo("Picard");
    assertThat(data.getApplicantName().get()).isEqualTo("Picard, Jean");
    assertThat(data.getPhoneNumber().get()).isEqualTo("5038234000");
    assertThat(data.getDateOfBirth().get()).isEqualTo("2305-07-13");

    data.setUserName("Kathryn", Optional.empty(), Optional.of("Janeway"), Optional.empty());
    data.setPhoneNumber("(206) 684-2489");
    data.setDateOfBirth("2328-05-20");
    applicant.save();

    assertThat(data.getApplicantFirstName().get()).isEqualTo("Kathryn");
    assertThat(data.getApplicantMiddleName()).isEmpty();
    assertThat(data.getApplicantLastName().get()).isEqualTo("Janeway");
    assertThat(data.getApplicantName().get()).isEqualTo("Janeway, Kathryn");
    assertThat(data.getPhoneNumber().get()).isEqualTo("2066842489");
    assertThat(data.getDateOfBirth().get()).isEqualTo("2328-05-20");

    assertThat(applicant.getFirstName().get()).isEqualTo("Kathryn");
    assertThat(applicant.getMiddleName()).isEmpty();
    assertThat(applicant.getLastName().get()).isEqualTo("Janeway");
    assertThat(applicant.getPhoneNumber().get()).isEqualTo("2066842489");
    assertThat(applicant.getDateOfBirth().get()).isEqualTo("2328-05-20");
  }
}
