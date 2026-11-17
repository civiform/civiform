package mapping.trustedintermediary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import forms.TiClientInfoForm;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import models.AccountModel;
import models.ApplicantModel;
import org.junit.Before;
import org.junit.Test;
import play.data.Form;
import play.data.validation.ValidationError;
import play.i18n.Lang;
import play.i18n.Messages;
import services.DateConverter;
import services.ti.TrustedIntermediaryService;
import views.trustedintermediary.TrustedIntermediaryClientInfoForm;
import views.trustedintermediary.TrustedIntermediaryClientInfoForm.Field;
import views.trustedintermediary.TrustedIntermediaryClientInfoForm.SuffixOption;

public final class TrustedIntermediaryClientInfoFormMapperTest {

  private static final String FORM_ID = "edit-ti";
  private static final String ACTION_URL = "/admin/tiDash/editClient/7";

  private TrustedIntermediaryClientInfoFormMapper mapper;
  private DateConverter dateConverter;
  private Messages messages;

  @Before
  public void setup() {
    mapper = new TrustedIntermediaryClientInfoFormMapper();
    dateConverter = new DateConverter(Clock.systemUTC());
    messages = stubMessagesApi().preferred(ImmutableSet.of(Lang.defaultLang()));
  }

  private static Field emptyField() {
    return new Field("", ImmutableList.of());
  }

  /** A client account whose representative applicant has every field filled in. */
  private static AccountModel completeAccount() {
    ApplicantModel applicant = mock(ApplicantModel.class);
    when(applicant.getFirstName()).thenReturn(Optional.of("Ada"));
    when(applicant.getMiddleName()).thenReturn(Optional.of("King"));
    when(applicant.getLastName()).thenReturn(Optional.of("Lovelace"));
    when(applicant.getSuffix()).thenReturn(Optional.of("SR"));
    when(applicant.getPhoneNumber()).thenReturn(Optional.of("2065550123"));
    when(applicant.getEmailAddress()).thenReturn(Optional.of("ada@example.com"));
    when(applicant.getDateOfBirth()).thenReturn(Optional.of(LocalDate.of(1815, 12, 10)));

    AccountModel account = mock(AccountModel.class);
    when(account.representativeApplicant()).thenReturn(Optional.of(applicant));
    when(account.getTiNote()).thenReturn("Prefers email");

    return account;
  }

  /** A submitted form carrying the given values and no errors, as Play would bind it. */
  private static Form<TiClientInfoForm> submittedForm(TiClientInfoForm data) {
    @SuppressWarnings("unchecked")
    Form<TiClientInfoForm> form = mock(Form.class);
    when(form.value()).thenReturn(Optional.of(data));
    when(form.errors(anyString())).thenReturn(ImmutableList.of());

    return form;
  }

  private TrustedIntermediaryClientInfoForm map(
      Optional<AccountModel> account, Optional<Form<TiClientInfoForm>> submittedForm) {
    return mapper.map(
        FORM_ID,
        ACTION_URL,
        account,
        submittedForm,
        /* showNameSuffix= */ false,
        dateConverter,
        messages);
  }

  @Test
  public void map_setsFormIdAndActionUrl() {
    TrustedIntermediaryClientInfoForm form =
        map(/* account= */ Optional.empty(), /* submittedForm= */ Optional.empty());

    assertThat(form.getFormId()).isEqualTo(FORM_ID);
    assertThat(form.getActionUrl()).isEqualTo(ACTION_URL);
  }

  @Test
  public void map_setsShowNameSuffix() {
    TrustedIntermediaryClientInfoForm form =
        mapper.map(
            FORM_ID,
            ACTION_URL,
            /* account= */ Optional.empty(),
            /* submittedForm= */ Optional.empty(),
            /* showNameSuffix= */ true,
            dateConverter,
            messages);

    assertThat(form.isShowNameSuffix()).isTrue();
  }

  @Test
  public void map_withoutAccountOrSubmittedForm_leavesFieldsEmpty() {
    TrustedIntermediaryClientInfoForm form =
        map(/* account= */ Optional.empty(), /* submittedForm= */ Optional.empty());

    assertThat(
            ImmutableList.of(
                form.getFirstName(),
                form.getMiddleName(),
                form.getLastName(),
                form.getNameSuffix(),
                form.getPhoneNumber(),
                form.getEmailAddress(),
                form.getDateOfBirth(),
                form.getTiNote()))
        .containsOnly(emptyField());
  }

  @Test
  public void map_withAccount_fillsFieldsFromRepresentativeApplicant() {
    TrustedIntermediaryClientInfoForm form =
        map(Optional.of(completeAccount()), /* submittedForm= */ Optional.empty());

    assertThat(form.getFirstName().value()).isEqualTo("Ada");
    assertThat(form.getMiddleName().value()).isEqualTo("King");
    assertThat(form.getLastName().value()).isEqualTo("Lovelace");
    assertThat(form.getNameSuffix().value()).isEqualTo("SR");
    assertThat(form.getPhoneNumber().value()).isEqualTo("2065550123");
    assertThat(form.getEmailAddress().value()).isEqualTo("ada@example.com");
  }

  @Test
  public void map_withAccount_formatsDateOfBirthAsIsoDate() {
    TrustedIntermediaryClientInfoForm form =
        map(Optional.of(completeAccount()), /* submittedForm= */ Optional.empty());

    assertThat(form.getDateOfBirth().value()).isEqualTo("1815-12-10");
  }

  @Test
  public void map_withAccount_fillsTiNoteFromAccount() {
    TrustedIntermediaryClientInfoForm form =
        map(Optional.of(completeAccount()), /* submittedForm= */ Optional.empty());

    assertThat(form.getTiNote().value()).isEqualTo("Prefers email");
  }

  @Test
  public void map_withAccountWithoutApplicant_leavesApplicantFieldsEmpty() {
    AccountModel account = mock(AccountModel.class);
    when(account.representativeApplicant()).thenReturn(Optional.empty());
    when(account.getTiNote()).thenReturn("Prefers email");

    TrustedIntermediaryClientInfoForm form =
        map(Optional.of(account), /* submittedForm= */ Optional.empty());

    assertThat(form.getFirstName()).isEqualTo(emptyField());
    assertThat(form.getDateOfBirth()).isEqualTo(emptyField());
    assertThat(form.getTiNote().value()).isEqualTo("Prefers email");
  }

  @Test
  public void map_withAccount_nullTiNoteBecomesEmpty() {
    AccountModel account = mock(AccountModel.class);
    when(account.representativeApplicant()).thenReturn(Optional.empty());
    when(account.getTiNote()).thenReturn(null);

    TrustedIntermediaryClientInfoForm form =
        map(Optional.of(account), /* submittedForm= */ Optional.empty());

    assertThat(form.getTiNote()).isEqualTo(emptyField());
  }

  @Test
  public void map_withSubmittedForm_replacesStoredValuesWithSubmittedOnes() {
    TiClientInfoForm data = new TiClientInfoForm();
    data.setFirstName("Grace");
    data.setMiddleName("");
    data.setLastName("Hopper");
    data.setNameSuffix("JR");
    data.setPhoneNumber("2065550199");
    data.setEmailAddress("grace@example.com");
    data.setDob("1906-12-09");
    data.setTiNote("New note");

    TrustedIntermediaryClientInfoForm form =
        map(Optional.of(completeAccount()), Optional.of(submittedForm(data)));

    assertThat(form.getFirstName().value()).isEqualTo("Grace");
    assertThat(form.getMiddleName().value()).isEmpty();
    assertThat(form.getLastName().value()).isEqualTo("Hopper");
    assertThat(form.getNameSuffix().value()).isEqualTo("JR");
    assertThat(form.getPhoneNumber().value()).isEqualTo("2065550199");
    assertThat(form.getEmailAddress().value()).isEqualTo("grace@example.com");
    assertThat(form.getDateOfBirth().value()).isEqualTo("1906-12-09");
    assertThat(form.getTiNote().value()).isEqualTo("New note");
  }

  @Test
  public void map_withSubmittedForm_unboundValueBecomesEmpty() {
    TrustedIntermediaryClientInfoForm form =
        map(Optional.of(completeAccount()), Optional.of(submittedForm(new TiClientInfoForm())));

    assertThat(form.getFirstName()).isEqualTo(emptyField());
  }

  @Test
  public void map_withSubmittedFormErrors_setsErrorsOnTheirField() {
    Form<TiClientInfoForm> submittedForm = submittedForm(new TiClientInfoForm());
    when(submittedForm.errors(TrustedIntermediaryService.FORM_FIELD_NAME_DOB))
        .thenReturn(
            ImmutableList.of(
                new ValidationError(
                    TrustedIntermediaryService.FORM_FIELD_NAME_DOB, "Date of birth required")));

    TrustedIntermediaryClientInfoForm form =
        map(/* account= */ Optional.empty(), Optional.of(submittedForm));

    assertThat(form.getDateOfBirth().errors()).containsExactly("Date of birth required");
    assertThat(form.getFirstName().hasErrors()).isFalse();
  }

  @Test
  public void map_buildsSuffixOptionsInEnumOrder() {
    TrustedIntermediaryClientInfoForm form =
        map(/* account= */ Optional.empty(), /* submittedForm= */ Optional.empty());

    assertThat(form.getSuffixOptions())
        .containsExactly(
            new SuffixOption("JR", "Jr.", false),
            new SuffixOption("SR", "Sr.", false),
            new SuffixOption("I", "I", false),
            new SuffixOption("II", "II", false),
            new SuffixOption("III", "III", false),
            new SuffixOption("IV", "IV", false),
            new SuffixOption("V", "V", false));
  }

  @Test
  public void map_selectsSuffixOptionMatchingTheSuffixValue() {
    TrustedIntermediaryClientInfoForm form =
        map(Optional.of(completeAccount()), /* submittedForm= */ Optional.empty());

    assertThat(form.getSuffixOptions())
        .filteredOn(SuffixOption::selected)
        .extracting(SuffixOption::value)
        .containsExactly("SR");
  }
}
