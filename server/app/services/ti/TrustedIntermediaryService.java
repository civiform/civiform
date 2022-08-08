package services.ti;

import auth.CiviFormProfile;
import com.google.common.base.Strings;
import forms.AddApplicantToTrustedIntermediaryGroupForm;
import models.TrustedIntermediaryGroup;
import play.data.Form;
import repository.UserRepository;
import services.DateConverter;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.zip.DataFormatException;

import static play.mvc.Results.notFound;
import static play.mvc.Results.unauthorized;

public class TrustedIntermediaryService {
  private final UserRepository userRepository;
  private final DateConverter dateConverter;

  public TrustedIntermediaryService(UserRepository userRepository,DateConverter dateConverter) {
    this.userRepository = userRepository;
    this.dateConverter = dateConverter;
  }

  public TIClientCreationResult addNewClient(Form<AddApplicantToTrustedIntermediaryGroupForm> form, CiviFormProfile civiFormProfile,Long id)
  {
    Optional<TrustedIntermediaryGroup> trustedIntermediaryGroup =
      userRepository.getTrustedIntermediaryGroup(civiFormProfile);
    if (trustedIntermediaryGroup.isEmpty()) {
      return TIClientCreationResult.failure(form,Optional.of(notFound()));
    }
    if (!trustedIntermediaryGroup.get().id.equals(id)) {
      return TIClientCreationResult.failure(form,Optional.of(unauthorized()));
    }

    form = validateFirstName(form);
    form = validateLastName(form);
    form = validateDateOfBirth(form);
    if (form.hasErrors()) {
      return TIClientCreationResult.failure(form,/* statusHeader= */Optional.empty());
    }
    try {
      userRepository.createNewApplicantForTrustedIntermediaryGroup(
        form.get(), trustedIntermediaryGroup.get());
    }
    catch (EmailAddressExistsException e)
    {
      form.withError("providedEmail","Email address already in use.  Cannot create applicant if an account already exists. ")
      return TIClientCreationResult.failure(form,Optional.empty());
    }
    return TIClientCreationResult.success();
  }

  private Form<AddApplicantToTrustedIntermediaryGroupForm> validateFirstName(Form<AddApplicantToTrustedIntermediaryGroupForm> form)
  {
    if (Strings.isNullOrEmpty(form.get().getFirstName())) {
      return form.withError("providedFirstName", "First name required");
    }
    return form;
  }
  private Form<AddApplicantToTrustedIntermediaryGroupForm> validateLastName(Form<AddApplicantToTrustedIntermediaryGroupForm> form)
  {
    if (Strings.isNullOrEmpty(form.get().getLastName())) {
      return form.withError("providedLastName", "Last name required");
    }
    return form;
  }
  private Form<AddApplicantToTrustedIntermediaryGroupForm> validateDateOfBirth(Form<AddApplicantToTrustedIntermediaryGroupForm> form)
  {
    if (Strings.isNullOrEmpty(form.get().getDob())) {
      return form.withError("providedDob", "Date of Birth required");
    }
    LocalDate currentDob = null;
    try{
      currentDob = dateConverter.parseStringtoLocalDate(form.get().getDob());
    }
    catch (DateTimeParseException e)
    {
      return form.withError("providedDob", "Date of Birth must be in MM-dd-yyyy format");
    }
    if (!currentDob.isBefore(dateConverter.getCurrentDateForZoneId())) {
      return form.withError("providedDob", "Date of Birth should be in the past");
    }
    return form;
  }



}
