package services.ti;

import static play.mvc.Results.redirect;

import auth.ProfileUtils;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import forms.AddApplicantToTrustedIntermediaryGroupForm;
import forms.UpdateApplicantDob;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import models.Account;
import models.Applicant;
import models.TrustedIntermediaryGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.mvc.Result;
import repository.SearchParameters;
import repository.UserRepository;
import services.DateConverter;

public class TrustedIntermediaryService {

  private final DateConverter dateConverter;
  private final UserRepository userRepository;
  private final ProfileUtils profileUtils;
  private final String baseUrl;

  private static final Logger LOGGER = LoggerFactory.getLogger(TrustedIntermediaryService.class);

  @Inject
  public TrustedIntermediaryService(
      DateConverter dateConverter,
      UserRepository userRepository,
      ProfileUtils profileUtils,
      Config config) {
    this.dateConverter = Preconditions.checkNotNull(dateConverter);
    this.userRepository = Preconditions.checkNotNull(userRepository);
    this.profileUtils = Preconditions.checkNotNull(profileUtils);
    this.baseUrl = Preconditions.checkNotNull(config).getString("base_url");
  }

  public ImmutableList<Account> retriveSearchResult(
      SearchParameters searchParameters,
      Optional<TrustedIntermediaryGroup> trustedIntermediaryGroup) {
    return filterAccountsBySearchParams(searchParameters, trustedIntermediaryGroup.get());
  }

  public ImmutableList<Account> filterAccountsBySearchParams(
      SearchParameters searchParameters, TrustedIntermediaryGroup tiGroup) {
    ImmutableList<Account> allAccounts = tiGroup.getManagedAccounts();
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
    return allAccounts;
  }

  private Result redirectToDashboardWithError(
      String errorMessage, Form<AddApplicantToTrustedIntermediaryGroupForm> form) {
    return redirect(
            routes.TrustedIntermediaryController.dashboard(
                /* paramName=  search*/
                Optional.empty(),
                /* paramName=  searchDate*/
                Optional.empty(),
                /* paramName=  page*/
                Optional.empty()))
        .flashing("error", errorMessage)
        .flashing("providedFirstName", form.get().getFirstName())
        .flashing("providedMiddleName", form.get().getMiddleName())
        .flashing("providedLastName", form.get().getLastName())
        .flashing("providedEmail", form.get().getEmailAddress())
        .flashing("providedDateOfBirth", form.get().getDob());
  }

  private Result redirectToDashboardWithUpdateDateOfBirthError(
      String errorMessage, Form<UpdateApplicantDob> form) {
    return redirect(
            routes.TrustedIntermediaryController.dashboard(
                /* paramName=  search*/
                Optional.empty(),
                /* paramName=  searchDate*/
                Optional.empty(),
                /* paramName=  page*/
                Optional.empty()))
        .flashing("error", errorMessage)
        .flashing("providedDateOfBirth", form.get().getDob());
  }

  public Result updateDateOfBirth(Form<UpdateApplicantDob> form, Long applicantId) {
    if (form.hasErrors()) {
      StringBuilder errorMesssage = new StringBuilder();
      form.errors().stream()
          .forEach(validationError -> errorMesssage.append(validationError.message()));
      return redirectToDashboardWithUpdateDateOfBirthError(errorMesssage.toString(), form);
    }
    if (Strings.isNullOrEmpty(form.get().getDob())) {
      return redirectToDashboardWithUpdateDateOfBirthError("Date Of Birth is required.", form);
    }
    if (!validateDateOfBirth(form.get().getDob())) {
      return redirectToDashboardWithUpdateDateOfBirthError("Date Of Birth is not valid", form);
    }

    Optional<Applicant> optionalApplicant = userRepository.lookupApplicantSync(applicantId);
    if (optionalApplicant.isEmpty()) {
      redirectToDashboardWithUpdateDateOfBirthError(
          "No applicant found for this id: " + applicantId, form);
    }
    Applicant applicant = optionalApplicant.get();
    applicant.getApplicantData().setDateOfBirth(form.get().getDob());
    userRepository.updateApplicant(applicant);
    return redirect(
        routes.TrustedIntermediaryController.dashboard(
            /* paramName=  search*/
            Optional.empty(),
            /* paramName=  searchDate*/
            Optional.empty(),
            /* paramName=  page*/
            Optional.empty()));
  }

  public Result addApplicant(
      Form<AddApplicantToTrustedIntermediaryGroupForm> form,
      Optional<TrustedIntermediaryGroup> trustedIntermediaryGroup) {
    if (form.hasErrors()) {
      StringBuilder errorMesssage = new StringBuilder();
      form.errors().stream()
          .forEach(validationError -> errorMesssage.append(validationError.message()));
      return redirectToDashboardWithError(errorMesssage.toString(), form);
    }
    if (Strings.isNullOrEmpty(form.get().getFirstName())) {
      return redirectToDashboardWithError("First name required.", form);
    }
    if (Strings.isNullOrEmpty(form.get().getLastName())) {
      return redirectToDashboardWithError("Last name required.", form);
    }
    if (Strings.isNullOrEmpty(form.get().getDob()) || !validateDateOfBirth(form.get().getDob())) {
      return redirectToDashboardWithError("Date Of Birth required.", form);
    }
    try {
      userRepository.createNewApplicantForTrustedIntermediaryGroup(
          form.get(), trustedIntermediaryGroup.get());
      return routes.TrustedIntermediaryController.dashboard(
          /* paramName=  search*/
          Optional.empty(),
          /* paramName=  searchDate*/
          Optional.empty(),
          /* paramName=  page*/
          Optional.empty());
    } catch (EmailAddressExistsException e) {
      String trustedIntermediaryUrl = baseUrl + "/trustedIntermediaries";

      return redirectToDashboardWithError(
          "Email address already in use.  Cannot create applicant if an account already exists. "
              + " Direct applicant to sign in and go to"
              + " "
              + trustedIntermediaryUrl,
          form);
    }
  }

  private boolean validateDateOfBirth(String dobString) {
    try {
      LocalDate dobDate = dateConverter.parseStringtoLocalDate(dobString);
      return dobDate.isBefore(LocalDate.now());
    } catch (DateTimeParseException e) {
      LOGGER.warn("Unformatted Date Entered - " + dobString);
    }
    return false;
  }
}
