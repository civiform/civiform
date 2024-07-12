package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javax.inject.Inject;
import services.Path;
import services.applicant.question.CurrencyQuestion;
import services.applicant.question.DateQuestion;
import services.applicant.question.EnumeratorQuestion;
import services.applicant.question.FileUploadQuestion;
import services.applicant.question.MultiSelectQuestion;
import services.applicant.question.NumberQuestion;
import services.applicant.question.PhoneQuestion;
import services.applicant.question.Question;
import services.applicant.question.Scalar;
import services.applicant.question.SingleSelectQuestion;
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
public interface QuestionJsonPresenter<Q extends Question> {

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
   * <p>The returned map follows the same semantics as {@link #getAnswerJsonEntries(Question)}.
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
   * <p>The returned map follows the same semantics as {@link #getAnswerJsonEntries(Question)}.
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

    private final ContextualizedScalarsJsonPresenter contextualizedScalarsJsonPresenter;
    private final CurrencyJsonPresenter currencyJsonPresenter;
    private final DateJsonPresenter dateJsonPresenter;
    private final EmptyJsonPresenter emptyJsonPresenter;
    private final EnumeratorJsonPresenter enumeratorJsonPresenter;
    private final FileUploadJsonPresenter fileUploadJsonPresenter;
    private final MultiSelectJsonPresenter multiSelectJsonPresenter;
    private final NumberJsonPresenter numberJsonPresenter;
    private final PhoneJsonPresenter phoneJsonPresenter;
    private final SingleSelectJsonPresenter singleSelectJsonPresenter;

    @Inject
    Factory(
        ContextualizedScalarsJsonPresenter contextualizedScalarsJsonPresenter,
        CurrencyJsonPresenter currencyJsonPresenter,
        DateJsonPresenter dateJsonPresenter,
        EmptyJsonPresenter emptyJsonPresenter,
        EnumeratorJsonPresenter enumeratorJsonPresenter,
        FileUploadJsonPresenter fileUploadJsonPresenter,
        MultiSelectJsonPresenter multiSelectJsonPresenter,
        NumberJsonPresenter numberJsonPresenter,
        PhoneJsonPresenter phoneJsonPresenter,
        SingleSelectJsonPresenter singleSelectJsonPresenter) {
      this.contextualizedScalarsJsonPresenter = checkNotNull(contextualizedScalarsJsonPresenter);
      this.currencyJsonPresenter = checkNotNull(currencyJsonPresenter);
      this.dateJsonPresenter = checkNotNull(dateJsonPresenter);
      this.emptyJsonPresenter = checkNotNull(emptyJsonPresenter);
      this.enumeratorJsonPresenter = checkNotNull(enumeratorJsonPresenter);
      this.fileUploadJsonPresenter = checkNotNull(fileUploadJsonPresenter);
      this.multiSelectJsonPresenter = checkNotNull(multiSelectJsonPresenter);
      this.numberJsonPresenter = checkNotNull(numberJsonPresenter);
      this.phoneJsonPresenter = checkNotNull(phoneJsonPresenter);
      this.singleSelectJsonPresenter = checkNotNull(singleSelectJsonPresenter);
    }

    public QuestionJsonPresenter create(QuestionType questionType) {
      switch (questionType) {
        case ADDRESS:
        case EMAIL:
        case ID:
        case NAME:
        case TEXT:
          return contextualizedScalarsJsonPresenter;
        case CHECKBOX:
          return multiSelectJsonPresenter;
        case CURRENCY:
          return currencyJsonPresenter;
        case DATE:
          return dateJsonPresenter;
        case DROPDOWN:
        case RADIO_BUTTON:
          return singleSelectJsonPresenter;
        case ENUMERATOR:
          return enumeratorJsonPresenter;
        case FILEUPLOAD:
          return fileUploadJsonPresenter;
        case NUMBER:
          return numberJsonPresenter;
        case PHONE:
          return phoneJsonPresenter;
          // Static content questions are not included in API responses because they
          // do not include an answer from the user.
        case STATIC:
          return emptyJsonPresenter;

        default:
          throw new RuntimeException(String.format("Unrecognized questionType %s", questionType));
      }
    }
  }

  class ContextualizedScalarsJsonPresenter implements QuestionJsonPresenter<Question> {
    @Override
    public ImmutableMap<Path, Optional<?>> getAnswerJsonEntries(Question question) {
      ImmutableMap.Builder<Path, Optional<?>> jsonEntries = new ImmutableMap.Builder<>();

      question.getApplicantQuestion().getContextualizedScalars().keySet().stream()
          .filter(path -> !Scalar.getMetadataScalarKeys().contains(path.keyName()))
          .forEachOrdered(
              path -> {
                jsonEntries.put(
                    path.asNestedEntitiesPath(),
                    question.getApplicantQuestion().getApplicantData().readAsString(path));
              });
      return jsonEntries.build();
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

  class EmptyJsonPresenter implements QuestionJsonPresenter<Question> {
    @Override
    public ImmutableMap<Path, Optional<?>> getAnswerJsonEntries(Question question) {
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
      return ImmutableMap.of(
          question.getFileKeyPath().asNestedEntitiesPath(),
          question
              .getApplicantQuestion()
              .createFileUploadQuestion()
              .getFileKeyValue()
              .map(
                  fileKey ->
                      baseUrl
                          + controllers.routes.FileController.acledAdminShow(
                                  URLEncoder.encode(fileKey, StandardCharsets.UTF_8))
                              .url()));
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
}
