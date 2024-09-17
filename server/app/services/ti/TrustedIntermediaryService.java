package services.ti;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import forms.TiClientInfoForm;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import models.AccountModel;
import models.ApplicantModel;
import models.TrustedIntermediaryGroupModel;
import play.data.Form;
import play.i18n.Messages;
import repository.AccountRepository;
import repository.SearchParameters;
import services.DateConverter;
import services.MessageKey;
import services.PhoneValidationUtils;
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
  public static final String FORM_FIELD_NAME_SUFFIX = "suffixName";
  public static final String FORM_FIELD_NAME_TI_NOTES = "tiNote";

  @Inject
  public TrustedIntermediaryService(
      AccountRepository accountRepository, DateConverter dateConverter) {
    this.accountRepository = Preconditions.checkNotNull(accountRepository);
    this.dateConverter = Preconditions.checkNotNull(dateConverter);
  }

  public AddNewApplicantReturnObject addNewClient(
      Form<TiClientInfoForm> form,
      TrustedIntermediaryGroupModel trustedIntermediaryGroup,
      Messages preferredLanguage) {
    Long clientApplicantId;
    form = validateFirstNameForEditClient(form, preferredLanguage);
    form = validateLastNameForEditClient(form, preferredLanguage);
    form = validatePhoneNumber(form, preferredLanguage);
    form = validateDateOfBirth(form, preferredLanguage);
    if (form.hasErrors()) {
      return new AddNewApplicantReturnObject(form);
    }
    try {
      clientApplicantId =
          accountRepository.createNewApplicantForTrustedIntermediaryGroup(
              form.get(), trustedIntermediaryGroup);
    } catch (EmailAddressExistsException e) {
      return new AddNewApplicantReturnObject(
          form.withError(
              FORM_FIELD_NAME_EMAIL_ADDRESS,
              preferredLanguage.at(MessageKey.ERROR_EMAIL_IN_USE_CLIENT_CREATE.getKeyName())));
    }
    return new AddNewApplicantReturnObject(form, clientApplicantId);
  }

  private Form<TiClientInfoForm> validateDateOfBirth(
      Form<TiClientInfoForm> form, Messages messages) {
    String dob = form.value().get().getDob();
    if (Strings.isNullOrEmpty(dob)) {
      return form.withError(
          FORM_FIELD_NAME_DOB, messages.at(MessageKey.DOB_ERROR_LABEL.getKeyName()));
    }
    final LocalDate currentDob;
    try {
      currentDob = dateConverter.parseIso8601DateToLocalDate(dob);
    } catch (DateTimeParseException e) {
      return form.withError(
          FORM_FIELD_NAME_DOB,
          messages.at(MessageKey.DATE_VALIDATION_INVALID_DATE_FORMAT.getKeyName()));
    }
    if (!currentDob.isBefore(dateConverter.getCurrentDateForZoneId())) {
      return form.withError(
          FORM_FIELD_NAME_DOB,
          messages.at(MessageKey.DATE_VALIDATION_DOB_NOT_IN_PAST.getKeyName()));
    }
    if (currentDob.isBefore(dateConverter.getCurrentDateForZoneId().minusYears(150))) {
      return form.withError(
          FORM_FIELD_NAME_DOB, messages.at(MessageKey.DATE_VALIDATION_IMPOSSIBLE_DOB.getKeyName()));
    }
    return form;
  }

  private Form<TiClientInfoForm> validateFirstNameForEditClient(
      Form<TiClientInfoForm> form, Messages messages) {
    if (Strings.isNullOrEmpty(form.value().get().getFirstName())) {
      return form.withError(
          FORM_FIELD_NAME_FIRST_NAME, messages.at(MessageKey.ERROR_FIRST_NAME.getKeyName()));
    }
    return form;
  }

  private Form<TiClientInfoForm> validateLastNameForEditClient(
      Form<TiClientInfoForm> form, Messages messages) {
    if (Strings.isNullOrEmpty(form.value().get().getLastName())) {
      return form.withError(
          FORM_FIELD_NAME_LAST_NAME, messages.at(MessageKey.ERROR_LAST_NAME.getKeyName()));
    }
    return form;
  }

  private Boolean hasEmailChanged(String newEmail, AccountModel account) {
    return !newEmail.equals(account.getEmailAddress());
  }

  private Form<TiClientInfoForm> validatePhoneNumber(
      Form<TiClientInfoForm> form, Messages preferredLanguage) {
    String phoneNumber = form.value().get().getPhoneNumber();

    if (Strings.isNullOrEmpty(phoneNumber)) {
      return form;
    }

    var phoneValidationResult = PhoneValidationUtils.determineCountryCode(Optional.of(phoneNumber));

    if (!phoneValidationResult.isValid()) {
      return form.withError(
          FORM_FIELD_NAME_PHONE,
          preferredLanguage.at(phoneValidationResult.getMessageKey().get().getKeyName()));
    }

    return form;
  }

  private Form<TiClientInfoForm> validateEmailAddress(
      Form<TiClientInfoForm> form, AccountModel currentAccount, Messages messages) {
    String newEmail = form.get().getEmailAddress();
    // email addresses not a requirement for TI Client
    if (Strings.isNullOrEmpty(newEmail)) {
      return form;
    }
    if (hasEmailChanged(newEmail, currentAccount)
        && accountRepository.lookupAccountByEmail(newEmail).isPresent()) {
      return form.withError(
          FORM_FIELD_NAME_EMAIL_ADDRESS,
          messages.at(MessageKey.ERROR_EMAIL_IN_USE_CLIENT_EDIT.getKeyName()));
    }
    return form;
  }

  /**
   * This function updates the client Information after validating the form fields
   *
   * @param form - this contains all the fields like dob, phoneNumber, emailAddress, name and
   *     tiNotes.
   * @param tiGroup - the TIGroup who manages the account whose info needs to be updated.
   * @param accountId - the account Id of the applicant whose info should be updated
   * @param preferredLanguage - the preferred Language of the TI Client (by default it is US-En)
   * @return form - the form object is always returned. If the form contains error, the controller
   *     will handle the field messages. If the account is not found for the given AccountId, a
   *     runtime exception is raised.
   */
  public Form<TiClientInfoForm> updateClientInfo(
      Form<TiClientInfoForm> form,
      TrustedIntermediaryGroupModel tiGroup,
      Long accountId,
      Messages preferredLanguage)
      throws ApplicantNotFoundException {
    // validate functions return the form w/ validation errors if applicable
    form = validateFirstNameForEditClient(form, preferredLanguage);
    form = validateLastNameForEditClient(form, preferredLanguage);
    form = validatePhoneNumber(form, preferredLanguage);
    form = validateDateOfBirth(form, preferredLanguage);
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
    form = validateEmailAddress(form, accountMaybe.get(), preferredLanguage);
    if (form.hasErrors()) {
      return form;
    }
    ApplicantModel applicant = accountMaybe.get().newestApplicant().get();
    TiClientInfoForm currentForm = form.get();
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
        /* nameSuffix= */ currentForm.getNameSuffix(),
        /* phoneNumber= */ currentForm.getPhoneNumber(),
        /* tiNote= */ currentForm.getTiNote(),
        /* email= */ currentForm.getEmailAddress(),
        /* newDob= */ currentForm.getDob());
    return form;
  }

  /**
   * Gets all the TrustedIntermediaryAccount managed by the given TI Group with/without filtering
   *
   * @param searchParameters - This object contains a nameQuery, a dayQuery, a monthQuery and a
   *     yearQuery String. If all are empty, an unfiltered list of accounts is returned. If
   *     nameQuery is present, a match between the Account holder's name and the nameQuery is
   *     performed. If dayQuery, monthQuery and yearQuery are present, a match between the Account
   *     holder's Date of Birth and the date queries is performed. The matched results are collected
   *     and sent as an Immutable List. If name query is empty and only some of the date queries are
   *     present, but not all, an empty list is returned.
   * @param tiGroup - this is TrustedIntermediaryGroup for which the list of associated account is
   *     requested. This is needed to fetch all the accounts from the user repository.
   * @return a result object containing the ListOfAccounts which may be filtered by the Search
   *     Parameter and an optional errorMessage which is generated if the filtering has failed.
   */
  public TrustedIntermediarySearchResult getManagedAccounts(
      SearchParameters searchParameters, TrustedIntermediaryGroupModel tiGroup) {
    ImmutableList<AccountModel> allAccounts = tiGroup.getManagedAccounts();
    List<SearchParameters.ParamTypes> missingParams = findMissingSearchParams(searchParameters);
    if (missingParams.size() == 4) {
      return new TrustedIntermediarySearchResult(allAccounts);
    }
    final ImmutableList<AccountModel> searchedResult;
    try {
      searchedResult = searchAccounts(searchParameters, allAccounts, missingParams);
    } catch (DateTimeParseException e) {
      return new TrustedIntermediarySearchResult(
          allAccounts, Optional.of("Please enter a valid birth date."));
    }
    return new TrustedIntermediarySearchResult(searchedResult);
  }

  private ImmutableList<AccountModel> searchAccounts(
      SearchParameters searchParameters,
      ImmutableList<AccountModel> allAccounts,
      List<SearchParameters.ParamTypes> missingParams) {
    Optional<LocalDate> maybeDOB =
        validateAndConvertSearchParamDOB(searchParameters, missingParams);
    return allAccounts.stream()
        .filter(
            account ->
                ((account.newestApplicant().get().getApplicantData().getDateOfBirth().isPresent()
                        && maybeDOB.isPresent()
                        && account
                            .newestApplicant()
                            .get()
                            .getApplicantData()
                            .getDateOfBirth()
                            .get()
                            .equals(maybeDOB.get()))
                    || (!missingParams.contains(SearchParameters.ParamTypes.NAME)
                        && account
                            .getApplicantName()
                            .toLowerCase(Locale.ROOT)
                            .contains(
                                searchParameters.nameQuery().get().toLowerCase(Locale.ROOT)))))
        .collect(ImmutableList.toImmutableList());
  }

  public static List<SearchParameters.ParamTypes> findMissingSearchParams(
      SearchParameters searchParameters) {
    List<SearchParameters.ParamTypes> missing = new ArrayList<>();

    /* Filtering to transform empty strings into empty optionals.
    Empty parameters should return all accounts. */
    Optional<String> filteredNameQuery =
        searchParameters.nameQuery().filter(s -> !s.isEmpty() && !s.isBlank());
    Optional<String> filteredDayQuery =
        searchParameters.dayQuery().filter(s -> !s.isEmpty() && !s.isBlank());
    Optional<String> filteredMonthQuery =
        searchParameters.monthQuery().filter(s -> !s.isEmpty() && !s.isBlank());
    Optional<String> filteredYearQuery =
        searchParameters.yearQuery().filter(s -> !s.isEmpty() && !s.isBlank());

    if (filteredNameQuery.isEmpty()) {
      missing.add(SearchParameters.ParamTypes.NAME);
    }
    if (filteredDayQuery.isEmpty()) {
      missing.add(SearchParameters.ParamTypes.DAY);
    }
    if (filteredMonthQuery.isEmpty()) {
      missing.add(SearchParameters.ParamTypes.MONTH);
    }
    if (filteredYearQuery.isEmpty()) {
      missing.add(SearchParameters.ParamTypes.YEAR);
    }

    return missing;
  }

  public static boolean validateSearch(SearchParameters searchParameters) {
    List<SearchParameters.ParamTypes> missingParams =
        TrustedIntermediaryService.findMissingSearchParams(searchParameters);
    boolean isValidSearch =
        missingParams.size() == 4
            || !missingParams.contains(SearchParameters.ParamTypes.NAME)
            || (!missingParams.contains(SearchParameters.ParamTypes.DAY)
                && !missingParams.contains(SearchParameters.ParamTypes.MONTH)
                && !missingParams.contains(SearchParameters.ParamTypes.YEAR));
    return isValidSearch;
  }

  private Optional<LocalDate> validateAndConvertSearchParamDOB(
      SearchParameters searchParameters, List<SearchParameters.ParamTypes> missing) {
    if (missing.stream()
        .noneMatch(
            paramType ->
                paramType.equals(SearchParameters.ParamTypes.DAY)
                    || paramType.equals(SearchParameters.ParamTypes.MONTH)
                    || paramType.equals(SearchParameters.ParamTypes.YEAR))) {
      return Optional.of(
          dateConverter.parseDayMonthYearToLocalDate(
              searchParameters.dayQuery().get(),
              searchParameters.monthQuery().get(),
              searchParameters.yearQuery().get()));
    }
    return Optional.empty();
  }
}
