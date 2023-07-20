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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.inject.Inject;
import services.LocalizedStrings;
import services.Path;
import services.applicant.question.CurrencyQuestion;
import services.applicant.question.DateQuestion;
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
 * question's answers in JSON form.
 *
 * <p>Some {@link QuestionType}s share the same {@link QuestionJsonPresenter}.
 */
public interface QuestionJsonPresenter<Q extends Question> {

  /** The entries that should be present in a JSON export of answers, for this question. */
  ImmutableMap<Path, ?> getJsonEntries(Q question);

  final class Factory {

    private final CurrencyJsonPresenter currencyJsonPresenter;
    private final ContextualizedScalarsJsonPresenter contextualizedScalarsJsonPresenter;
    private final DateJsonPresenter dateJsonPresenter;
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
        EmptyJsonPresenter emptyJsonPresenter,
        SingleSelectJsonPresenter singleSelectJsonPresenter,
        NumberJsonPresenter numberJsonPresenter,
        FileUploadJsonPresenter fileUploadJsonPresenter,
        MultiSelectJsonPresenter multiSelectJsonPresenter) {
      this.currencyJsonPresenter = checkNotNull(currencyJsonPresenter);
      this.contextualizedScalarsJsonPresenter = checkNotNull(contextualizedScalarsJsonPresenter);
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

          // Answers to enumerator questions are not included. This is because enumerators store an
          // identifier value for each repeated entity, which with the current export logic
          // conflicts with the answers stored for repeated entities.
        case ENUMERATOR:

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

  class FileUploadJsonPresenter implements QuestionJsonPresenter<FileUploadQuestion> {

    private final String baseUrl;

    @Inject
    FileUploadJsonPresenter(SettingsManifest settingsManifest) {
      baseUrl = settingsManifest.getBaseUrl().orElse("");
    }

    @Override
    public ImmutableMap<Path, String> getJsonEntries(FileUploadQuestion question) {
      return ImmutableMap.of(
          question.getApplicantQuestion().getContextualizedPath().join(Scalar.FILE_KEY),
          question
              .getApplicantQuestion()
              .createFileUploadQuestion()
              .getFileKeyValue()
              .map(
                  fileKey ->
                      baseUrl
                          + controllers.routes.FileController.acledAdminShow(
                                  URLEncoder.encode(fileKey, StandardCharsets.UTF_8))
                              .url())
              .orElse(""));
    }
  }

  class MultiSelectJsonPresenter implements QuestionJsonPresenter<MultiSelectQuestion> {

    @Override
    public ImmutableMap<Path, ImmutableList<String>> getJsonEntries(MultiSelectQuestion question) {
      Path path = question.getSelectionPath();

      if (question.getSelectedOptionsValue().isPresent()) {
        ImmutableList<String> selectedOptions =
            question.getSelectedOptionsValue().get().stream()
                .map(LocalizedQuestionOption::optionText)
                .collect(ImmutableList.toImmutableList());

        return ImmutableMap.of(path, selectedOptions);
      }

      return ImmutableMap.of();
    }
  }

  class ContextualizedScalarsJsonPresenter implements QuestionJsonPresenter<Question> {
    @Override
    public ImmutableMap<Path, String> getJsonEntries(Question question) {
      return question.getApplicantQuestion().getContextualizedScalars().keySet().stream()
          .filter(path -> !Scalar.getMetadataScalarKeys().contains(path.keyName()))
          .collect(
              toImmutableMap(
                  path -> path,
                  path ->
                      question
                          .getApplicantQuestion()
                          .getApplicantData()
                          .readAsString(path)
                          .orElse("")));
    }
  }

  class CurrencyJsonPresenter implements QuestionJsonPresenter<CurrencyQuestion> {
    @Override
    public ImmutableMap<Path, Double> getJsonEntries(CurrencyQuestion question) {
      Path path = question.getCurrencyPath().replacingLastSegment("currency_dollars");

      if (question.getCurrencyValue().isPresent()) {
        Long centsTotal = Long.valueOf(question.getCurrencyValue().get().getCents());

        return ImmutableMap.of(path, centsTotal.doubleValue() / 100.0);
      } else {
        return ImmutableMap.of();
      }
    }
  }

  class DateJsonPresenter implements QuestionJsonPresenter<DateQuestion> {
    @Override
    public ImmutableMap<Path, String> getJsonEntries(DateQuestion question) {
      Path path = question.getDatePath();

      if (question.getDateValue().isPresent()) {
        LocalDate date = question.getDateValue().get();
        return ImmutableMap.of(path, DateTimeFormatter.ISO_DATE.format(date));
      } else {
        return ImmutableMap.of();
      }
    }
  }

  class EmptyJsonPresenter implements QuestionJsonPresenter<Question> {
    @Override
    public ImmutableMap<Path, ?> getJsonEntries(Question question) {
      return ImmutableMap.of();
    }
  }

  class NumberJsonPresenter implements QuestionJsonPresenter<NumberQuestion> {
    @Override
    public ImmutableMap<Path, Long> getJsonEntries(NumberQuestion question) {
      Path path = question.getNumberPath();

      if (question.getNumberValue().isPresent()) {
        return ImmutableMap.of(path, question.getNumberValue().get());
      } else {
        return ImmutableMap.of();
      }
    }
  }

  class PhoneJsonPresenter implements QuestionJsonPresenter<PhoneQuestion> {

    private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

    @Override
    public ImmutableMap<Path, String> getJsonEntries(PhoneQuestion question) {
      Path path = question.getPhoneNumberPath();

      if (question.getPhoneNumberValue().isPresent()
          && question.getCountryCodeValue().isPresent()) {
        String formattedPhone =
            getFormattedPhoneNumber(
                question.getPhoneNumberValue().get(), question.getCountryCodeValue().get());
        return ImmutableMap.of(path, formattedPhone);
      } else {
        return ImmutableMap.of();
      }
    }

    /**
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
    public ImmutableMap<Path, String> getJsonEntries(SingleSelectQuestion question) {
      return ImmutableMap.of(
          question.getApplicantQuestion().getContextualizedPath().join(Scalar.SELECTION),
          question
              .getApplicantQuestion()
              .createSingleSelectQuestion()
              .getSelectedOptionValue(LocalizedStrings.DEFAULT_LOCALE)
              .map(LocalizedQuestionOption::optionText)
              .orElse(""));
    }
  }
}
