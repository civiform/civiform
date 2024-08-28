package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import javax.inject.Inject;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.question.AddressQuestion;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.CurrencyQuestion;
import services.applicant.question.DateQuestion;
import services.applicant.question.EmailQuestion;
import services.applicant.question.FileUploadQuestion;
import services.applicant.question.IdQuestion;
import services.applicant.question.MultiSelectQuestion;
import services.applicant.question.NameQuestion;
import services.applicant.question.NumberQuestion;
import services.applicant.question.PhoneQuestion;
import services.applicant.question.Question;
import services.applicant.question.SingleSelectQuestion;
import services.applicant.question.TextQuestion;
import services.program.ProgramQuestionDefinition;
import services.question.LocalizedQuestionOption;
import services.question.QuestionAnswerer;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

/**
 * A {@link QuestionJsonSampler} for a Question {@link Q} provides a strategy for showing the
 * question's answers in JSON form.
 *
 * <p>Some {@link QuestionType}s share the same {@link QuestionJsonSampler}.
 */
public interface QuestionJsonSampler<Q extends Question> {

  default ImmutableMap<Path, Optional<?>> getSampleJsonEntries(
      QuestionDefinition questionDefinition, boolean multipleFileUploadEnabled) {
    if (questionDefinition.getEnumeratorId().isPresent()) {
      // TODO(#5238): support enumerated questions.
      return ImmutableMap.of();
    }

    ProgramQuestionDefinition programQuestionDefinition =
        ProgramQuestionDefinition.create(questionDefinition, Optional.empty());
    ApplicantData applicantData = new ApplicantData();
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(programQuestionDefinition, applicantData, Optional.empty());
    addSampleData(applicantData, applicantQuestion, multipleFileUploadEnabled);

    Q question = getQuestion(applicantQuestion);
    // Suppress warning about unchecked assignment because the JSON presenter is parameterized on
    // the question type, which we know matches Q.
    @SuppressWarnings("unchecked")
    ImmutableMap<Path, Optional<?>> entries =
        getJsonPresenter().getAllJsonEntries(question, multipleFileUploadEnabled);

    return entries;
  }

  Q getQuestion(ApplicantQuestion applicantQuestion);

  void addSampleData(
      ApplicantData applicantData,
      ApplicantQuestion applicantQuestion,
      boolean multipleFileUploadEnabled);

  QuestionJsonPresenter getJsonPresenter();

  final class Factory {

    private final AddressJsonSampler addressJsonSampler;
    private final CurrencyJsonSampler currencyJsonSampler;
    private final DateJsonSampler dateJsonSampler;
    private final EmailJsonSampler emailJsonSampler;
    private final EmptyJsonSampler emptyJsonSampler;
    private final FileUploadJsonSampler fileUploadJsonSampler;
    private final IdJsonSampler idJsonSampler;
    private final MultiSelectJsonSampler multiSelectJsonSampler;
    private final NameJsonSampler nameJsonSampler;
    private final NumberJsonSampler numberJsonSampler;
    private final PhoneJsonSampler phoneJsonSampler;
    private final SingleSelectJsonSampler singleSelectJsonSampler;
    private final TextJsonSampler textJsonSampler;

    @Inject
    Factory(
        AddressJsonSampler addressJsonSampler,
        CurrencyJsonSampler currencyJsonSampler,
        DateJsonSampler dateJsonSampler,
        EmailJsonSampler emailJsonSampler,
        EmptyJsonSampler emptyJsonSampler,
        FileUploadJsonSampler fileUploadJsonSampler,
        IdJsonSampler idJsonSampler,
        MultiSelectJsonSampler multiSelectJsonSampler,
        NameJsonSampler nameJsonSampler,
        NumberJsonSampler numberJsonSampler,
        PhoneJsonSampler phoneJsonSampler,
        SingleSelectJsonSampler singleSelectJsonSampler,
        TextJsonSampler textJsonSampler) {
      this.addressJsonSampler = checkNotNull(addressJsonSampler);
      this.currencyJsonSampler = checkNotNull(currencyJsonSampler);
      this.dateJsonSampler = checkNotNull(dateJsonSampler);
      this.emailJsonSampler = checkNotNull(emailJsonSampler);
      this.emptyJsonSampler = checkNotNull(emptyJsonSampler);
      this.fileUploadJsonSampler = checkNotNull(fileUploadJsonSampler);
      this.idJsonSampler = checkNotNull(idJsonSampler);
      this.multiSelectJsonSampler = checkNotNull(multiSelectJsonSampler);
      this.nameJsonSampler = checkNotNull(nameJsonSampler);
      this.numberJsonSampler = checkNotNull(numberJsonSampler);
      this.phoneJsonSampler = checkNotNull(phoneJsonSampler);
      this.singleSelectJsonSampler = checkNotNull(singleSelectJsonSampler);
      this.textJsonSampler = checkNotNull(textJsonSampler);
    }

    public QuestionJsonSampler create(QuestionType questionType) {
      switch (questionType) {
        case ADDRESS:
          return addressJsonSampler;
        case CHECKBOX:
          return multiSelectJsonSampler;
        case CURRENCY:
          return currencyJsonSampler;
        case DATE:
          return dateJsonSampler;
        case DROPDOWN:
        case RADIO_BUTTON:
          return singleSelectJsonSampler;
        case EMAIL:
          return emailJsonSampler;
          // Answers to enumerator questions are not included. This is because enumerators store an
          // identifier value for each repeated entity, which with the current export logic
          // conflicts with the answers stored for repeated entities.
        case ENUMERATOR:
          return emptyJsonSampler;
        case FILEUPLOAD:
          return fileUploadJsonSampler;
        case ID:
          return idJsonSampler;
        case NAME:
          return nameJsonSampler;
        case NUMBER:
          return numberJsonSampler;
        case PHONE:
          return phoneJsonSampler;
          // Static content questions are not included in API responses because they
          // do not include an answer from the user.
        case STATIC:
          return emptyJsonSampler;
        case TEXT:
          return textJsonSampler;

        default:
          throw new RuntimeException(String.format("Unrecognized questionType %s", questionType));
      }
    }
  }

  class AddressJsonSampler implements QuestionJsonSampler<AddressQuestion> {
    private final QuestionJsonPresenter addressJsonPresenter;

    @Inject
    AddressJsonSampler(QuestionJsonPresenter.Factory questionJsonPresenterFactory) {
      this.addressJsonPresenter = questionJsonPresenterFactory.create(QuestionType.ADDRESS);
    }

    @Override
    public AddressQuestion getQuestion(ApplicantQuestion applicantQuestion) {
      return applicantQuestion.createAddressQuestion();
    }

    @Override
    public void addSampleData(
        ApplicantData applicantData,
        ApplicantQuestion applicantQuestion,
        boolean multipleFileUploadEnabled) {
      QuestionAnswerer.answerAddressQuestion(
          applicantData,
          applicantQuestion.getContextualizedPath(),
          /* street= */ "742 Evergreen Terrace",
          /* line2= */ "",
          /* city= */ "Springfield",
          /* state= */ "OR",
          /* zip= */ "97403",
          /* corrected= */ "Corrected",
          /* latitude= */ 44.0462,
          /* longitude= */ -123.0236,
          /* wellKnownId= */ 4326L,
          /* serviceArea= */ "springfieldCounty_InArea_1709069741,portland_NotInArea_1709069741");
    }

    @Override
    public QuestionJsonPresenter getJsonPresenter() {
      return addressJsonPresenter;
    }
  }

  class CurrencyJsonSampler implements QuestionJsonSampler<CurrencyQuestion> {

    private final QuestionJsonPresenter currencyJsonPresenter;

    @Inject
    CurrencyJsonSampler(QuestionJsonPresenter.Factory questionJsonPresenterFactory) {
      this.currencyJsonPresenter = questionJsonPresenterFactory.create(QuestionType.CURRENCY);
    }

    @Override
    public CurrencyQuestion getQuestion(ApplicantQuestion applicantQuestion) {
      return applicantQuestion.createCurrencyQuestion();
    }

    @Override
    public void addSampleData(
        ApplicantData applicantData,
        ApplicantQuestion applicantQuestion,
        boolean multipleFileUploadEnabled) {
      QuestionAnswerer.answerCurrencyQuestion(
          applicantData, applicantQuestion.getContextualizedPath(), "123.45");
    }

    @Override
    public QuestionJsonPresenter getJsonPresenter() {
      return currencyJsonPresenter;
    }
  }

  class DateJsonSampler implements QuestionJsonSampler<DateQuestion> {
    private final QuestionJsonPresenter dateJsonPresenter;

    @Inject
    DateJsonSampler(QuestionJsonPresenter.Factory questionJsonPresenterFactory) {
      this.dateJsonPresenter = questionJsonPresenterFactory.create(QuestionType.DATE);
    }

    @Override
    public DateQuestion getQuestion(ApplicantQuestion applicantQuestion) {
      return applicantQuestion.createDateQuestion();
    }

    @Override
    public void addSampleData(
        ApplicantData applicantData,
        ApplicantQuestion applicantQuestion,
        boolean multipleFileUploadEnabled) {
      QuestionAnswerer.answerDateQuestion(
          applicantData, applicantQuestion.getContextualizedPath(), "2023-01-02");
    }

    @Override
    public QuestionJsonPresenter getJsonPresenter() {
      return dateJsonPresenter;
    }
  }

  class EmailJsonSampler implements QuestionJsonSampler<EmailQuestion> {
    private final QuestionJsonPresenter emailJsonPresenter;

    @Inject
    EmailJsonSampler(QuestionJsonPresenter.Factory questionJsonPresenterFactory) {
      this.emailJsonPresenter = questionJsonPresenterFactory.create(QuestionType.EMAIL);
    }

    @Override
    public EmailQuestion getQuestion(ApplicantQuestion applicantQuestion) {
      return applicantQuestion.createEmailQuestion();
    }

    @Override
    public void addSampleData(
        ApplicantData applicantData,
        ApplicantQuestion applicantQuestion,
        boolean multipleFileUploadEnabled) {
      QuestionAnswerer.answerEmailQuestion(
          applicantData,
          applicantQuestion.getContextualizedPath(),
          "homer.simpson@springfield.gov");
    }

    @Override
    public QuestionJsonPresenter getJsonPresenter() {
      return emailJsonPresenter;
    }
  }

  class EmptyJsonSampler implements QuestionJsonSampler<Question> {

    @Override
    public ImmutableMap<Path, Optional<?>> getSampleJsonEntries(
        QuestionDefinition questionDefinition, boolean multipleFileUploadEnabled) {
      return ImmutableMap.of();
    }

    @Override
    public Question getQuestion(ApplicantQuestion applicantQuestion) {
      return null;
    }

    @Override
    public void addSampleData(
        ApplicantData applicantData,
        ApplicantQuestion applicantQuestion,
        boolean multipleFileUploadEnabled) {
      // no-op
    }

    @Override
    public QuestionJsonPresenter getJsonPresenter() {
      return null;
    }
  }

  class FileUploadJsonSampler implements QuestionJsonSampler<FileUploadQuestion> {
    private final QuestionJsonPresenter fileUploadJsonPresenter;

    @Inject
    FileUploadJsonSampler(QuestionJsonPresenter.Factory questionJsonPresenterFactory) {
      this.fileUploadJsonPresenter = questionJsonPresenterFactory.create(QuestionType.FILEUPLOAD);
    }

    @Override
    public FileUploadQuestion getQuestion(ApplicantQuestion applicantQuestion) {
      return applicantQuestion.createFileUploadQuestion();
    }

    @Override
    public void addSampleData(
        ApplicantData applicantData,
        ApplicantQuestion applicantQuestion,
        boolean multipleFileUploadEnabled) {
      if (multipleFileUploadEnabled) {
        QuestionAnswerer.answerFileQuestionWithMultipleUpload(
            applicantData,
            applicantQuestion.getContextualizedPath(),
            ImmutableList.of("my-file-key-1", "my-file-key-2"));
      } else {
        QuestionAnswerer.answerFileQuestion(
            applicantData, applicantQuestion.getContextualizedPath(), "my-file-key");
      }
    }

    @Override
    public QuestionJsonPresenter getJsonPresenter() {
      return fileUploadJsonPresenter;
    }
  }

  class IdJsonSampler implements QuestionJsonSampler<IdQuestion> {
    private final QuestionJsonPresenter idJsonPresenter;

    @Inject
    IdJsonSampler(QuestionJsonPresenter.Factory questionJsonPresenterFactory) {
      this.idJsonPresenter = questionJsonPresenterFactory.create(QuestionType.ID);
    }

    @Override
    public IdQuestion getQuestion(ApplicantQuestion applicantQuestion) {
      return applicantQuestion.createIdQuestion();
    }

    @Override
    public void addSampleData(
        ApplicantData applicantData,
        ApplicantQuestion applicantQuestion,
        boolean multipleFileUploadEnabled) {
      QuestionAnswerer.answerIdQuestion(
          applicantData, applicantQuestion.getContextualizedPath(), "12345");
    }

    @Override
    public QuestionJsonPresenter getJsonPresenter() {
      return idJsonPresenter;
    }
  }

  class MultiSelectJsonSampler implements QuestionJsonSampler<MultiSelectQuestion> {

    private final QuestionJsonPresenter multiSelectJsonPresenter;

    @Inject
    MultiSelectJsonSampler(QuestionJsonPresenter.Factory questionJsonPresenterFactory) {
      // Any multi-select QuestionType passed to create() works here (as of writing, only CHECKBOX).
      this.multiSelectJsonPresenter = questionJsonPresenterFactory.create(QuestionType.CHECKBOX);
    }

    @Override
    public MultiSelectQuestion getQuestion(ApplicantQuestion applicantQuestion) {
      return applicantQuestion.createMultiSelectQuestion();
    }

    @Override
    public void addSampleData(
        ApplicantData applicantData,
        ApplicantQuestion applicantQuestion,
        boolean multipleFileUploadEnabled) {
      ImmutableList<LocalizedQuestionOption> questionOptions =
          applicantQuestion.createMultiSelectQuestion().getOptions();

      // Add up to two options to the sample data.
      if (questionOptions.size() > 0) {
        QuestionAnswerer.answerMultiSelectQuestion(
            applicantData,
            applicantQuestion.getContextualizedPath(),
            /* index= */ 0,
            questionOptions.get(0).id());
      }
      if (questionOptions.size() > 1) {
        QuestionAnswerer.answerMultiSelectQuestion(
            applicantData,
            applicantQuestion.getContextualizedPath(),
            /* index= */ 1,
            questionOptions.get(1).id());
      }
    }

    @Override
    public QuestionJsonPresenter getJsonPresenter() {
      return multiSelectJsonPresenter;
    }
  }

  class NameJsonSampler implements QuestionJsonSampler<NameQuestion> {
    private final QuestionJsonPresenter nameJsonPresenter;

    @Inject
    NameJsonSampler(QuestionJsonPresenter.Factory questionJsonPresenterFactory) {
      this.nameJsonPresenter = questionJsonPresenterFactory.create(QuestionType.NAME);
    }

    @Override
    public NameQuestion getQuestion(ApplicantQuestion applicantQuestion) {
      return applicantQuestion.createNameQuestion();
    }

    @Override
    public void addSampleData(
        ApplicantData applicantData,
        ApplicantQuestion applicantQuestion,
        boolean multipleFileUploadEnabled) {
      QuestionAnswerer.answerNameQuestion(
          applicantData,
          applicantQuestion.getContextualizedPath(),
          "Homer",
          "Jay",
          "Simpson",
          "Jr.");
    }

    @Override
    public QuestionJsonPresenter getJsonPresenter() {
      return nameJsonPresenter;
    }
  }

  class NumberJsonSampler implements QuestionJsonSampler<NumberQuestion> {

    private final QuestionJsonPresenter numberJsonPresenter;

    @Inject
    NumberJsonSampler(QuestionJsonPresenter.Factory questionJsonPresenterFactory) {
      this.numberJsonPresenter = questionJsonPresenterFactory.create(QuestionType.NUMBER);
    }

    @Override
    public NumberQuestion getQuestion(ApplicantQuestion applicantQuestion) {
      return applicantQuestion.createNumberQuestion();
    }

    @Override
    public void addSampleData(
        ApplicantData applicantData,
        ApplicantQuestion applicantQuestion,
        boolean multipleFileUploadEnabled) {
      QuestionAnswerer.answerNumberQuestion(
          applicantData, applicantQuestion.getContextualizedPath(), 12321);
    }

    @Override
    public QuestionJsonPresenter getJsonPresenter() {
      return numberJsonPresenter;
    }
  }

  class PhoneJsonSampler implements QuestionJsonSampler<PhoneQuestion> {
    private final QuestionJsonPresenter phoneJsonPresenter;

    @Inject
    PhoneJsonSampler(QuestionJsonPresenter.Factory questionJsonPresenterFactory) {
      this.phoneJsonPresenter = questionJsonPresenterFactory.create(QuestionType.PHONE);
    }

    @Override
    public PhoneQuestion getQuestion(ApplicantQuestion applicantQuestion) {
      return applicantQuestion.createPhoneQuestion();
    }

    @Override
    public void addSampleData(
        ApplicantData applicantData,
        ApplicantQuestion applicantQuestion,
        boolean multipleFileUploadEnabled) {
      QuestionAnswerer.answerPhoneQuestion(
          applicantData, applicantQuestion.getContextualizedPath(), "US", "(214)-367-3764");
    }

    @Override
    public QuestionJsonPresenter getJsonPresenter() {
      return phoneJsonPresenter;
    }
  }

  class SingleSelectJsonSampler implements QuestionJsonSampler<SingleSelectQuestion> {

    private final QuestionJsonPresenter singleSelectJsonPresenter;

    @Inject
    SingleSelectJsonSampler(QuestionJsonPresenter.Factory questionJsonPresenterFactory) {
      // Any single-select QuestionType passed to create() works here.
      this.singleSelectJsonPresenter =
          questionJsonPresenterFactory.create(QuestionType.RADIO_BUTTON);
    }

    @Override
    public SingleSelectQuestion getQuestion(ApplicantQuestion applicantQuestion) {
      return applicantQuestion.createSingleSelectQuestion();
    }

    @Override
    public void addSampleData(
        ApplicantData applicantData,
        ApplicantQuestion applicantQuestion,
        boolean multipleFileUploadEnabled) {
      ImmutableList<LocalizedQuestionOption> questionOptions =
          applicantQuestion.createSingleSelectQuestion().getOptions();

      if (questionOptions.size() != 0) {
        LocalizedQuestionOption firstOption = questionOptions.get(0);
        QuestionAnswerer.answerSingleSelectQuestion(
            applicantData, applicantQuestion.getContextualizedPath(), firstOption.id());
      }
    }

    @Override
    public QuestionJsonPresenter getJsonPresenter() {
      return singleSelectJsonPresenter;
    }
  }

  class TextJsonSampler implements QuestionJsonSampler<TextQuestion> {
    private final QuestionJsonPresenter emailJsonPresenter;

    @Inject
    TextJsonSampler(QuestionJsonPresenter.Factory questionJsonPresenterFactory) {
      this.emailJsonPresenter = questionJsonPresenterFactory.create(QuestionType.TEXT);
    }

    @Override
    public TextQuestion getQuestion(ApplicantQuestion applicantQuestion) {
      return applicantQuestion.createTextQuestion();
    }

    @Override
    public void addSampleData(
        ApplicantData applicantData,
        ApplicantQuestion applicantQuestion,
        boolean multipleFileUploadEnabled) {
      QuestionAnswerer.answerTextQuestion(
          applicantData, applicantQuestion.getContextualizedPath(), "I love CiviForm!");
    }

    @Override
    public QuestionJsonPresenter getJsonPresenter() {
      return emailJsonPresenter;
    }
  }
}
