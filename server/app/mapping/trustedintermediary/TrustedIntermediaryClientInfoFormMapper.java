package mapping.trustedintermediary;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import controllers.ti.routes;
import forms.TiClientInfoForm;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import models.AccountModel;
import models.ApplicantModel;
import play.data.Form;
import play.i18n.Messages;
import services.DateConverter;
import services.ti.TrustedIntermediaryService;
import views.trustedintermediary.TrustedIntermediaryClientInfoForm;
import views.trustedintermediary.TrustedIntermediaryClientInfoForm.Field;
import views.trustedintermediary.TrustedIntermediaryClientInfoForm.SuffixOption;

/**
 * Maps a client's stored information, or a submitted client information form, to the
 * TrustedIntermediaryClientInfoForm shared by the add client and edit client pages.
 */
public final class TrustedIntermediaryClientInfoFormMapper {

  /**
   * Maps the client information form.
   *
   * @param formId id of the form element
   * @param actionUrl target of the form's POST
   * @param account the client being edited, whose stored information fills the fields; empty when
   *     adding a client
   * @param submittedForm the form the trusted intermediary submitted; when present its values and
   *     errors replace the stored information
   * @param showNameSuffix show the name suffix select
   * @param dateConverter formats the stored date of birth
   * @param messages localizes the validation errors
   */
  public TrustedIntermediaryClientInfoForm map(
      String formId,
      String actionUrl,
      Optional<AccountModel> account,
      Optional<Form<TiClientInfoForm>> submittedForm,
      boolean showNameSuffix,
      DateConverter dateConverter,
      Messages messages) {
    Optional<ApplicantModel> applicant = account.flatMap(AccountModel::representativeApplicant);
    FieldMapper fields = new FieldMapper(submittedForm, messages);

    Field nameSuffix =
        fields.map(
            TrustedIntermediaryService.FORM_FIELD_NAME_SUFFIX,
            TiClientInfoForm::getNameSuffix,
            applicant.flatMap(ApplicantModel::getSuffix).orElse(""));

    return TrustedIntermediaryClientInfoForm.builder()
        .formId(formId)
        .actionUrl(actionUrl)
        .showNameSuffix(showNameSuffix)
        .firstName(
            fields.map(
                TrustedIntermediaryService.FORM_FIELD_NAME_FIRST_NAME,
                TiClientInfoForm::getFirstName,
                applicant.flatMap(ApplicantModel::getFirstName).orElse("")))
        .middleName(
            fields.map(
                TrustedIntermediaryService.FORM_FIELD_NAME_MIDDLE_NAME,
                TiClientInfoForm::getMiddleName,
                applicant.flatMap(ApplicantModel::getMiddleName).orElse("")))
        .lastName(
            fields.map(
                TrustedIntermediaryService.FORM_FIELD_NAME_LAST_NAME,
                TiClientInfoForm::getLastName,
                applicant.flatMap(ApplicantModel::getLastName).orElse("")))
        .nameSuffix(nameSuffix)
        .phoneNumber(
            fields.map(
                TrustedIntermediaryService.FORM_FIELD_NAME_PHONE,
                TiClientInfoForm::getPhoneNumber,
                applicant.flatMap(ApplicantModel::getPhoneNumber).orElse("")))
        .emailAddress(
            fields.map(
                TrustedIntermediaryService.FORM_FIELD_NAME_EMAIL_ADDRESS,
                TiClientInfoForm::getEmailAddress,
                applicant.flatMap(ApplicantModel::getEmailAddress).orElse("")))
        .dateOfBirth(
            fields.map(
                TrustedIntermediaryService.FORM_FIELD_NAME_DOB,
                TiClientInfoForm::getDob,
                applicant
                    .flatMap(ApplicantModel::getDateOfBirth)
                    .map(dateConverter::formatIso8601Date)
                    .orElse("")))
        .tiNote(
            fields.map(
                TrustedIntermediaryService.FORM_FIELD_NAME_TI_NOTES,
                TiClientInfoForm::getTiNote,
                account.map(AccountModel::getTiNote).orElse("")))
        .suffixOptions(buildSuffixOptions(nameSuffix.value()))
        .build();
  }

  /**
   * Path of the first page of the client list, the target of the add and edit client pages' tab
   * link, back link and Cancel button.
   */
  static String clientListPath() {
    return routes.TrustedIntermediaryController.dashboard(
            /* nameQuery= */ Optional.empty(),
            /* dayQuery= */ Optional.empty(),
            /* monthQuery= */ Optional.empty(),
            /* yearQuery= */ Optional.empty(),
            /* page= */ Optional.of(1))
        .url();
  }

  private static ImmutableList<SuffixOption> buildSuffixOptions(String selectedSuffix) {
    return Stream.of(ApplicantModel.Suffix.values())
        .map(
            suffix ->
                new SuffixOption(
                    suffix.toString(), suffix.getValue(), suffix.toString().equals(selectedSuffix)))
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Picks each field's value and errors the way the legacy EditTiClientView.setStateIfPresent did:
   * a submitted form replaces the stored value, even with an empty one, and carries the errors.
   */
  private record FieldMapper(Optional<Form<TiClientInfoForm>> submittedForm, Messages messages) {

    Field map(String key, Function<TiClientInfoForm, String> submittedValue, String storedValue) {
      if (submittedForm.isEmpty()) {
        return new Field(Strings.nullToEmpty(storedValue), ImmutableList.of());
      }

      Form<TiClientInfoForm> form = submittedForm.get();

      // The legacy view rendered an unbound (null) value as a bare value attribute; an empty value
      // shows the same empty field.
      String value = Strings.nullToEmpty(submittedValue.apply(form.value().get()));
      ImmutableList<String> errors =
          form.errors(key).stream()
              .map(error -> error.format(messages))
              .collect(ImmutableList.toImmutableList());

      return new Field(value, errors);
    }
  }
}
