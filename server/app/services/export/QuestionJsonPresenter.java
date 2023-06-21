package services.export;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.inject.Inject;
import services.LocalizedStrings;
import services.Path;
import services.applicant.AnswerData;
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

public interface QuestionJsonPresenter<Q extends Question> {

  /** The entries that should be present in a JSON export of answers, for this question. */
  ImmutableMap<Path, ?> getJsonEntries(Q question);

  class Factory {

    private final CurrencyJsonPresenter currencyJsonPresenter;
    private final ContextualizedScalarsJsonPresenter contextualizedScalarsJsonPresenter;
    private final DateJsonPresenter dateJsonPresenter;
    private final EmptyJsonPresenter emptyJsonPresenter;
    private final FileUploadQuestionJsonPresenter fileUploadQuestionJsonPresenter;
    private final NumberQuestionJsonPresenter numberQuestionJsonPresenter;
    private final PhoneQuestionJsonPresenter phoneQuestionJsonPresenter;
    private final MultiSelectQuestionJsonPresenter multiSelectQuestionJsonPresenter;
    private final SingleSelectQuestionJsonPresenter singleSelectQuestionJsonPresenter;

    @Inject
    Factory(
        CurrencyJsonPresenter currencyJsonPresenter,
        ContextualizedScalarsJsonPresenter contextualizedScalarsJsonPresenter,
        DateJsonPresenter dateJsonPresenter,
        PhoneQuestionJsonPresenter phoneQuestionJsonPresenter,
        EmptyJsonPresenter emptyJsonPresenter,
        SingleSelectQuestionJsonPresenter singleSelectQuestionJsonPresenter,
        NumberQuestionJsonPresenter numberQuestionJsonPresenter,
        FileUploadQuestionJsonPresenter fileUploadQuestionJsonPresenter,
        MultiSelectQuestionJsonPresenter multiSelectQuestionJsonPresenter) {
      this.currencyJsonPresenter = currencyJsonPresenter;
      this.contextualizedScalarsJsonPresenter = contextualizedScalarsJsonPresenter;
      this.emptyJsonPresenter = emptyJsonPresenter;
      this.dateJsonPresenter = dateJsonPresenter;
      this.fileUploadQuestionJsonPresenter = fileUploadQuestionJsonPresenter;
      this.numberQuestionJsonPresenter = numberQuestionJsonPresenter;
      this.phoneQuestionJsonPresenter = phoneQuestionJsonPresenter;
      this.multiSelectQuestionJsonPresenter = multiSelectQuestionJsonPresenter;
      this.singleSelectQuestionJsonPresenter = singleSelectQuestionJsonPresenter;
    }

    QuestionJsonPresenter create(AnswerData answerData) {
      QuestionType type = answerData.applicantQuestion().getType();
      switch (type) {
        case ADDRESS:
        case EMAIL:
        case ID:
        case NAME:
        case TEXT:
          return contextualizedScalarsJsonPresenter;

          // Answers to enumerator questions are not be included because the path is
          // incompatible with the JSON export schema. This is because enumerators store an
          // identifier
          // value for each repeated entity, which with the current export logic conflicts with the
          // answers stored for repeated entities.
        case ENUMERATOR:

          // Static content questions are not included in API responses because they
          // do not include an answer from the user.
        case STATIC:
          return emptyJsonPresenter;

        case CHECKBOX:
          return multiSelectQuestionJsonPresenter;
        case FILEUPLOAD:
          return fileUploadQuestionJsonPresenter;
        case NUMBER:
          return numberQuestionJsonPresenter;
        case PHONE:
          return phoneQuestionJsonPresenter;
        case RADIO_BUTTON:
        case DROPDOWN:
          return singleSelectQuestionJsonPresenter;
        case CURRENCY:
          return currencyJsonPresenter;
        case DATE:
          return dateJsonPresenter;

        default:
          throw new RuntimeException(String.format("Unrecognized questionType %s", type));
      }
    }
  }

  class FileUploadQuestionJsonPresenter implements QuestionJsonPresenter<FileUploadQuestion> {

    private final String baseUrl;

    @Inject
    FileUploadQuestionJsonPresenter(Config config) {
      baseUrl = config.getString("base_url");
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

  class MultiSelectQuestionJsonPresenter implements QuestionJsonPresenter<MultiSelectQuestion> {

    @Override
    public ImmutableMap<Path, ImmutableList<String>> getJsonEntries(MultiSelectQuestion question) {
      Path path = question.getSelectionPath().asApplicationPath();

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
    public ImmutableMap<Path, Double> getJsonEntries(CurrencyQuestion question) {
      Path path =
          question.getCurrencyPath().asApplicationPath().replacingLastSegment("currency_dollars");

      if (question.getCurrencyValue().isPresent()) {
        Long centsTotal = Long.valueOf(question.getCurrencyValue().get().getCents());

        return ImmutableMap.of(path, centsTotal.doubleValue() / 100.0);
      } else {
        return ImmutableMap.of();
      }
    }
  }

  class DateJsonPresenter implements QuestionJsonPresenter<DateQuestion> {
    public ImmutableMap<Path, String> getJsonEntries(DateQuestion question) {
      Path path = question.getDatePath().asApplicationPath();

      if (question.getDateValue().isPresent()) {
        LocalDate date = question.getDateValue().get();
        return ImmutableMap.of(path, DateTimeFormatter.ISO_DATE.format(date));
      } else {
        return ImmutableMap.of();
      }
    }
  }

  class MultiSelectJsonPresenter implements QuestionJsonPresenter<MultiSelectQuestion> {
    public ImmutableMap<Path, ImmutableList<String>> getJsonEntries(MultiSelectQuestion question) {
      Path path = question.getSelectionPath().asApplicationPath();

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

  class EmptyJsonPresenter implements QuestionJsonPresenter<Question> {
    public ImmutableMap<Path, ?> getJsonEntries(Question question) {
      return ImmutableMap.of();
    }
  }

  class NumberQuestionJsonPresenter implements QuestionJsonPresenter<NumberQuestion> {
    public ImmutableMap<Path, Long> getJsonEntries(NumberQuestion question) {
      Path path = question.getNumberPath().asApplicationPath();

      if (question.getNumberValue().isPresent()) {
        return ImmutableMap.of(path, question.getNumberValue().get());
      } else {
        return ImmutableMap.of();
      }
    }
  }

  class PhoneQuestionJsonPresenter implements QuestionJsonPresenter<PhoneQuestion> {
    public ImmutableMap<Path, String> getJsonEntries(PhoneQuestion question) {
      Path path = question.getPhoneNumberPath().asApplicationPath();

      if (question.getPhoneNumberValue().isPresent()
          && question.getCountryCodeValue().isPresent()) {
        String formattedPhone =
            question.getFormattedPhoneNumber(
                question.getPhoneNumberValue().get(), question.getCountryCodeValue().get());
        return ImmutableMap.of(path, formattedPhone);
      } else {
        return ImmutableMap.of();
      }
    }
  }

  class SingleSelectQuestionJsonPresenter implements QuestionJsonPresenter<SingleSelectQuestion> {
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
