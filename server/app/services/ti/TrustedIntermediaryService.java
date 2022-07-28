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

  public ImmutableList<Account> retriveSearchResult(
      SearchParameters searchParameters,
      Optional<TrustedIntermediaryGroup> trustedIntermediaryGroup) {
    return filterAccountsBySearchParams(searchParameters, trustedIntermediaryGroup.get());
  }

  public ImmutableList<Account> filterAccountsBySearchParams(
      SearchParameters searchParameters, TrustedIntermediaryGroup tiGroup) {
    ImmutableList<Account> allAccounts = tiGroup.getManagedAccounts();
    if(searchParameters.search().isPresent() || searchParameters.searchDate().isPresent()) {
      allAccounts =
        allAccounts.stream()
          .filter(
            account -> {
              if (searchParameters.search().isPresent()) {
                return account
                  .getApplicantName()
                  .toLowerCase(Locale.ROOT)
                  .contains(searchParameters.search().get().toLowerCase(Locale.ROOT));
              }
              if (searchParameters.searchDate().isPresent()
                && !searchParameters.searchDate().isEmpty()
                && account.getApplicantDateOfBirth().isPresent()) {
                LocalDate searchDate =
                  dateConverter.parseStringtoLocalDate(searchParameters.searchDate().get());
                return account.getApplicantDateOfBirth().get().equals(searchDate);
              }
              return false;
            })
          .collect(ImmutableList.toImmutableList());
    }
    return allAccounts;
  }

  public Optional<Applicant> checkFormForDobUpdate(Form<UpdateApplicantDob> form, Long accountId)
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
    return optionalAccount.get().newestApplicant();
  }

  private boolean isDobInPast(LocalDate dobDate) {
    return dobDate.isBefore(dateConverter.getCurrentDateForZoneId());
  }
}
