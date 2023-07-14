package services.export;

import static com.google.common.base.Preconditions.checkNotNull;
import static services.export.JsonExporter.exportToJsonApplication;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import javax.inject.Inject;
import services.CfJsonDocumentContext;
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
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

/**
 * A {@link QuestionJsonSampler} for a Question {@link Q} provides a strategy for showing the
 * question's answers in JSON form.
 *
 * <p>Some {@link QuestionType}s share the same {@link QuestionJsonSampler}.
 */
public interface QuestionJsonSampler<Q extends Question> {

  default CfJsonDocumentContext getSampleJson(QuestionDefinition questionDefinition) {
    if (questionDefinition.getEnumeratorId().isPresent()) {
      // TODO(#5238): support enumerated questions.
      return new CfJsonDocumentContext();
    }

    ProgramQuestionDefinition programQuestionDefinition =
        ProgramQuestionDefinition.create(questionDefinition, Optional.empty());
    ApplicantData applicantData = new ApplicantData();
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(programQuestionDefinition, applicantData, Optional.empty());

    Q question = getQuestion(applicantQuestion);
    addSampleData(applicantData, question);

    @SuppressWarnings("unchecked")
    ImmutableMap<Path, ?> entries = getJsonPresenter().getJsonEntries(question);
    CfJsonDocumentContext jsonApplication = new CfJsonDocumentContext();

    entries.entrySet().forEach(entry -> exportToJsonApplication(jsonApplication, entry));

    return jsonApplication;
  }

  Q getQuestion(ApplicantQuestion applicantQuestion);

  void addSampleData(ApplicantData applicantData, Q question);

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
        case EMAIL:
          return emailJsonSampler;
        case ID:
          return idJsonSampler;
        case NAME:
          return nameJsonSampler;
        case TEXT:
          return textJsonSampler;

          // Answers to enumerator questions are not included. This is because enumerators store an
          // identifier value for each repeated entity, which with the current export logic
          // conflicts with the answers stored for repeated entities.
        case ENUMERATOR:

          // Static content questions are not included in API responses because they
          // do not include an answer from the user.
        case STATIC:
          return emptyJsonSampler;

        case CHECKBOX:
          return multiSelectJsonSampler;
        case FILEUPLOAD:
          return fileUploadJsonSampler;
        case NUMBER:
          return numberJsonSampler;
        case PHONE:
          return phoneJsonSampler;
        case RADIO_BUTTON:
        case DROPDOWN:
          return singleSelectJsonSampler;
        case CURRENCY:
          return currencyJsonSampler;
        case DATE:
          return dateJsonSampler;

        default:
          throw new RuntimeException(String.format("Unrecognized questionType %s", questionType));
      }
    }
  }

  class AddressJsonSampler extends ContextualizedScalarsJsonSampler<AddressQuestion> {
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
    public void addSampleData(ApplicantData applicantData, AddressQuestion question) {
      applicantData.putString(question.getStreetPath(), "742 Evergreen Terrace");
      applicantData.putString(question.getCityPath(), "Springfield");
      applicantData.putString(question.getStatePath(), "OR");
      applicantData.putString(question.getZipPath(), "97403");
      applicantData.putString(question.getLatitudePath(), "44.0462");
      applicantData.putString(question.getLongitudePath(), "-123.0236");
      applicantData.putString(question.getWellKnownIdPath(), "23214");
      applicantData.putString(question.getServiceAreaPath(), "springfield_county");
    }

    @Override
    public QuestionJsonPresenter getJsonPresenter() {
      return addressJsonPresenter;
    }
  }

  abstract class ContextualizedScalarsJsonSampler<Q extends Question>
      implements QuestionJsonSampler<Q> {

    @Override
    public CfJsonDocumentContext getSampleJson(QuestionDefinition questionDefinition) {
      ProgramQuestionDefinition programQuestionDefinition =
          ProgramQuestionDefinition.create(questionDefinition, Optional.empty());
      ApplicantData applicantData = new ApplicantData();
      ApplicantQuestion applicantQuestion =
          new ApplicantQuestion(programQuestionDefinition, applicantData, Optional.empty());

      Q question = getQuestion(applicantQuestion);
      addSampleData(applicantData, question);

      @SuppressWarnings("unchecked")
      ImmutableMap<Path, ?> entries = getJsonPresenter().getJsonEntries(question);
      CfJsonDocumentContext jsonApplication = new CfJsonDocumentContext();

      entries.entrySet().forEach(entry -> exportToJsonApplication(jsonApplication, entry));

      return jsonApplication;
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
    public void addSampleData(ApplicantData applicantData, CurrencyQuestion question) {
      applicantData.putCurrencyDollars(question.getCurrencyPath(), "123.45");
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
    public void addSampleData(ApplicantData applicantData, DateQuestion question) {
      applicantData.putDate(question.getDatePath(), "2023-01-02");
    }

    @Override
    public QuestionJsonPresenter getJsonPresenter() {
      return dateJsonPresenter;
    }
  }

  class EmailJsonSampler extends ContextualizedScalarsJsonSampler<EmailQuestion> {
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
    public void addSampleData(ApplicantData applicantData, EmailQuestion question) {
      applicantData.putString(question.getEmailPath(), "homer.simpson@springfield.gov");
    }

    @Override
    public QuestionJsonPresenter getJsonPresenter() {
      return emailJsonPresenter;
    }
  }

  class EmptyJsonSampler implements QuestionJsonSampler<Question> {

    @Override
    public CfJsonDocumentContext getSampleJson(QuestionDefinition questionDefinition) {
      return new CfJsonDocumentContext();
    }

    @Override
    public Question getQuestion(ApplicantQuestion applicantQuestion) {
      return applicantQuestion.createCurrencyQuestion();
    }

    @Override
    public void addSampleData(ApplicantData applicantData, Question question) {
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
    public void addSampleData(ApplicantData applicantData, FileUploadQuestion question) {
      applicantData.putString(question.getFileKeyPath(), "my-file-key");
    }

    @Override
    public QuestionJsonPresenter getJsonPresenter() {
      return fileUploadJsonPresenter;
    }
  }

  class IdJsonSampler extends ContextualizedScalarsJsonSampler<IdQuestion> {
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
    public void addSampleData(ApplicantData applicantData, IdQuestion question) {
      applicantData.putLong(question.getIdPath(), 12345L);
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
    public void addSampleData(ApplicantData applicantData, MultiSelectQuestion question) {
      LocalizedQuestionOption firstOption = question.getOptions().get(0);
      LocalizedQuestionOption secondOption = question.getOptions().get(1);

      applicantData.putArray(
          question.getSelectionPath(), ImmutableList.of(firstOption.id(), secondOption.id()));
    }

    @Override
    public QuestionJsonPresenter getJsonPresenter() {
      return multiSelectJsonPresenter;
    }
  }

  class NameJsonSampler extends ContextualizedScalarsJsonSampler<NameQuestion> {
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
    public void addSampleData(ApplicantData applicantData, NameQuestion question) {
      applicantData.putString(question.getFirstNamePath(), "Homer");
      applicantData.putString(question.getMiddleNamePath(), "Jay");
      applicantData.putString(question.getLastNamePath(), "Simpson");
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
    public void addSampleData(ApplicantData applicantData, NumberQuestion question) {
      applicantData.putLong(question.getNumberPath(), 12321);
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
    public void addSampleData(ApplicantData applicantData, PhoneQuestion question) {
      applicantData.putPhoneNumber(question.getPhoneNumberPath(), "(214)-367-3764");
      applicantData.putString(question.getCountryCodePath(), "US");
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
    public void addSampleData(ApplicantData applicantData, SingleSelectQuestion question) {
      if (question.getOptions().size() != 0) {
        LocalizedQuestionOption firstOption = question.getOptions().get(0);
        applicantData.putLong(question.getSelectionPath(), firstOption.id());
      }
    }

    @Override
    public QuestionJsonPresenter getJsonPresenter() {
      return singleSelectJsonPresenter;
    }
  }

  class TextJsonSampler extends ContextualizedScalarsJsonSampler<TextQuestion> {
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
    public void addSampleData(ApplicantData applicantData, TextQuestion question) {
      applicantData.putString(question.getTextPath(), "I love CiviForm!");
    }

    @Override
    public QuestionJsonPresenter getJsonPresenter() {
      return emailJsonPresenter;
    }
  }
}
