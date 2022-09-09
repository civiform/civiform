package services.ti;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import forms.AddApplicantToTrustedIntermediaryGroupForm;
import forms.UpdateApplicantDobForm;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import models.Account;
import models.Applicant;
import models.TrustedIntermediaryGroup;
import play.data.Form;
import repository.SearchParameters;
import repository.UserRepository;
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
  private final UserRepository userRepository;
  private final DateConverter dateConverter;
  public static final String FORM_FIELD_NAME_FIRST_NAME = "firstName";
  public static final String FORM_FIELD_NAME_LAST_NAME = "lastName";
  public static final String FORM_FIELD_NAME_EMAIL_ADDRESS = "emailAddress";
  public static final String FORM_FIELD_NAME_DOB = "dob";

  @Inject
  public TrustedIntermediaryService(UserRepository userRepository, DateConverter dateConverter) {
    this.userRepository = Preconditions.checkNotNull(userRepository);
    this.dateConverter = Preconditions.checkNotNull(dateConverter);
  }

  public Form<AddApplicantToTrustedIntermediaryGroupForm> addNewClient(
      Form<AddApplicantToTrustedIntermediaryGroupForm> form,
      TrustedIntermediaryGroup trustedIntermediaryGroup) {
    form = validateEmailAddress(form);
    form = validateFirstName(form);
    form = validateLastName(form);
    form = validateDateOfBirthForAddApplicant(form);
    if (form.hasErrors()) {
      return form;
    }
    try {
      userRepository.createNewApplicantForTrustedIntermediaryGroup(
          form.get(), trustedIntermediaryGroup);
    } catch (EmailAddressExistsException e) {
      return form.withError(
          FORM_FIELD_NAME_EMAIL_ADDRESS,
          "Email address already in use. Cannot create applicant if an account already"
              + " exists.");
    }
    return form;
  }

  private Form<AddApplicantToTrustedIntermediaryGroupForm> validateEmailAddress(
      Form<AddApplicantToTrustedIntermediaryGroupForm> form) {
    if (Strings.isNullOrEmpty(form.value().get().getEmailAddress())) {
      return form.withError(FORM_FIELD_NAME_EMAIL_ADDRESS, "Email Address required");
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
    LocalDate currentDob = null;
    try {
      currentDob = dateConverter.parseIso8601DateToLocalDate(dob);
    } catch (DateTimeParseException e) {
      return Optional.of("Date of Birth must be in MM/dd/yyyy format");
    }
    if (!currentDob.isBefore(dateConverter.getCurrentDateForZoneId())) {
      return Optional.of("Date of Birth should be in the past");
    }
    return Optional.empty();
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
      SearchParameters searchParameters, TrustedIntermediaryGroup tiGroup) {
    ImmutableList<Account> allAccounts = tiGroup.getManagedAccounts();
    if (searchParameters.nameQuery().isEmpty() && searchParameters.dateQuery().isEmpty()) {
      return TrustedIntermediarySearchResult.success(allAccounts);
    }
    ImmutableList<Account> searchedResult = null;
    try {
      searchedResult = searchAccounts(searchParameters, allAccounts);
    } catch (DateTimeParseException e) {
      return TrustedIntermediarySearchResult.fail(
          allAccounts, "Please enter date in MM/dd/yyyy format");
    }
    return TrustedIntermediarySearchResult.success(searchedResult);
  }

  private ImmutableList<Account> searchAccounts(
      SearchParameters searchParameters, ImmutableList<Account> allAccounts) {
    ;
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

  /**
   * This function updates the Applicant's date of birth by calling the UpdateApplicant() on the
   * User Repository.
   *
   * @param trustedIntermediaryGroup - the TIGroup who manages the account whose Dob needs to be
   *     updated.
   * @param accountId - the account Id of the applicant whose Dob should be updated
   * @param form - this contains the dob field which would be parsed into local date and updated for
   *     the applicant
   * @return form - the form object is always returned. If the form contains error, the controller
   *     will handle the flash messages If the account is not found for the given AccountId, a
   *     runtime exception is raised.
   */
  public Form<UpdateApplicantDobForm> updateApplicantDateOfBirth(
      TrustedIntermediaryGroup trustedIntermediaryGroup,
      Long accountId,
      Form<UpdateApplicantDobForm> form)
      throws ApplicantNotFoundException {

    form = validateDateOfBirthForUpdateDob(form);

    if (form.hasErrors()) {
      return form;
    }
    Optional<Account> optionalAccount =
        trustedIntermediaryGroup.getManagedAccounts().stream()
            .filter(account -> account.id.equals(accountId))
            .findAny();

    if (optionalAccount.isEmpty() || optionalAccount.get().newestApplicant().isEmpty()) {
      throw new ApplicantNotFoundException(accountId);
    }
    Applicant applicant = optionalAccount.get().newestApplicant().get();
    applicant.getApplicantData().setDateOfBirth(form.get().getDob());
    userRepository.updateApplicant(applicant).toCompletableFuture().join();
    return form;
  }

  private Form<UpdateApplicantDobForm> validateDateOfBirthForUpdateDob(
      Form<UpdateApplicantDobForm> form) {
    Optional<String> errorMessage = validateDateOfBirth(form.value().get().getDob());
    if (errorMessage.isPresent()) {
      return form.withError(FORM_FIELD_NAME_DOB, errorMessage.get());
    }
    return form;
  }
}
