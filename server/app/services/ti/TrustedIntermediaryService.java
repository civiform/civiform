package services.ti;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import forms.AddApplicantToTrustedIntermediaryGroupForm;
import forms.EditTiClientInfoForm;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import models.AccountModel;
import models.ApplicantModel;
import models.TrustedIntermediaryGroupModel;
import play.data.Form;
import repository.AccountRepository;
import repository.SearchParameters;
import services.DateConverter;
import services.applicant.exception.ApplicantNotFoundException;

/**
 * Service Class for TrustedIntermediaryController.
 *
 * <p>Civiform TrustedIntermediaries have the ability to add Clients and apply to Civiform programs
 * on their behalf. The first step to this process is add a Client by providing their First and Last
 * name, their email address and their Date of birth.
 *
 * <p>This class performs the validation of the request form passed from the Controller and if the
 * form has no-errors, it sends back the form object.
 *
 * <p>If any of the validation fails, it sends the form object with all of its errors.
 */
public final class TrustedIntermediaryService {
  private final AccountRepository accountRepository;
  private final DateConverter dateConverter;
  public static final String FORM_FIELD_NAME_FIRST_NAME = "firstName";
  public static final String FORM_FIELD_NAME_LAST_NAME = "lastName";
  public static final String FORM_FIELD_NAME_EMAIL_ADDRESS = "emailAddress";
  public static final String FORM_FIELD_NAME_DOB = "dob";
  public static final String FORM_FIELD_NAME_PHONE = "phoneNumber";
  public static final String FORM_FIELD_NAME_MIDDLE_NAME = "middleName";
  public static final String FORM_FIELD_NAME_TI_NOTES = "tiNote";
  private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

  @Inject
  public TrustedIntermediaryService(
      AccountRepository accountRepository, DateConverter dateConverter) {
    this.accountRepository = Preconditions.checkNotNull(accountRepository);
    this.dateConverter = Preconditions.checkNotNull(dateConverter);
  }

  public Form<AddApplicantToTrustedIntermediaryGroupForm> addNewClient(
      Form<AddApplicantToTrustedIntermediaryGroupForm> form,
      TrustedIntermediaryGroupModel trustedIntermediaryGroup) {
    form = validateFirstName(form);
    form = validateLastName(form);
    form = validateDateOfBirthForAddApplicant(form);
    if (form.hasErrors()) {
      return form;
    }
    try {
      accountRepository.createNewApplicantForTrustedIntermediaryGroup(
          form.get(), trustedIntermediaryGroup);
    } catch (EmailAddressExistsException e) {
      return form.withError(
          FORM_FIELD_NAME_EMAIL_ADDRESS,
          "Email address already in use. Cannot create applicant if an account already"
              + " exists.");
    }
    return form;
  }

  private Form<AddApplicantToTrustedIntermediaryGroupForm> validateFirstName(
      Form<AddApplicantToTrustedIntermediaryGroupForm> form) {
    if (Strings.isNullOrEmpty(form.value().get().getFirstName())) {
      return form.withError(FORM_FIELD_NAME_FIRST_NAME, "First name required");
    }
    return form;
  }

  private Form<AddApplicantToTrustedIntermediaryGroupForm> validateLastName(
      Form<AddApplicantToTrustedIntermediaryGroupForm> form) {
    if (Strings.isNullOrEmpty(form.value().get().getLastName())) {
      return form.withError(FORM_FIELD_NAME_LAST_NAME, "Last name required");
    }
    return form;
  }

  private Form<AddApplicantToTrustedIntermediaryGroupForm> validateDateOfBirthForAddApplicant(
      Form<AddApplicantToTrustedIntermediaryGroupForm> form) {
    Optional<String> errorMessage = validateDateOfBirth(form.value().get().getDob());
    if (errorMessage.isPresent()) {
      return form.withError(FORM_FIELD_NAME_DOB, errorMessage.get());
    }
    return form;
  }

  private Optional<String> validateDateOfBirth(String dob) {
    if (Strings.isNullOrEmpty(dob)) {
      return Optional.of("Date of Birth required");
    }
    final LocalDate currentDob;
    try {
      currentDob = dateConverter.parseIso8601DateToLocalDate(dob);
    } catch (DateTimeParseException e) {
      return Optional.of("Date of Birth must be in MM/dd/yyyy format");
    }
    if (!currentDob.isBefore(dateConverter.getCurrentDateForZoneId())) {
      return Optional.of("Date of Birth should be in the past");
    }
    if (currentDob.isBefore(dateConverter.getCurrentDateForZoneId().minusYears(150))) {
      return Optional.of("Date of Birth should be less than 150 years ago");
    }
    return Optional.empty();
  }

  private Form<EditTiClientInfoForm> validateFirstNameForEditClient(
      Form<EditTiClientInfoForm> form) {
    if (Strings.isNullOrEmpty(form.value().get().getFirstName())) {
      return form.withError(FORM_FIELD_NAME_FIRST_NAME, "First name required");
    }
    return form;
  }

  private Form<EditTiClientInfoForm> validateLastNameForEditClient(
      Form<EditTiClientInfoForm> form) {
    if (Strings.isNullOrEmpty(form.value().get().getLastName())) {
      return form.withError(FORM_FIELD_NAME_LAST_NAME, "Last name required");
    }
    return form;
  }

  private Boolean hasEmailChanged(String newEmail, AccountModel account) {
    return !newEmail.equals(account.getEmailAddress());
  }

  private Form<EditTiClientInfoForm> validatePhoneNumber(Form<EditTiClientInfoForm> form) {
    String phoneNumber = form.value().get().getPhoneNumber();
    if (Strings.isNullOrEmpty(phoneNumber)) {
      return form;
    }
    // removes space, '(',')' and '-' from the phone number
    phoneNumber = phoneNumber.replaceAll("[()-[\\s]]", "");
    if (!phoneNumber.matches("[0-9]+")) {
      return form.withError(FORM_FIELD_NAME_PHONE, "A phone number must contain only digits");
    }
    if (phoneNumber.length() != 10) {
      return form.withError(FORM_FIELD_NAME_PHONE, "A phone number must contain only 10 digits");
    }
    try {
      Phonenumber.PhoneNumber phonenumber = PHONE_NUMBER_UTIL.parse(phoneNumber, "US");
      if (!PHONE_NUMBER_UTIL.isValidNumber(phonenumber)) {
        return form.withError(FORM_FIELD_NAME_PHONE, "This phone number is not valid");
      }
    } catch (NumberParseException e) {
      throw new RuntimeException(e);
    }
    return form;
  }

  private Form<EditTiClientInfoForm> validateDateOfBirth(Form<EditTiClientInfoForm> form) {
    Optional<String> errorMessage = validateDateOfBirth(form.value().get().getDob());
    if (errorMessage.isPresent()) {
      return form.withError(FORM_FIELD_NAME_DOB, errorMessage.get());
    }
    return form;
  }

  private Form<EditTiClientInfoForm> validateEmailAddress(
      Form<EditTiClientInfoForm> form, AccountModel currentAccount) {
    String newEmail = form.get().getEmailAddress();
    // email addresses not a requirement for TI Client
    if (Strings.isNullOrEmpty(newEmail)) {
      return form;
    }
    if (hasEmailChanged(newEmail, currentAccount)
        && accountRepository.lookupAccountByEmail(newEmail).isPresent()) {
      return form.withError(
          FORM_FIELD_NAME_EMAIL_ADDRESS,
          "Email address already in use. Cannot update applicant if an account already"
              + " exists.");
    }
    return form;
  }

  /**
   * This function updates the client Information after validating the form fields
   *
   * @param tiGroup - the TIGroup who manages the account whose info needs to be updated.
   * @param accountId - the account Id of the applicant whose info should be updated
   * @param form - this contains all the fields like dob, phoneNumber, emailAddress, name and
   *     tiNotes.
   * @return form - the form object is always returned. If the form contains error, the controller
   *     will handle the field messages. If the account is not found for the given AccountId, a
   *     runtime exception is raised.
   */
  public Form<EditTiClientInfoForm> updateClientInfo(
      Form<EditTiClientInfoForm> form, TrustedIntermediaryGroupModel tiGroup, Long accountId)
      throws ApplicantNotFoundException {
    // validate functions return the form w/ validation errors if applicable
    form = validateFirstNameForEditClient(form);
    form = validateLastNameForEditClient(form);
    form = validatePhoneNumber(form);
    form = validateDateOfBirth(form);
    if (form.hasErrors()) {
      return form;
    }
    Optional<AccountModel> accountMaybe =
        tiGroup.getManagedAccounts().stream()
            .filter(account -> account.id.equals(accountId))
            .findAny();
    if (accountMaybe.isEmpty() || accountMaybe.get().newestApplicant().isEmpty()) {
      throw new ApplicantNotFoundException(accountId);
    }
    form = validateEmailAddress(form, accountMaybe.get());
    if (form.hasErrors()) {
      return form;
    }
    ApplicantModel applicant = accountMaybe.get().newestApplicant().get();
    EditTiClientInfoForm currentForm = form.get();
    // after the validations are over, we can directly update the changes, as there are only two
    // cases possible for an update
    // case 1- new updates were added to the form and an update is necessary
    // case 2- no new updates were added hence it will show the same value as the current applicant
    // information
    accountRepository.updateTiClient(
        /* account= */ accountMaybe.get(),
        /* applicant= */ applicant,
        /* firstName= */ currentForm.getFirstName(),
        /* middleName= */ currentForm.getMiddleName(),
        /* lastName= */ currentForm.getLastName(),
        /* phoneNumber= */ currentForm.getPhoneNumber(),
        /* tiNote= */ currentForm.getTiNote(),
        /* email= */ currentForm.getEmailAddress(),
        /* newDob= */ currentForm.getDob());
    return form;
  }

  /**
   * Gets all the TrustedIntermediaryAccount managed by the given TI Group with/without filtering
   *
   * @param searchParameters - This object contains a nameQuery and/or a dateQuery String. If both
   *     are empty- an unfiltered list of accounts is returned. If nameQuery is present- a match
   *     between the Account holder's name and the nameQuery is performed. If dateQuery is present -
   *     a match between the Account holder's Date of Birth and the dateQuery is performed - the
   *     matched results are collected and sent as an Immutable List.
   * @param tiGroup - this is TrustedIntermediaryGroup for which the list of associated account is
   *     requested. This is needed to fetch all the accounts from the user repository.
   * @return a result object containing the ListOfAccounts which may be filtered by the Search
   *     Parameter and an optional errorMessage which is generated if the filtering has failed.
   */
  public TrustedIntermediarySearchResult getManagedAccounts(
      SearchParameters searchParameters, TrustedIntermediaryGroupModel tiGroup) {
    ImmutableList<AccountModel> allAccounts = tiGroup.getManagedAccounts();
    /* Filtering to transform empty strings into empty optionals.
    Empty parameters should return all accounts. */
    Optional<String> filteredNameQuery =
        searchParameters.nameQuery().filter(s -> !s.isEmpty() && !s.isBlank());
    Optional<String> filteredDateQuery =
        searchParameters.dateQuery().filter(s -> !s.isEmpty() && !s.isBlank());
    if (filteredNameQuery.isEmpty() && filteredDateQuery.isEmpty()) {
      return TrustedIntermediarySearchResult.success(allAccounts);
    }
    final ImmutableList<AccountModel> searchedResult;
    try {
      searchedResult = searchAccounts(searchParameters, allAccounts);
    } catch (DateTimeParseException e) {
      return TrustedIntermediarySearchResult.fail(
          allAccounts, "Please enter date in MM/dd/yyyy format");
    }
    return TrustedIntermediarySearchResult.success(searchedResult);
  }

  private ImmutableList<AccountModel> searchAccounts(
      SearchParameters searchParameters, ImmutableList<AccountModel> allAccounts) {
    return allAccounts.stream()
        .filter(
            account ->
                ((account.newestApplicant().get().getApplicantData().getDateOfBirth().isPresent()
                        && searchParameters.dateQuery().isPresent()
                        && !searchParameters.dateQuery().get().isEmpty()
                        && account
                            .newestApplicant()
                            .get()
                            .getApplicantData()
                            .getDateOfBirth()
                            .get()
                            .equals(
                                dateConverter.parseIso8601DateToLocalDate(
                                    searchParameters.dateQuery().get())))
                    || (searchParameters.nameQuery().isPresent()
                        && !searchParameters.nameQuery().get().isEmpty()
                        && account
                            .getApplicantName()
                            .toLowerCase(Locale.ROOT)
                            .contains(
                                searchParameters.nameQuery().get().toLowerCase(Locale.ROOT)))))
        .collect(ImmutableList.toImmutableList());
  }
}
