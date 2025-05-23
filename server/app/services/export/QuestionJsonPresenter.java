package services.export;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javax.inject.Inject;
import services.Path;
import services.applicant.question.AbstractQuestion;
import services.applicant.question.AddressQuestion;
import services.applicant.question.CurrencyQuestion;
import services.applicant.question.DateQuestion;
import services.applicant.question.EmailQuestion;
import services.applicant.question.EnumeratorQuestion;
import services.applicant.question.FileUploadQuestion;
import services.applicant.question.IdQuestion;
import services.applicant.question.MultiSelectQuestion;
import services.applicant.question.NameQuestion;
import services.applicant.question.NumberQuestion;
import services.applicant.question.PhoneQuestion;
import services.applicant.question.Scalar;
import services.applicant.question.SingleSelectQuestion;
import services.applicant.question.TextQuestion;
import services.export.enums.ApiPathSegment;
import services.export.enums.QuestionTypeExternal;
import services.question.types.QuestionType;
import services.settings.SettingsManifest;

/**
 * A {@link QuestionJsonPresenter} for a Question {@link Q} provides a strategy for showing the
 * question's answers in JSON form.
 *
 * <p>Some {@link QuestionType}s share the same {@link QuestionJsonPresenter}.
 */
public interface QuestionJsonPresenter<Q extends AbstractQuestion> {

  /**
   * The answer entries that should be present in a JSON export of answers, for this question.
   *
   * <p>The returned map's keys are the {@link Path} where the value should be set, and values are
   * what should be set at the path. The Optional is empty if the value at the path should be
   * `null`, and present if a value should be set at the path.
   *
   * <p>The returned map is only empty when a question should not be included in the response at
   * all, such as with static questions.
   *
   * @param question the Question to build a JSON response for.
   * @return a map of paths to Optional answer values.
   */
  ImmutableMap<Path, Optional<?>> getAnswerJsonEntries(Q question);

  /**
   * The metadata entries that should be present in a JSON export of answers, for this question.
   *
   * <p>The returned map follows the same semantics as {@link
   * #getAnswerJsonEntries(AbstractQuestion)}.
   *
   * @param question the Question to build a JSON response for.
   * @return a map of paths to Optional metadata values.
   */
  default ImmutableMap<Path, Optional<?>> getMetadataJsonEntries(Q question) {
    return ImmutableMap.of(
        question
            .getApplicantQuestion()
            .getContextualizedPath()
            .safeWithoutArrayReference()
            .join(ApiPathSegment.QUESTION_TYPE)
            .asNestedEntitiesPath(),
        Optional.of(
            QuestionTypeExternal.fromQuestionType(question.getApplicantQuestion().getType())
                .name()));
  }

  /**
   * The metadata and answer entries that should be present in a JSON export of answers, for this
   * question.
   *
   * <p>The returned map follows the same semantics as {@link
   * #getAnswerJsonEntries(AbstractQuestion)}.
   *
   * @param question the Question to build a JSON response for.
   * @return a map of paths to Optional metadata and answer values.
   */
  default ImmutableMap<Path, Optional<?>> getAllJsonEntries(Q question) {
    return ImmutableMap.<Path, Optional<?>>builder()
        .putAll(getMetadataJsonEntries(question))
        .putAll(getAnswerJsonEntries(question))
        .build();
  }

  final class Factory {

    private final AddressJsonPresenter addressJsonPresenter;
    private final CurrencyJsonPresenter currencyJsonPresenter;
    private final DateJsonPresenter dateJsonPresenter;
    private final EmailJsonPresenter emailJsonPresenter;
    private final EmptyJsonPresenter emptyJsonPresenter;
    private final EnumeratorJsonPresenter enumeratorJsonPresenter;
    private final FileUploadJsonPresenter fileUploadJsonPresenter;
    private final IdJsonPresenter idJsonPresenter;
    private final MultiSelectJsonPresenter multiSelectJsonPresenter;
    private final NameJsonPresenter nameJsonPresenter;
    private final NumberJsonPresenter numberJsonPresenter;
    private final PhoneJsonPresenter phoneJsonPresenter;
    private final SingleSelectJsonPresenter singleSelectJsonPresenter;
    private final TextJsonPresenter textJsonPresenter;

    @Inject
    Factory(
        AddressJsonPresenter addressJsonPresenter,
        CurrencyJsonPresenter currencyJsonPresenter,
        DateJsonPresenter dateJsonPresenter,
        EmailJsonPresenter emailJsonPresenter,
        EmptyJsonPresenter emptyJsonPresenter,
        EnumeratorJsonPresenter enumeratorJsonPresenter,
        FileUploadJsonPresenter fileUploadJsonPresenter,
        IdJsonPresenter idJsonPresenter,
        MultiSelectJsonPresenter multiSelectJsonPresenter,
        NameJsonPresenter nameJsonPresenter,
        NumberJsonPresenter numberJsonPresenter,
        PhoneJsonPresenter phoneJsonPresenter,
        SingleSelectJsonPresenter singleSelectJsonPresenter,
        TextJsonPresenter textJsonPresenter) {
      this.addressJsonPresenter = checkNotNull(addressJsonPresenter);
      this.currencyJsonPresenter = checkNotNull(currencyJsonPresenter);
      this.dateJsonPresenter = checkNotNull(dateJsonPresenter);
      this.emailJsonPresenter = checkNotNull(emailJsonPresenter);
      this.emptyJsonPresenter = checkNotNull(emptyJsonPresenter);
      this.enumeratorJsonPresenter = checkNotNull(enumeratorJsonPresenter);
      this.fileUploadJsonPresenter = checkNotNull(fileUploadJsonPresenter);
      this.idJsonPresenter = checkNotNull(idJsonPresenter);
      this.multiSelectJsonPresenter = checkNotNull(multiSelectJsonPresenter);
      this.nameJsonPresenter = checkNotNull(nameJsonPresenter);
      this.numberJsonPresenter = checkNotNull(numberJsonPresenter);
      this.phoneJsonPresenter = checkNotNull(phoneJsonPresenter);
      this.singleSelectJsonPresenter = checkNotNull(singleSelectJsonPresenter);
      this.textJsonPresenter = checkNotNull(textJsonPresenter);
    }

    public QuestionJsonPresenter create(QuestionType questionType) {
      return switch (questionType) {
        case ADDRESS -> addressJsonPresenter;
        case CHECKBOX -> multiSelectJsonPresenter;
        case CURRENCY -> currencyJsonPresenter;
        case DATE -> dateJsonPresenter;
        case DROPDOWN, RADIO_BUTTON -> singleSelectJsonPresenter;
        case EMAIL -> emailJsonPresenter;
        case ENUMERATOR -> enumeratorJsonPresenter;
        case FILEUPLOAD -> fileUploadJsonPresenter;
        case ID -> idJsonPresenter;
        case NAME -> nameJsonPresenter;
        case NUMBER -> numberJsonPresenter;
        case PHONE -> phoneJsonPresenter;
          // Static content questions are not included in API responses because they
          // do not include an answer from the user.
        case STATIC -> emptyJsonPresenter;
        case TEXT -> textJsonPresenter;
        default ->
            throw new RuntimeException(String.format("Unrecognized questionType %s", questionType));
      };
    }
  }

  class AddressJsonPresenter implements QuestionJsonPresenter<AddressQuestion> {
    @Override
    public ImmutableMap<Path, Optional<?>> getAnswerJsonEntries(AddressQuestion question) {

      // We could be clever and loop through question.getAllPaths(), but we want
      // to explicitly set which scalars are exposed to the API.
      // These values are all exposed as strings for backwards compatibility.
      return ImmutableMap.of(
          /* k1= */ question.getStreetPath().asNestedEntitiesPath(),
          /* v1= */ question.getStreetValue(),
          /* k2= */ question.getLine2Path().asNestedEntitiesPath(),
          /* v2= */ question.getLine2Value(),
          /* k3= */ question.getCityPath().asNestedEntitiesPath(),
          /* v3= */ question.getCityValue(),
          /* k4= */ question.getStatePath().asNestedEntitiesPath(),
          /* v4= */ question.getStateValue(),
          /* k5= */ question.getZipPath().asNestedEntitiesPath(),
          /* v5= */ question.getZipValue(),
          /* k6= */ question.getCorrectedPath().asNestedEntitiesPath(),
          /* v6= */ question.getCorrectedValue(),
          /* k7= */ question.getLatitudePath().asNestedEntitiesPath(),
          /* v7= */ question.getLatitudeValue().map(ExportFormatUtils::formatLatOrLongAsString),
          /* k8= */ question.getLongitudePath().asNestedEntitiesPath(),
          /* v8= */ question.getLongitudeValue().map(ExportFormatUtils::formatLatOrLongAsString),
          /* k9= */ question.getWellKnownIdPath().asNestedEntitiesPath(),
          /* v9= */ question.getWellKnownIdValue().map(w -> Long.toString(w)),

          // TODO: #7134 Only here for api backwards compatibility. Long term this should move
          //       to call {@link question.getServiceAreasPath}
          /* k10= */ question.getServiceAreaPath().asNestedEntitiesPath(),
          /* v10= */ question.getServiceAreaValue().map(ExportFormatUtils::serializeServiceArea));
    }
  }

  class CurrencyJsonPresenter implements QuestionJsonPresenter<CurrencyQuestion> {

    @Override
    public ImmutableMap<Path, Optional<?>> getAnswerJsonEntries(CurrencyQuestion question) {
      Path questionPath =
          question.getApplicantQuestion().getContextualizedPath().asNestedEntitiesPath();

      return ImmutableMap.of(
          questionPath.join(ApiPathSegment.CURRENCY_DOLLARS),
          question.getCurrencyValue().map(v -> v.getCents().doubleValue() / 100.0));
    }
  }

  class DateJsonPresenter implements QuestionJsonPresenter<DateQuestion> {
    @Override
    public ImmutableMap<Path, Optional<?>> getAnswerJsonEntries(DateQuestion question) {
      Path path = question.getDatePath().asNestedEntitiesPath();

      return ImmutableMap.of(path, question.getDateValue().map(DateTimeFormatter.ISO_DATE::format));
    }
  }

  class EmailJsonPresenter implements QuestionJsonPresenter<EmailQuestion> {
    @Override
    public ImmutableMap<Path, Optional<?>> getAnswerJsonEntries(EmailQuestion question) {
      Path path = question.getEmailPath().asNestedEntitiesPath();

      return ImmutableMap.of(path, question.getEmailValue());
    }
  }

  class EmptyJsonPresenter implements QuestionJsonPresenter<AbstractQuestion> {
    @Override
    public ImmutableMap<Path, Optional<?>> getAnswerJsonEntries(AbstractQuestion question) {
      return ImmutableMap.of();
    }
  }

  class EnumeratorJsonPresenter implements QuestionJsonPresenter<EnumeratorQuestion> {
    @Override
    public ImmutableMap<Path, Optional<?>> getAnswerJsonEntries(EnumeratorQuestion question) {
      Path path =
          question
              .getApplicantQuestion()
              .getContextualizedPath()
              .withoutArrayReference()
              .asNestedEntitiesPath();
      ImmutableMap.Builder<Path, Optional<?>> jsonEntries = ImmutableMap.builder();

      if (!question.isAnswered()) {
        jsonEntries.put(path.join(ApiPathSegment.ENTITIES), Optional.of(ImmutableList.of()));
        return jsonEntries.build();
      }

      ImmutableList<String> entityNames = question.getEntityNames();
      for (int i = 0; i < entityNames.size(); i++) {
        jsonEntries.put(
            path.join(ApiPathSegment.ENTITIES).asArrayElement().atIndex(i).join(Scalar.ENTITY_NAME),
            Optional.of(entityNames.get(i)));
      }

      return jsonEntries.build();
    }
  }

  class FileUploadJsonPresenter implements QuestionJsonPresenter<FileUploadQuestion> {
    private final String baseUrl;

    @Inject
    FileUploadJsonPresenter(SettingsManifest settingsManifest) {
      baseUrl = settingsManifest.getBaseUrl().orElse("");
    }

    @Override
    public ImmutableMap<Path, Optional<?>> getAnswerJsonEntries(FileUploadQuestion question) {

      Path fileUrlsPath =
          question
              .getApplicantQuestion()
              .getContextualizedPath()
              .join(ApiPathSegment.FILE_URLS)
              .asNestedEntitiesPath();
      ImmutableList<String> fileKeys =
          question
              .getApplicantQuestion()
              .createFileUploadQuestion()
              .getFileKeyListValue()
              .orElse(ImmutableList.of());

      ImmutableList<String> fileUrls =
          fileKeys.stream()
              .map(fileKey -> ExportFormatUtils.formatFileUrlForAdmin(baseUrl, fileKey))
              .collect(toImmutableList());

      return ImmutableMap.of(fileUrlsPath, Optional.of(fileUrls));
    }
  }

  class IdJsonPresenter implements QuestionJsonPresenter<IdQuestion> {
    @Override
    public ImmutableMap<Path, Optional<?>> getAnswerJsonEntries(IdQuestion question) {
      Path path = question.getIdPath().asNestedEntitiesPath();

      return ImmutableMap.of(path, question.getIdValue());
    }
  }

  class MultiSelectJsonPresenter implements QuestionJsonPresenter<MultiSelectQuestion> {
    @Override
    public ImmutableMap<Path, Optional<?>> getAnswerJsonEntries(MultiSelectQuestion question) {
      Path path = question.getSelectionPath().asNestedEntitiesPath();

      ImmutableList<String> selectedOptions =
          question.getSelectedOptionAdminNames().orElse(ImmutableList.of());

      return ImmutableMap.of(path, Optional.of(selectedOptions));
    }
  }

  class NameJsonPresenter implements QuestionJsonPresenter<NameQuestion> {
    @Override
    public ImmutableMap<Path, Optional<?>> getAnswerJsonEntries(NameQuestion question) {
      // We could be clever and loop through question.getAllPaths(), but we want
      // to explicitly set which scalars are exposed to the API.

      Path questionPath =
          question.getApplicantQuestion().getContextualizedPath().asNestedEntitiesPath();
      return ImmutableMap.of(
          /* k1= */ question.getFirstNamePath().asNestedEntitiesPath(),
          /* v1= */ question.getFirstNameValue(),
          /* k2= */ question.getMiddleNamePath().asNestedEntitiesPath(),
          /* v2= */ question.getMiddleNameValue(),
          /* k3= */ question.getLastNamePath().asNestedEntitiesPath(),
          /* v3= */ question.getLastNameValue(),
          /* k4= */ questionPath.join(ApiPathSegment.SUFFIX),
          /* v4= */ question.getNameSuffixValue());
    }
  }

  class NumberJsonPresenter implements QuestionJsonPresenter<NumberQuestion> {
    @Override
    public ImmutableMap<Path, Optional<?>> getAnswerJsonEntries(NumberQuestion question) {
      Path path = question.getNumberPath().asNestedEntitiesPath();
      return ImmutableMap.of(path, question.getNumberValue());
    }
  }

  class PhoneJsonPresenter implements QuestionJsonPresenter<PhoneQuestion> {
    private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

    @Override
    public ImmutableMap<Path, Optional<?>> getAnswerJsonEntries(PhoneQuestion question) {
      Path path = question.getPhoneNumberPath().asNestedEntitiesPath();

      if (question.getPhoneNumberValue().isPresent()
          && question.getCountryCodeValue().isPresent()) {
        String formattedPhone =
            getFormattedPhoneNumber(
                question.getPhoneNumberValue().get(), question.getCountryCodeValue().get());
        return ImmutableMap.of(path, Optional.of(formattedPhone));
      } else {
        return ImmutableMap.of(path, Optional.empty());
      }
    }

    /*
     * This method accepts a phoneNumber as String and the countryCode which is iso alpha 2 format
     * as a String. It formats the phone number per E164 format. For a sample input of
     * phoneNumberValue="2123456789" with countryCode="US", the output will be +12123456789
     */
    private static String getFormattedPhoneNumber(String phoneNumberValue, String countryCode) {
      try {
        Phonenumber.PhoneNumber phoneNumber =
            PHONE_NUMBER_UTIL.parse(phoneNumberValue, countryCode);
        return PHONE_NUMBER_UTIL.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
      } catch (NumberParseException e) {
        throw new RuntimeException(e);
      }
    }
  }

  class SingleSelectJsonPresenter implements QuestionJsonPresenter<SingleSelectQuestion> {
    @Override
    public ImmutableMap<Path, Optional<?>> getAnswerJsonEntries(SingleSelectQuestion question) {
      return ImmutableMap.of(
          question.getSelectionPath().asNestedEntitiesPath(),
          question
              .getApplicantQuestion()
              .createSingleSelectQuestion()
              .getSelectedOptionAdminName());
    }
  }

  class TextJsonPresenter implements QuestionJsonPresenter<TextQuestion> {
    @Override
    public ImmutableMap<Path, Optional<?>> getAnswerJsonEntries(TextQuestion question) {
      Path path = question.getTextPath().asNestedEntitiesPath();

      return ImmutableMap.of(path, question.getTextValue());
    }
  }
}
