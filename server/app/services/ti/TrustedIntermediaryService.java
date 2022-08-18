package services.ti;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import forms.AddApplicantToTrustedIntermediaryGroupForm;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.inject.Inject;
import models.TrustedIntermediaryGroup;
import play.data.Form;
import repository.UserRepository;
import services.DateConverter;

/**
 * Service Class for TrustedIntermediaryController.
 *
 * <p>Civiform TrustedIntermediaries have the ability to add Clients and apply to Civiform programs
 * on their behalf. The first step to this process is add a Client by providing their First and Last
 * name, their email address and their Date of birth.
 *
 * <p>This class performs the validation of the request form passed from the Controller and if the
 * form has no-errors, it sends back a successful TiclientCreationResult object.
 *
 * <p>If any of the validation fails, it sends a failed TIClientCreationResult object with the form
 * and all of its errors.
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

  public TIClientCreationResult addNewClient(
      Form<AddApplicantToTrustedIntermediaryGroupForm> form,
      TrustedIntermediaryGroup trustedIntermediaryGroup) {
    form = validateEmailAddress(form);
    form = validateFirstName(form);
    form = validateLastName(form);
    form = validateDateOfBirth(form);
    if (form.hasErrors()) {
      return TIClientCreationResult.failure(form);
    }
    try {
      userRepository.createNewApplicantForTrustedIntermediaryGroup(
          form.get(), trustedIntermediaryGroup);
    } catch (EmailAddressExistsException e) {
      return TIClientCreationResult.failure(
          form.withError(
              FORM_FIELD_NAME_EMAIL_ADDRESS,
              "Email address already in use. Cannot create applicant if an account already"
                  + " exists."));
    }
    return TIClientCreationResult.success();
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

  private Form<AddApplicantToTrustedIntermediaryGroupForm> validateDateOfBirth(
      Form<AddApplicantToTrustedIntermediaryGroupForm> form) {
    if (Strings.isNullOrEmpty(form.value().get().getDob())) {
      return form.withError(FORM_FIELD_NAME_DOB, "Date of Birth required");
    }
    LocalDate currentDob = null;
    try {
      currentDob = dateConverter.parseIso8601DateToLocalDate(form.value().get().getDob());
    } catch (DateTimeParseException e) {
      return form.withError(FORM_FIELD_NAME_DOB, "Date of Birth must be in MM/dd/yyyy format");
    }
    if (!currentDob.isBefore(dateConverter.getCurrentDateForZoneId())) {
      return form.withError(FORM_FIELD_NAME_DOB, "Date of Birth should be in the past");
    }
    return form;
  }
}
