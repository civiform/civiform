package services.export;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

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
import services.LocalizedStrings;
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
import services.question.LocalizedQuestionOption;
import services.question.types.QuestionType;
import services.settings.SettingsManifest;

/**
 * A {@link QuestionJsonPresenter} for a Question {@link Q} provides a strategy for showing the
 * question's answers (of type {@link T}) in JSON form.
 *
 * <p>Some {@link QuestionType}s share the same {@link QuestionJsonPresenter}.
 */
public interface QuestionJsonPresenter<Q extends Question, T> {

  /**
   * The entries that should be present in a JSON export of answers, for this question.
   *
   * <p>The returned map's keys are the {@link Path} where the value should be set, and values are
   * what should be set at the path. The Optional is empty if the question was unanswered, and
   * present if the question was answered or another specific value should be set at the path.
   *
   * <p>If the "unanswered" representation of the question is an empty list, the path should include
   * an array element (e.g. `[]`).
   *
   * <p>The returned map is only empty when a question should not be included in the response at
   * all, such as with static questions.
   *
   * @param question the Question to build a JSON response for.
   * @return a map of paths to Optional answer values.
   */
  ImmutableMap<Path, Optional<T>> getJsonEntries(Q question);

  final class Factory {

    private final CurrencyJsonPresenter currencyJsonPresenter;
    private final ContextualizedScalarsJsonPresenter contextualizedScalarsJsonPresenter;
    private final DateJsonPresenter dateJsonPresenter;
    private final EnumeratorJsonPresenter enumeratorJsonPresenter;
    private final EmptyJsonPresenter emptyJsonPresenter;
    private final FileUploadJsonPresenter fileUploadJsonPresenter;
    private final NumberJsonPresenter numberJsonPresenter;
    private final PhoneJsonPresenter phoneJsonPresenter;
    private final MultiSelectJsonPresenter multiSelectJsonPresenter;
    private final SingleSelectJsonPresenter singleSelectJsonPresenter;

    @Inject
    Factory(
        CurrencyJsonPresenter currencyJsonPresenter,
        ContextualizedScalarsJsonPresenter contextualizedScalarsJsonPresenter,
        DateJsonPresenter dateJsonPresenter,
        PhoneJsonPresenter phoneJsonPresenter,
        EnumeratorJsonPresenter enumeratorJsonPresenter,
        EmptyJsonPresenter emptyJsonPresenter,
        SingleSelectJsonPresenter singleSelectJsonPresenter,
        NumberJsonPresenter numberJsonPresenter,
        FileUploadJsonPresenter fileUploadJsonPresenter,
        MultiSelectJsonPresenter multiSelectJsonPresenter) {
      this.currencyJsonPresenter = checkNotNull(currencyJsonPresenter);
      this.contextualizedScalarsJsonPresenter = checkNotNull(contextualizedScalarsJsonPresenter);
      this.enumeratorJsonPresenter = checkNotNull(enumeratorJsonPresenter);
      this.emptyJsonPresenter = checkNotNull(emptyJsonPresenter);
      this.dateJsonPresenter = checkNotNull(dateJsonPresenter);
      this.fileUploadJsonPresenter = checkNotNull(fileUploadJsonPresenter);
      this.numberJsonPresenter = checkNotNull(numberJsonPresenter);
      this.phoneJsonPresenter = checkNotNull(phoneJsonPresenter);
      this.multiSelectJsonPresenter = checkNotNull(multiSelectJsonPresenter);
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
        case ENUMERATOR:
          return enumeratorJsonPresenter;

          // Static content questions are not included in API responses because they
          // do not include an answer from the user.
        case STATIC:
          return emptyJsonPresenter;

        case CHECKBOX:
          return multiSelectJsonPresenter;
        case FILEUPLOAD:
          return fileUploadJsonPresenter;
        case NUMBER:
          return numberJsonPresenter;
        case PHONE:
          return phoneJsonPresenter;
        case RADIO_BUTTON:
        case DROPDOWN:
          return singleSelectJsonPresenter;
        case CURRENCY:
          return currencyJsonPresenter;
        case DATE:
          return dateJsonPresenter;

        default:
          throw new RuntimeException(String.format("Unrecognized questionType %s", questionType));
      }
    }
  }

  class FileUploadJsonPresenter implements QuestionJsonPresenter<FileUploadQuestion, String> {

    private final String baseUrl;

    @Inject
    FileUploadJsonPresenter(SettingsManifest settingsManifest) {
      baseUrl = settingsManifest.getBaseUrl().orElse("");
    }

    @Override
    public ImmutableMap<Path, Optional<String>> getJsonEntries(FileUploadQuestion question) {
      return ImmutableMap.of(
          question.getFileKeyPath(),
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

  class MultiSelectJsonPresenter
      implements QuestionJsonPresenter<MultiSelectQuestion, ImmutableList<String>> {

    @Override
    public ImmutableMap<Path, Optional<ImmutableList<String>>> getJsonEntries(
        MultiSelectQuestion question) {
      Path path = question.getSelectionPath();

      ImmutableList<String> selectedOptions =
          question.getSelectedOptionsValue().orElse(ImmutableList.of()).stream()
              .map(LocalizedQuestionOption::optionText)
              .collect(ImmutableList.toImmutableList());

      if (selectedOptions.isEmpty()) {
        return ImmutableMap.of(path.asArrayElement(), Optional.empty());
      }

      return ImmutableMap.of(path, Optional.of(selectedOptions));
    }
  }

  class ContextualizedScalarsJsonPresenter implements QuestionJsonPresenter<Question, String> {
    @Override
    public ImmutableMap<Path, Optional<String>> getJsonEntries(Question question) {
      return question.getApplicantQuestion().getContextualizedScalars().keySet().stream()
          .filter(path -> !Scalar.getMetadataScalarKeys().contains(path.keyName()))
          .collect(
              toImmutableMap(
                  path -> path,
                  path -> question.getApplicantQuestion().getApplicantData().readAsString(path)));
    }
  }

  class CurrencyJsonPresenter implements QuestionJsonPresenter<CurrencyQuestion, Double> {
    @Override
    public ImmutableMap<Path, Optional<Double>> getJsonEntries(CurrencyQuestion question) {
      Path path = question.getCurrencyPath().replacingLastSegment("currency_dollars");

      return ImmutableMap.of(
          path, question.getCurrencyValue().map(v -> v.getCents().doubleValue() / 100.0));
    }
  }

  class DateJsonPresenter implements QuestionJsonPresenter<DateQuestion, String> {
    @Override
    public ImmutableMap<Path, Optional<String>> getJsonEntries(DateQuestion question) {
      Path path = question.getDatePath();

      return ImmutableMap.of(path, question.getDateValue().map(DateTimeFormatter.ISO_DATE::format));
    }
  }

  class EnumeratorJsonPresenter implements QuestionJsonPresenter<EnumeratorQuestion, String> {
    @Override
    public ImmutableMap<Path, Optional<String>> getJsonEntries(EnumeratorQuestion question) {
      Path repeatedEntityPath = question.getApplicantQuestion().getContextualizedPath();
      if (!question.isAnswered()) {
        return ImmutableMap.of(repeatedEntityPath, Optional.empty());
      }

      ImmutableList<String> entityNames = question.getEntityNames();
      ImmutableMap.Builder<Path, Optional<String>> jsonEntries = ImmutableMap.builder();

      for (int i = 0; i < entityNames.size(); i++) {
        jsonEntries.put(
            repeatedEntityPath.atIndex(i).join(Scalar.ENTITY_NAME),
            Optional.of(entityNames.get(i)));
      }

      return jsonEntries.build();
    }
  }

  class EmptyJsonPresenter implements QuestionJsonPresenter<Question, String> {
    @Override
    public ImmutableMap<Path, Optional<String>> getJsonEntries(Question question) {
      return ImmutableMap.of();
    }
  }

  class NumberJsonPresenter implements QuestionJsonPresenter<NumberQuestion, Long> {
    @Override
    public ImmutableMap<Path, Optional<Long>> getJsonEntries(NumberQuestion question) {
      Path path = question.getNumberPath();
      return ImmutableMap.of(path, question.getNumberValue());
    }
  }

  class PhoneJsonPresenter implements QuestionJsonPresenter<PhoneQuestion, String> {
    private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

    @Override
    public ImmutableMap<Path, Optional<String>> getJsonEntries(PhoneQuestion question) {
      Path path = question.getPhoneNumberPath();

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

  class SingleSelectJsonPresenter implements QuestionJsonPresenter<SingleSelectQuestion, String> {
    @Override
    public ImmutableMap<Path, Optional<String>> getJsonEntries(SingleSelectQuestion question) {
      return ImmutableMap.of(
          question.getSelectionPath(),
          question
              .getApplicantQuestion()
              .createSingleSelectQuestion()
              .getSelectedOptionValue(LocalizedStrings.DEFAULT_LOCALE)
              .map(LocalizedQuestionOption::optionText));
    }
  }
}
