package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import models.ApplicantModel;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.RepeatedEntity;
import services.applicant.question.AbstractQuestion;
import services.applicant.question.AddressQuestion;
import services.applicant.question.ApplicantQuestion;
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
import services.applicant.question.SingleSelectQuestion;
import services.applicant.question.TextQuestion;
import services.geo.ServiceAreaInclusion;
import services.geo.ServiceAreaState;
import services.program.ProgramQuestionDefinition;
import services.question.LocalizedQuestionOption;
import services.question.QuestionAnswerer;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

/**
 * A {@link QuestionJsonSampler} for a Question {@link Q} provides a strategy for showing the
 * question's answers in JSON form.
 *
 * <p>Some {@link QuestionType}s share the same {@link QuestionJsonSampler}.
 */
public interface QuestionJsonSampler<Q extends AbstractQuestion> {

  public record SampleDataContext(
      ApplicantModel applicantModel,
      Map<Long, List<ImmutableList<RepeatedEntity>>> enumeratorRepeatedEntities) {

    public SampleDataContext() {
      this(new ApplicantModel(), new HashMap<>());
    }

    ApplicantData getApplicantData() {
      return applicantModel.getApplicantData();
    }
  }

  default ImmutableMap<Path, Optional<?>> getSampleJsonEntries(
      QuestionDefinition questionDefinition) {
    return getSampleJsonEntries(questionDefinition, new SampleDataContext());
  }

  default ImmutableMap<Path, Optional<?>> getSampleJsonEntries(
      QuestionDefinition questionDefinition, SampleDataContext sampleDataContext) {
    ProgramQuestionDefinition programQuestionDefinition =
        ProgramQuestionDefinition.create(questionDefinition, Optional.empty());
    return questionDefinition
        .getEnumeratorId()
        .map(
            enumeratorId ->
                getJsonEntriesForEnumerator(
                    enumeratorId, programQuestionDefinition, sampleDataContext))
        .orElseGet(
            () ->
                getEntitySampleJsonEntries(
                    Optional.empty(), programQuestionDefinition, sampleDataContext));
  }

  /**
   * Generates sample JSON entries for a question associated with an enumerator question. This
   * method processes the given question for each repeated entity associated with the specified
   * enumerator, effectively creating sample JSON data for each entity.
   *
   * @param enumeratorId The ID of the enumerator question.
   * @param programQuestionDefinition The definition of the program question for which to generate
   *     JSON.
   * @param sampleDataContext The sample data context containing information about repeated
   *     entities.
   * @return An ImmutableMap representing the generated JSON entries, where keys are Paths and
   *     values are Optional values. Returns an empty map if no repeated entities are found for the
   *     given enumerator ID.
   */
  private ImmutableMap<Path, Optional<?>> getJsonEntriesForEnumerator(
      long enumeratorId,
      ProgramQuestionDefinition programQuestionDefinition,
      SampleDataContext sampleDataContext) {
    return sampleDataContext
        .enumeratorRepeatedEntities
        .getOrDefault(enumeratorId, ImmutableList.of())
        .stream()
        .flatMap(repeatedEntityList -> repeatedEntityList.stream())
        .flatMap(
            repeatedEntity ->
                getEntitySampleJsonEntries(
                    Optional.of(repeatedEntity), programQuestionDefinition, sampleDataContext)
                    .entrySet()
                    .stream())
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private ImmutableMap<Path, Optional<?>> getEntitySampleJsonEntries(
      Optional<RepeatedEntity> repeatedEntity,
      ProgramQuestionDefinition programQuestionDefinition,
      SampleDataContext sampleDataContext) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            programQuestionDefinition,
            sampleDataContext.applicantModel,
            sampleDataContext.getApplicantData(),
            repeatedEntity);
    addSampleData(sampleDataContext, applicantQuestion);
    Q question = getQuestion(applicantQuestion);
    // Suppress warning about unchecked assignment because the JSON presenter is parameterized on
    // the question type, which we know matches Q.
    @SuppressWarnings("unchecked")
    ImmutableMap<Path, Optional<?>> entries = getJsonPresenter().getAllJsonEntries(question);
    return entries;
  }

  Q getQuestion(ApplicantQuestion applicantQuestion);

  void addSampleData(SampleDataContext sampleDataContext, ApplicantQuestion applicantQuestion);

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
    private final EnumeratorJsonSampler enumeratorJsonSampler;

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
        TextJsonSampler textJsonSampler,
        EnumeratorJsonSampler enumeratorJsonSampler) {
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
      this.enumeratorJsonSampler = checkNotNull(enumeratorJsonSampler);
    }

    public QuestionJsonSampler create(QuestionType questionType) {
      return switch (questionType) {
        case ADDRESS -> addressJsonSampler;
        case CHECKBOX -> multiSelectJsonSampler;
        case CURRENCY -> currencyJsonSampler;
        case DATE -> dateJsonSampler;
        case DROPDOWN -> singleSelectJsonSampler;
        case RADIO_BUTTON -> singleSelectJsonSampler;
        case EMAIL -> emailJsonSampler;
          // Answers to enumerator questions are not included. This is because enumerators store an
          // identifier value for each repeated entity, which with the current export logic
          // conflicts with the answers stored for repeated entities.
        case ENUMERATOR -> enumeratorJsonSampler;
        case FILEUPLOAD -> fileUploadJsonSampler;
        case ID -> idJsonSampler;
        case NAME -> nameJsonSampler;
        case NUMBER -> numberJsonSampler;
        case PHONE -> phoneJsonSampler;
          // Static content questions are not included in API responses because they
          // do not include an answer from the user.
        case STATIC -> emptyJsonSampler;
        case TEXT -> textJsonSampler;

        default ->
            throw new RuntimeException(String.format("Unrecognized questionType %s", questionType));
      };
    }
  }

  class EnumeratorJsonSampler implements QuestionJsonSampler<EnumeratorQuestion> {

    private final QuestionJsonPresenter enumeratorJsonPresenter;

    // Sample entity names for enumerator questions.
    private static final ImmutableList<String> SAMPLE_ENTITY_NAMES =
        ImmutableList.of("member1", "member2");

    @Inject
    EnumeratorJsonSampler(QuestionJsonPresenter.Factory questionJsonPresenterFactory) {
      this.enumeratorJsonPresenter = questionJsonPresenterFactory.create(QuestionType.ENUMERATOR);
    }

    @Override
    public EnumeratorQuestion getQuestion(ApplicantQuestion applicantQuestion) {
      return applicantQuestion.createEnumeratorQuestion();
    }

    @Override
    public void addSampleData(
        SampleDataContext sampleDataContext, ApplicantQuestion applicantQuestion) {
      // Answers enumerator question with sample entities
      QuestionAnswerer.answerEnumeratorQuestion(
          sampleDataContext.getApplicantData(),
          applicantQuestion.getContextualizedPath(),
          SAMPLE_ENTITY_NAMES);

      EnumeratorQuestionDefinition enumeratorQuestionDefinition =
          (EnumeratorQuestionDefinition) applicantQuestion.getQuestionDefinition();

      // Create repeated entities and store it in the sampleDataContext. These entities will be used
      // when processing a question that is associated with this enumerator question.
      ImmutableList<RepeatedEntity> repeatedEntities =
          RepeatedEntity.createRepeatedEntities(
              applicantQuestion.getRepeatedEntity(),
              enumeratorQuestionDefinition,
              Optional.empty(),
              sampleDataContext.getApplicantData());

      sampleDataContext
          .enumeratorRepeatedEntities
          .computeIfAbsent(enumeratorQuestionDefinition.getId(), k -> new ArrayList<>())
          .add(repeatedEntities);
    }

    @Override
    public QuestionJsonPresenter getJsonPresenter() {
      return enumeratorJsonPresenter;
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
        SampleDataContext sampleDataContext, ApplicantQuestion applicantQuestion) {
      QuestionAnswerer.answerAddressQuestion(
          sampleDataContext.getApplicantData(),
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
          /* serviceAreaInclusions= */ ImmutableList.of(
              ServiceAreaInclusion.create(
                  "springfieldCounty", ServiceAreaState.IN_AREA, 1709069741),
              ServiceAreaInclusion.create("portland", ServiceAreaState.NOT_IN_AREA, 1709069741)));
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
        SampleDataContext sampleDataContext, ApplicantQuestion applicantQuestion) {
      QuestionAnswerer.answerCurrencyQuestion(
          sampleDataContext.getApplicantData(),
          applicantQuestion.getContextualizedPath(),
          "123.45");
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
        SampleDataContext sampleDataContext, ApplicantQuestion applicantQuestion) {
      QuestionAnswerer.answerDateQuestion(
          sampleDataContext.getApplicantData(),
          applicantQuestion.getContextualizedPath(),
          "2023-01-02");
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
        SampleDataContext sampleDataContext, ApplicantQuestion applicantQuestion) {
      QuestionAnswerer.answerEmailQuestion(
          sampleDataContext.getApplicantData(),
          applicantQuestion.getContextualizedPath(),
          "homer.simpson@springfield.gov");
    }

    @Override
    public QuestionJsonPresenter getJsonPresenter() {
      return emailJsonPresenter;
    }
  }

  class EmptyJsonSampler implements QuestionJsonSampler<AbstractQuestion> {

    @Override
    public ImmutableMap<Path, Optional<?>> getSampleJsonEntries(
        QuestionDefinition questionDefinition, SampleDataContext sampleDataContext) {
      return ImmutableMap.of();
    }

    @Override
    public AbstractQuestion getQuestion(ApplicantQuestion applicantQuestion) {
      return null;
    }

    @Override
    public void addSampleData(
        SampleDataContext sampleDataContext, ApplicantQuestion applicantQuestion) {
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
        SampleDataContext sampleDataContext, ApplicantQuestion applicantQuestion) {
      QuestionAnswerer.answerFileQuestionWithMultipleUpload(
          sampleDataContext.getApplicantData(),
          applicantQuestion.getContextualizedPath(),
          ImmutableList.of("my-file-key-1", "my-file-key-2"));
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
        SampleDataContext sampleDataContext, ApplicantQuestion applicantQuestion) {
      QuestionAnswerer.answerIdQuestion(
          sampleDataContext.getApplicantData(), applicantQuestion.getContextualizedPath(), "12345");
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
        SampleDataContext sampleDataContext, ApplicantQuestion applicantQuestion) {
      ApplicantData applicantData = sampleDataContext.getApplicantData();
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
        SampleDataContext sampleDataContext, ApplicantQuestion applicantQuestion) {
      QuestionAnswerer.answerNameQuestion(
          sampleDataContext.getApplicantData(),
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
        SampleDataContext sampleDataContext, ApplicantQuestion applicantQuestion) {
      QuestionAnswerer.answerNumberQuestion(
          sampleDataContext.getApplicantData(), applicantQuestion.getContextualizedPath(), 12321);
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
        SampleDataContext sampleDataContext, ApplicantQuestion applicantQuestion) {
      QuestionAnswerer.answerPhoneQuestion(
          sampleDataContext.getApplicantData(),
          applicantQuestion.getContextualizedPath(),
          "US",
          "(214)-367-3764");
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
        SampleDataContext sampleDataContext, ApplicantQuestion applicantQuestion) {
      ImmutableList<LocalizedQuestionOption> questionOptions =
          applicantQuestion.createSingleSelectQuestion().getOptions();

      if (questionOptions.size() != 0) {
        LocalizedQuestionOption firstOption = questionOptions.get(0);
        QuestionAnswerer.answerSingleSelectQuestion(
            sampleDataContext.getApplicantData(),
            applicantQuestion.getContextualizedPath(),
            firstOption.id());
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
        SampleDataContext sampleDataContext, ApplicantQuestion applicantQuestion) {
      QuestionAnswerer.answerTextQuestion(
          sampleDataContext.getApplicantData(),
          applicantQuestion.getContextualizedPath(),
          "I love CiviForm!");
    }

    @Override
    public QuestionJsonPresenter getJsonPresenter() {
      return emailJsonPresenter;
    }
  }
}
