package services.ti;

import com.google.common.base.Strings;
import forms.AddApplicantToTrustedIntermediaryGroupForm;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.inject.Inject;
import models.TrustedIntermediaryGroup;
import play.data.Form;
import repository.UserRepository;
import services.DateConverter;

public class TrustedIntermediaryService {
  private final UserRepository userRepository;
  private final DateConverter dateConverter;

  @Inject
  public TrustedIntermediaryService(UserRepository userRepository, DateConverter dateConverter) {
    this.userRepository = userRepository;
    this.dateConverter = dateConverter;
  }

  public TIClientCreationResult addNewClient(
      Form<AddApplicantToTrustedIntermediaryGroupForm> form,
      TrustedIntermediaryGroup trustedIntermediaryGroup)
      throws EmailAddressExistsException {
    form = validateEmailAddress(form);
    form = validateFirstName(form);
    form = validateLastName(form);
    form = validateDateOfBirth(form);
    if (form.hasErrors()) {
      return TIClientCreationResult.failure(form);
    }
    userRepository.createNewApplicantForTrustedIntermediaryGroup(
        form.get(), trustedIntermediaryGroup);

    return TIClientCreationResult.success();
  }

  private Form<AddApplicantToTrustedIntermediaryGroupForm> validateEmailAddress(
      Form<AddApplicantToTrustedIntermediaryGroupForm> form) {
    if (Strings.isNullOrEmpty(form.value().get().getEmailAddress())) {
      return form.withError("emailAddress", "Email Address required");
    }
    return form;
  }

  private Form<AddApplicantToTrustedIntermediaryGroupForm> validateFirstName(
      Form<AddApplicantToTrustedIntermediaryGroupForm> form) {
    if (Strings.isNullOrEmpty(form.value().get().getFirstName())) {
      return form.withError("firstName", "First name required");
    }
    return form;
  }

  private Form<AddApplicantToTrustedIntermediaryGroupForm> validateLastName(
      Form<AddApplicantToTrustedIntermediaryGroupForm> form) {
    if (Strings.isNullOrEmpty(form.value().get().getLastName())) {
      return form.withError("lastName", "Last name required");
    }
    return form;
  }

  private Form<AddApplicantToTrustedIntermediaryGroupForm> validateDateOfBirth(
      Form<AddApplicantToTrustedIntermediaryGroupForm> form) {
    if (Strings.isNullOrEmpty(form.value().get().getDob())) {
      return form.withError("dob", "Date of Birth required");
    }
    LocalDate currentDob = null;
    try {
      currentDob = dateConverter.parseStringtoLocalDate(form.value().get().getDob());
    } catch (DateTimeParseException e) {
      return form.withError("dob", "Date of Birth must be in MM-dd-yyyy format");
    }
    if (!currentDob.isBefore(dateConverter.getCurrentDateForZoneId())) {
      return form.withError("dob", "Date of Birth should be in the past");
    }
    return form;
  }
}
