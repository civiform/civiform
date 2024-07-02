package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import repository.QuestionRepository;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.question.PrimaryApplicantInfoTag;
import services.question.QuestionOption;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.AddressQuestionDefinition;
import services.question.types.DateQuestionDefinition;
import services.question.types.EmailQuestionDefinition;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.PhoneQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.QuestionType;
import services.question.types.TextQuestionDefinition;
import services.question.types.TextQuestionDefinition.TextValidationPredicates;

public class QuestionModelTest extends ResetPostgres {

  private QuestionRepository repo;

  @Before
  public void setupQuestionRepository() {
    repo = instanceOf(QuestionRepository.class);
  }

  @Test
  public void canSaveQuestion() throws UnsupportedQuestionTypeException {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("test")
                .setDescription("")
                .setQuestionText(LocalizedStrings.of())
                .setQuestionHelpText(LocalizedStrings.empty())
                .build());
    QuestionModel question = new QuestionModel(definition);

    question.save();

    QuestionModel found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    QuestionDefinition expected =
        new QuestionDefinitionBuilder(definition).setId(question.id).build();
    assertThat(found.getQuestionDefinition()).isEqualTo(expected);
  }

  @Test
  public void canSerializeEnumeratorId_EmptyOptionalLong() {
    QuestionDefinition questionDefinition =
        new TextQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("test")
                .setDescription("")
                .setQuestionText(LocalizedStrings.of())
                .setQuestionHelpText(LocalizedStrings.empty())
                .build());
    QuestionModel question = new QuestionModel(questionDefinition);
    question.save();

    QuestionModel found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getEnumeratorId()).isEmpty();
  }

  @Test
  public void canSerializeEnumeratorId_NonEmptyOptionalLong() {
    QuestionDefinition questionDefinition =
        new TextQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("test")
                .setDescription("")
                .setQuestionText(LocalizedStrings.of())
                .setQuestionHelpText(LocalizedStrings.empty())
                .setEnumeratorId(Optional.of(10L))
                .build());
    QuestionModel question = new QuestionModel(questionDefinition);
    question.save();

    QuestionModel found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getEnumeratorId()).hasValue(10L);
  }

  @Test
  public void canSerializeLocalizationMaps() {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("")
                .setDescription("")
                .setQuestionText(LocalizedStrings.of(Locale.US, "hello"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help"))
                .build());
    QuestionModel question = new QuestionModel(definition);

    question.save();

    QuestionModel found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getQuestionText())
        .isEqualTo(LocalizedStrings.of(Locale.US, "hello"));
    assertThat(found.getQuestionDefinition().getQuestionHelpText())
        .isEqualTo(LocalizedStrings.of(Locale.US, "help"));
  }

  @Test
  public void canSerializeDifferentQuestionTypes() {
    AddressQuestionDefinition address =
        new AddressQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("address")
                .setDescription("")
                .setQuestionText(LocalizedStrings.of())
                .setQuestionHelpText(LocalizedStrings.empty())
                .build());
    QuestionModel question = new QuestionModel(address);

    question.save();

    QuestionModel found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition()).isInstanceOf(AddressQuestionDefinition.class);
  }

  @Test
  public void canSerializeValidationPredicates() {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("")
                .setDescription("")
                .setQuestionText(LocalizedStrings.of())
                .setQuestionHelpText(LocalizedStrings.empty())
                .setValidationPredicates(TextValidationPredicates.create(0, 128))
                .build());
    QuestionModel question = new QuestionModel(definition);

    question.save();

    QuestionModel found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    // assert type field is saved to json blob
    assertThat(found.getQuestionDefinition().getValidationPredicatesAsString())
        .contains("\"type\":\"text\"");
    assertThat(found.getQuestionDefinition().getValidationPredicates())
        .isEqualTo(TextValidationPredicates.create(0, 128));
  }

  @Test
  public void canSerializeAndDeserializeMultiOptionQuestion()
      throws UnsupportedQuestionTypeException {
    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.DROPDOWN)
            .setName("")
            .setDescription("")
            .setEnumeratorId(Optional.of(123L))
            .setQuestionText(LocalizedStrings.of())
            .setQuestionHelpText(LocalizedStrings.empty())
            .setQuestionOptions(
                ImmutableList.of(
                    QuestionOption.create(1L, "opt1", LocalizedStrings.of(Locale.US, "option"))))
            .build();
    QuestionModel question = new QuestionModel(definition);

    question.save();

    QuestionModel found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getQuestionType().isMultiOptionType()).isTrue();
    MultiOptionQuestionDefinition multiOption =
        (MultiOptionQuestionDefinition) found.getQuestionDefinition();

    assertThat(multiOption.getOptions())
        .isEqualTo(
            ImmutableList.of(
                QuestionOption.create(1L, "opt1", LocalizedStrings.of(Locale.US, "option"))));
    assertThat(multiOption.getEnumeratorId()).hasValue(123L);
  }

  @Test
  public void canSerializeAndDeserializeEnumeratorQuestion()
      throws UnsupportedQuestionTypeException {
    LocalizedStrings entityType = LocalizedStrings.of(Locale.US, "entity");

    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.ENUMERATOR)
            .setName("")
            .setDescription("")
            .setEnumeratorId(Optional.of(123L))
            .setQuestionText(LocalizedStrings.of())
            .setQuestionHelpText(LocalizedStrings.empty())
            .setEntityType(entityType)
            .build();
    QuestionModel question = new QuestionModel(definition);

    question.save();

    QuestionModel found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getQuestionType()).isEqualTo(QuestionType.ENUMERATOR);
    EnumeratorQuestionDefinition enumerator =
        (EnumeratorQuestionDefinition) found.getQuestionDefinition();

    assertThat(enumerator.getEntityType()).isEqualTo(entityType);
  }

  @Test
  public void testTimestamps() throws Exception {
    QuestionDefinition questionDefinition =
        new TextQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("test")
                .setDescription("")
                .setQuestionText(LocalizedStrings.of())
                .setQuestionHelpText(LocalizedStrings.empty())
                .setEnumeratorId(Optional.of(10L))
                .build());
    QuestionModel initialQuestion = new QuestionModel(questionDefinition);
    initialQuestion.save();

    assertThat(initialQuestion.getCreateTime()).isNotEmpty();
    assertThat(initialQuestion.getQuestionDefinition().getLastModifiedTime()).isNotEmpty();

    // Ensure a freshly loaded copy has the same timestamps.
    QuestionModel freshlyLoaded =
        repo.lookupQuestion(initialQuestion.id).toCompletableFuture().join().get();
    assertThat(freshlyLoaded.getCreateTime()).isEqualTo(initialQuestion.getCreateTime());
    assertThat(freshlyLoaded.getQuestionDefinition().getLastModifiedTime())
        .isEqualTo(initialQuestion.getQuestionDefinition().getLastModifiedTime());

    // Update the copy.
    // When persisting models with @WhenModified fields, EBean
    // truncates the persisted timestamp to milliseconds:
    // https://github.com/seattle-uat/civiform/pull/2499#issuecomment-1133325484.
    // Sleep for a few milliseconds to ensure that a subsequent
    // update would have a distinct timestamp.
    TimeUnit.MILLISECONDS.sleep(5);
    freshlyLoaded.markAsDirty();
    freshlyLoaded.save();

    QuestionModel afterUpdate =
        repo.lookupQuestion(initialQuestion.id).toCompletableFuture().join().get();
    assertThat(afterUpdate.getCreateTime()).isEqualTo(initialQuestion.getCreateTime());
    assertThat(afterUpdate.getQuestionDefinition().getLastModifiedTime()).isPresent();
    assertThat(afterUpdate.getQuestionDefinition().getLastModifiedTime().get())
        .isAfter(initialQuestion.getQuestionDefinition().getLastModifiedTime().get());
  }

  @Test
  public void savesUniversalCorrectly() {
    QuestionDefinitionConfig.Builder builder =
        QuestionDefinitionConfig.builder()
            .setName("test")
            .setDescription("")
            .setQuestionText(LocalizedStrings.of())
            .setQuestionHelpText(LocalizedStrings.empty());
    QuestionDefinition universalQuestionDefinition =
        new TextQuestionDefinition(builder.setUniversal(true).build());
    QuestionDefinition nonUniversalQuestionDefinition =
        new TextQuestionDefinition(builder.setUniversal(false).build());

    QuestionModel universalQuestion = new QuestionModel(universalQuestionDefinition);
    universalQuestion.save();
    QuestionModel nonUniversalQuestion = new QuestionModel(nonUniversalQuestionDefinition);
    nonUniversalQuestion.save();

    QuestionModel universalFound =
        repo.lookupQuestion(universalQuestion.id).toCompletableFuture().join().get();
    assertThat(universalFound.getQuestionDefinition().isUniversal()).isTrue();
    assertThat(universalFound.containsTag(QuestionTag.UNIVERSAL)).isTrue();

    QuestionModel nonUniversalFound =
        repo.lookupQuestion(nonUniversalQuestion.id).toCompletableFuture().join().get();
    assertThat(nonUniversalFound.getQuestionDefinition().isUniversal()).isFalse();
    assertThat(nonUniversalFound.containsTag(QuestionTag.UNIVERSAL)).isFalse();
  }

  @Test
  public void savesPrimaryApplicantInfoTagsCorrectly() {
    QuestionDefinitionConfig.Builder builder =
        QuestionDefinitionConfig.builder()
            .setName("test")
            .setDescription("")
            .setQuestionText(LocalizedStrings.of())
            .setQuestionHelpText(LocalizedStrings.empty());
    QuestionDefinition nameQuestionDefinition =
        new NameQuestionDefinition(
            builder
                .setPrimaryApplicantInfoTags(
                    ImmutableSet.of(PrimaryApplicantInfoTag.APPLICANT_NAME))
                .build());
    QuestionDefinition dateQuestionDefinition =
        new DateQuestionDefinition(
            builder
                .setPrimaryApplicantInfoTags(ImmutableSet.of(PrimaryApplicantInfoTag.APPLICANT_DOB))
                .build());
    QuestionDefinition emailQuestionDefinition =
        new EmailQuestionDefinition(
            builder
                .setPrimaryApplicantInfoTags(
                    ImmutableSet.of(PrimaryApplicantInfoTag.APPLICANT_EMAIL))
                .build());
    QuestionDefinition phoneQuestionDefinition =
        new PhoneQuestionDefinition(
            builder
                .setPrimaryApplicantInfoTags(
                    ImmutableSet.of(PrimaryApplicantInfoTag.APPLICANT_PHONE))
                .build());

    QuestionModel nameQuestion = new QuestionModel(nameQuestionDefinition);
    nameQuestion.save();
    QuestionModel dateQuestion = new QuestionModel(dateQuestionDefinition);
    dateQuestion.save();
    QuestionModel emailQuestion = new QuestionModel(emailQuestionDefinition);
    emailQuestion.save();
    QuestionModel phoneQuestion = new QuestionModel(phoneQuestionDefinition);
    phoneQuestion.save();

    QuestionModel nameFound =
        repo.lookupQuestion(nameQuestion.id).toCompletableFuture().join().get();
    assertThat(
            nameFound
                .getQuestionDefinition()
                .containsPrimaryApplicantInfoTag(PrimaryApplicantInfoTag.APPLICANT_NAME))
        .isTrue();
    assertThat(nameFound.containsTag(PrimaryApplicantInfoTag.APPLICANT_NAME.getQuestionTag()))
        .isTrue();

    QuestionModel dateFound =
        repo.lookupQuestion(dateQuestion.id).toCompletableFuture().join().get();
    assertThat(
            dateFound
                .getQuestionDefinition()
                .containsPrimaryApplicantInfoTag(PrimaryApplicantInfoTag.APPLICANT_DOB))
        .isTrue();
    assertThat(dateFound.containsTag(PrimaryApplicantInfoTag.APPLICANT_DOB.getQuestionTag()))
        .isTrue();

    QuestionModel emailFound =
        repo.lookupQuestion(emailQuestion.id).toCompletableFuture().join().get();
    assertThat(
            emailFound
                .getQuestionDefinition()
                .containsPrimaryApplicantInfoTag(PrimaryApplicantInfoTag.APPLICANT_EMAIL))
        .isTrue();
    assertThat(emailFound.containsTag(PrimaryApplicantInfoTag.APPLICANT_EMAIL.getQuestionTag()))
        .isTrue();

    QuestionModel phoneFound =
        repo.lookupQuestion(phoneQuestion.id).toCompletableFuture().join().get();
    assertThat(
            phoneFound
                .getQuestionDefinition()
                .containsPrimaryApplicantInfoTag(PrimaryApplicantInfoTag.APPLICANT_PHONE))
        .isTrue();
    assertThat(phoneFound.containsTag(PrimaryApplicantInfoTag.APPLICANT_PHONE.getQuestionTag()))
        .isTrue();
  }
}
