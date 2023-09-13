package services.export;

import static controllers.dev.seeding.SampleQuestionDefinitions.ADDRESS_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.CHECKBOX_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.CURRENCY_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.DATE_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.EMAIL_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.ENUMERATOR_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.FILE_UPLOAD_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.ID_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.NAME_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.NUMBER_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.PHONE_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.RADIO_BUTTON_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.STATIC_CONTENT_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.TEXT_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.dateEnumeratedQuestionDefinition;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import services.Path;
import services.question.types.QuestionType;

public class QuestionJsonSamplerTest extends ResetPostgres {

  private QuestionJsonSampler.Factory questionJsonSamplerFactory;

  @Before
  public void setUp() {
    questionJsonSamplerFactory = instanceOf(QuestionJsonSampler.Factory.class);
  }

  @Test
  public void samplesAddressQuestion() {
    @SuppressWarnings("unchecked")
    ImmutableMap<Path, Optional<?>> entries =
        questionJsonSamplerFactory
            .create(QuestionType.ADDRESS)
            .getSampleJsonEntries(ADDRESS_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(entries)
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.<Path, Optional<String>>builder()
                .put(Path.create("applicant.sample_address_question.zip"), Optional.of("97403"))
                .put(
                    Path.create("applicant.sample_address_question.city"),
                    Optional.of("Springfield"))
                .put(
                    Path.create("applicant.sample_address_question.street"),
                    Optional.of("742 Evergreen Terrace"))
                .put(
                    Path.create("applicant.sample_address_question.latitude"),
                    Optional.of("44.0462"))
                .put(
                    Path.create("applicant.sample_address_question.well_known_id"),
                    Optional.of("23214"))
                .put(
                    Path.create("applicant.sample_address_question.service_area"),
                    Optional.of("springfield_county"))
                .put(Path.create("applicant.sample_address_question.state"), Optional.of("OR"))
                .put(Path.create("applicant.sample_address_question.line2"), Optional.empty())
                .put(Path.create("applicant.sample_address_question.corrected"), Optional.empty())
                .put(
                    Path.create("applicant.sample_address_question.longitude"),
                    Optional.of("-123.0236"))
                .put(
                    Path.create("applicant.sample_address_question.question_type"),
                    Optional.of("ADDRESS"))
                .build());
  }

  @Test
  public void samplesCurrencyQuestion() {
    @SuppressWarnings("unchecked")
    ImmutableMap<Path, Optional<?>> entries =
        questionJsonSamplerFactory
            .create(QuestionType.CURRENCY)
            .getSampleJsonEntries(CURRENCY_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(entries)
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(
                Path.create("applicant.sample_currency_question.question_type"),
                Optional.of("CURRENCY"),
                Path.create("applicant.sample_currency_question.currency_dollars"),
                Optional.of(123.45)));
  }

  @Test
  public void samplesDateQuestion() {
    @SuppressWarnings("unchecked")
    ImmutableMap<Path, Optional<?>> entries =
        questionJsonSamplerFactory
            .create(QuestionType.DATE)
            .getSampleJsonEntries(DATE_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(entries)
        .containsExactlyEntriesOf(
            ImmutableMap.of(
                Path.create("applicant.sample_date_question.question_type"),
                Optional.of("DATE"),
                Path.create("applicant.sample_date_question.date"),
                Optional.of("2023-01-02")));
  }

  @Test
  public void samplesEmailQuestion() {
    @SuppressWarnings("unchecked")
    ImmutableMap<Path, Optional<?>> entries =
        questionJsonSamplerFactory
            .create(QuestionType.EMAIL)
            .getSampleJsonEntries(EMAIL_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(entries)
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(
                Path.create("applicant.sample_email_question.question_type"),
                Optional.of("EMAIL"),
                Path.create("applicant.sample_email_question.email"),
                Optional.of("homer.simpson@springfield.gov")));
  }

  @Test
  public void samplesFileUploadQuestion() {
    @SuppressWarnings("unchecked")
    ImmutableMap<Path, Optional<?>> entries =
        questionJsonSamplerFactory
            .create(QuestionType.FILEUPLOAD)
            .getSampleJsonEntries(FILE_UPLOAD_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(entries)
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(
                Path.create("applicant.sample_file_upload_question.question_type"),
                Optional.of("FILE_UPLOAD"),
                Path.create("applicant.sample_file_upload_question.file_key"),
                Optional.of("http://localhost:9000/admin/applicant-files/my-file-key")));
  }

  @Test
  public void samplesIdQuestion() {
    @SuppressWarnings("unchecked")
    ImmutableMap<Path, Optional<?>> entries =
        questionJsonSamplerFactory
            .create(QuestionType.ID)
            .getSampleJsonEntries(ID_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(entries)
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(
                Path.create("applicant.sample_id_question.question_type"),
                Optional.of("ID"),
                Path.create("applicant.sample_id_question.id"),
                Optional.of("12345")));
  }

  @Test
  public void samplesMultiSelectQuestion() {
    @SuppressWarnings("unchecked")
    ImmutableMap<Path, Optional<?>> entries =
        questionJsonSamplerFactory
            .create(QuestionType.CHECKBOX)
            .getSampleJsonEntries(CHECKBOX_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(entries)
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(
                Path.create("applicant.sample_checkbox_question.question_type"),
                Optional.of("MULTI_SELECT"),
                Path.create("applicant.sample_checkbox_question.selections"),
                Optional.of(Arrays.asList("toaster", "pepper_grinder"))));
  }

  @Test
  public void samplesNameQuestion() {
    @SuppressWarnings("unchecked")
    ImmutableMap<Path, Optional<?>> entries =
        questionJsonSamplerFactory
            .create(QuestionType.NAME)
            .getSampleJsonEntries(NAME_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(entries)
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(
                Path.create("applicant.name.question_type"),
                Optional.of("NAME"),
                Path.create("applicant.name.first_name"),
                Optional.of("Homer"),
                Path.create("applicant.name.middle_name"),
                Optional.of("Jay"),
                Path.create("applicant.name.last_name"),
                Optional.of("Simpson")));
  }

  @Test
  public void samplesNumberQuestion() {
    @SuppressWarnings("unchecked")
    ImmutableMap<Path, Optional<?>> entries =
        questionJsonSamplerFactory
            .create(QuestionType.NUMBER)
            .getSampleJsonEntries(NUMBER_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(entries)
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(
                Path.create("applicant.sample_number_question.question_type"),
                Optional.of("NUMBER"),
                Path.create("applicant.sample_number_question.number"),
                Optional.of(12321L)));
  }

  @Test
  public void samplesPhoneQuestion() {
    @SuppressWarnings("unchecked")
    ImmutableMap<Path, Optional<?>> entries =
        questionJsonSamplerFactory
            .create(QuestionType.PHONE)
            .getSampleJsonEntries(PHONE_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(entries)
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(
                Path.create("applicant.sample_phone_question.question_type"),
                Optional.of("PHONE"),
                Path.create("applicant.sample_phone_question.phone_number"),
                Optional.of("+12143673764")));
  }

  @Test
  public void samplesSingleSelectQuestion() {
    @SuppressWarnings("unchecked")
    ImmutableMap<Path, Optional<?>> entries =
        questionJsonSamplerFactory
            .create(QuestionType.RADIO_BUTTON)
            .getSampleJsonEntries(RADIO_BUTTON_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(entries)
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(
                Path.create("applicant.sample_radio_button_question.question_type"),
                Optional.of("SINGLE_SELECT"),
                Path.create("applicant.sample_radio_button_question.selection"),
                Optional.of("winter")));
  }

  @Test
  public void samplesTextQuestion() {
    @SuppressWarnings("unchecked")
    ImmutableMap<Path, Optional<?>> entries =
        questionJsonSamplerFactory
            .create(QuestionType.TEXT)
            .getSampleJsonEntries(TEXT_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(entries)
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(
                Path.create("applicant.sample_text_question.question_type"),
                Optional.of("TEXT"),
                Path.create("applicant.sample_text_question.text"),
                Optional.of("I love CiviForm!")));
  }

  @Test
  public void staticQuestions_returnEmpty() {
    @SuppressWarnings("unchecked")
    ImmutableMap<Path, Optional<?>> entries =
        questionJsonSamplerFactory
            .create(QuestionType.STATIC)
            .getSampleJsonEntries(STATIC_CONTENT_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(entries).isEmpty();
  }

  // TODO(#4975): update this test once enumerator questions are supported.
  @Test
  public void enumeratorQuestions_returnEmpty() {
    @SuppressWarnings("unchecked")
    ImmutableMap<Path, Optional<?>> entries =
        questionJsonSamplerFactory
            .create(QuestionType.ENUMERATOR)
            .getSampleJsonEntries(ENUMERATOR_QUESTION_DEFINITION.withPopulatedTestId());

    assertThat(entries).isEmpty();
  }

  // TODO(#5238): update this test once enumerated questions are supported.
  @Test
  public void enumeratedQuestions_returnEmpty() {
    @SuppressWarnings("unchecked")
    ImmutableMap<Path, Optional<?>> entries =
        questionJsonSamplerFactory
            .create(QuestionType.DATE)
            .getSampleJsonEntries(dateEnumeratedQuestionDefinition(1L).withPopulatedTestId());

    assertThat(entries).isEmpty();
  }
}
