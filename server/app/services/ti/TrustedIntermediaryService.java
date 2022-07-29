package services.ti;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import forms.UpdateApplicantDob;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import models.Account;
import models.Applicant;
import models.TrustedIntermediaryGroup;
import org.slf4j.LoggerFactory;
import play.data.Form;
import repository.SearchParameters;
import repository.UserRepository;
import services.DateConverter;
import services.applicant.exception.ApplicantNotFoundException;

public class TrustedIntermediaryService {

  private final DateConverter dateConverter;
  private final UserRepository userRepository;

  @Inject
  public TrustedIntermediaryService(DateConverter dateConverter, UserRepository userRepository) {
    this.dateConverter = Preconditions.checkNotNull(dateConverter);
    this.userRepository = Preconditions.checkNotNull(userRepository);
  }

  /**
   * Gets all the TrustedIntermediaryAccount managed by the given TI Group with/without filtering
   *
   * @param searchParameters - This object contains a search and a searchDate attribute. If both are
   *     empty- an unfiltered list of accounts is returned. If search is present- a match between
   *     the Account holder's name and the search param is performed If searchDate is present - a
   *     match between the Account holder's Date of Birth and the SearchDate is performed -the
   *     matched results are collected and sent as an Immutable List
   * @param tiGroup - this is TrustedIntermediaryGroup for which the list of associated account is
   *     requested. This is needed to fetch all the accounts from the user repository.
   */
  public ImmutableList<Account> getManagedAccounts(
      SearchParameters searchParameters, TrustedIntermediaryGroup tiGroup) {
    ImmutableList<Account> allAccounts = tiGroup.getManagedAccounts();
    if (searchParameters.nameQuery().isEmpty() && searchParameters.searchDate().isEmpty()) {
      return allAccounts;
    }
    try {
      return searchAccounts(searchParameters, allAccounts);
    } catch (DateTimeParseException e) {
      LoggerFactory.getLogger(TrustedIntermediaryService.class)
          .info("Unformed String is Entered" + searchParameters.searchDate().get());
    }
    return allAccounts;
  }

  private ImmutableList<Account> searchAccounts(
      SearchParameters searchParameters, ImmutableList<Account> allAccounts) {
    return allAccounts.stream()
        .filter(
            account ->
                ((account.getApplicantDateOfBirth().isPresent()
                        && searchParameters.searchDate().isPresent()
                        && !searchParameters.searchDate().get().isEmpty()
                        && account
                            .getApplicantDateOfBirth()
                            .get()
                            .equals(
                                dateConverter.parseStringtoLocalDate(
                                    searchParameters.searchDate().get())))
                    || (searchParameters.nameQuery().isPresent()
                        && !searchParameters.nameQuery().get().isEmpty()
                        && account
                            .getApplicantName()
                            .toLowerCase(Locale.ROOT)
                            .contains(
                                searchParameters.nameQuery().get().toLowerCase(Locale.ROOT)))))
        .collect(ImmutableList.toImmutableList());
  }

  public void updateApplicantDateOfBirth(Long accountId, Form<UpdateApplicantDob> form)
      throws FormHasErrorException, ApplicantNotFoundException, DateOfBirthNotInPastException,
          IncorrectDateFormatException, MissingDateOfBirthException {
    if (form.hasErrors()) {
      StringBuilder errorMesssage = new StringBuilder();
      form.errors().stream()
          .forEach(validationError -> errorMesssage.append(validationError.message()));
      throw new FormHasErrorException(errorMesssage.toString());
    }
    if (Strings.isNullOrEmpty(form.get().getDob())) {
      throw new MissingDateOfBirthException("Please Enter a valid Date of Birth");
    }
    Optional<LocalDate> dobDate;
    try {
      dobDate = Optional.ofNullable(dateConverter.parseStringtoLocalDate(form.get().getDob()));
    } catch (DateTimeParseException e) {
      throw e;
    }
    if (!isDobInPast(dobDate.get())) {
      throw new DateOfBirthNotInPastException("The Date of Birth cannot be in future");
    }
    Optional<Account> optionalAccount = userRepository.lookupAccount(accountId);

    if (optionalAccount.isEmpty() || optionalAccount.get().newestApplicant().isEmpty()) {
      throw new ApplicantNotFoundException(accountId);
    }
    Applicant applicant = optionalAccount.get().newestApplicant().get();
    applicant.getApplicantData().setDateOfBirth(form.get().getDob());
    userRepository.updateApplicant(applicant).toCompletableFuture().join();
  }

  private boolean isDobInPast(LocalDate dobDate) {
    return dobDate.isBefore(dateConverter.getCurrentDateForZoneId());
  }
}
