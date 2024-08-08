package services.migration;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import controllers.admin.ProgramMigrationWrapper;
import repository.QuestionRepository;
import services.ErrorAnd;
import services.program.ProgramDefinition;
import services.question.types.AddressQuestionDefinition;
import services.question.types.CurrencyQuestionDefinition;
import services.question.types.DateQuestionDefinition;
import services.question.types.EmailQuestionDefinition;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.FileUploadQuestionDefinition;
import services.question.types.IdQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.NameQuestionDefinition;
import services.question.types.NumberQuestionDefinition;
import services.question.types.PhoneQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.StaticContentQuestionDefinition;
import services.question.types.TextQuestionDefinition;

/**
 * A service responsible for helping admins migrate program definitions between different
 * environments.
 */
public final class ProgramMigrationService {
  private final ObjectMapper objectMapper;
  private final QuestionRepository questionRepository;

  @Inject
  public ProgramMigrationService(ObjectMapper objectMapper, QuestionRepository questionRepository) {
    // These extra modules let ObjectMapper serialize Guava types like ImmutableList.
    this.objectMapper =
        checkNotNull(objectMapper)
            .registerModule(new GuavaModule())
            .registerModule(new Jdk8Module())
            .configure(Feature.INCLUDE_SOURCE_IN_LOCATION, true);
    this.questionRepository = checkNotNull(questionRepository);
  }

  /**
   * Attempts to convert the provided {@code program} and {@code questions} into a serialized
   * instance of {@link ProgramMigrationWrapper}, returning either the successfully serialized
   * string or an error message.
   *
   * <p>If an error is returned, there will always be exactly one error message.
   */
  public ErrorAnd<String, String> serialize(
      ProgramDefinition program, ImmutableList<QuestionDefinition> questions) {
    try {
      String programJson =
          objectMapper
              .writerWithDefaultPrettyPrinter()
              .writeValueAsString(new ProgramMigrationWrapper(program, questions));
      return ErrorAnd.of(programJson);
    } catch (JsonProcessingException e) {
      return ErrorAnd.error(
          ImmutableSet.of(String.format("Program could not be serialized: %s", e)));
    }
  }

  /**
   * Attempts to deserialize the provided {@code programJson} into an instance of {@link
   * ProgramMigrationWrapper}, returning either the successfully deserialized wrapper or an error
   * message.
   *
   * <p>If an error is returned, there will always be exactly one error message.
   */
  public ErrorAnd<ProgramMigrationWrapper, String> deserialize(String programJson) {
    try {
      ProgramMigrationWrapper programMigrationWrapper =
          objectMapper.readValue(programJson, ProgramMigrationWrapper.class);
      return ErrorAnd.of(programMigrationWrapper);
    } catch (RuntimeException | JsonProcessingException e) {
      return ErrorAnd.error(
          ImmutableSet.of(String.format("JSON is incorrectly formatted: %s", e.getMessage())));
    }
  }

  /**
   * Checks if there are existing questions that match the admin id of any of the incoming
   * questions. If a match is found, it overwrites the admin id on the config of the incoming
   * question and creates a new question with the udpated config.
   */
  public ImmutableList<QuestionDefinition> maybeOverwriteQuestionName(
      ImmutableList<QuestionDefinition> questions) {
    return questions.stream()
        .map(
            (QuestionDefinition question) -> {
              boolean questionExists =
                  questionRepository.checkQuestionNameExists(question.getName());

              if (questionExists) {
                String newAdminName = question.getName() + "-1";
                QuestionDefinitionConfig newConfig =
                    question.overwriteQuestionName(newAdminName, question.getQuestionText());

                switch (question.getQuestionType()) {
                  case ADDRESS:
                    return new AddressQuestionDefinition(newConfig);
                  case CHECKBOX:
                    MultiOptionQuestionDefinition multiOptionCheckboxQuestion =
                        (MultiOptionQuestionDefinition) question;
                    return new MultiOptionQuestionDefinition(
                        newConfig,
                        multiOptionCheckboxQuestion.getOptions(),
                        MultiOptionQuestionType.CHECKBOX);
                  case DROPDOWN:
                    MultiOptionQuestionDefinition multiOptionDropdownQuestion =
                        (MultiOptionQuestionDefinition) question;
                    return new MultiOptionQuestionDefinition(
                        newConfig,
                        multiOptionDropdownQuestion.getOptions(),
                        MultiOptionQuestionType.DROPDOWN);
                  case RADIO_BUTTON:
                    MultiOptionQuestionDefinition multiOptionRadioQuestion =
                        (MultiOptionQuestionDefinition) question;
                    return new MultiOptionQuestionDefinition(
                        newConfig,
                        multiOptionRadioQuestion.getOptions(),
                        MultiOptionQuestionType.RADIO_BUTTON);
                  case CURRENCY:
                    return new CurrencyQuestionDefinition(newConfig);
                  case DATE:
                    return new DateQuestionDefinition(newConfig);
                  case EMAIL:
                    return new EmailQuestionDefinition(newConfig);
                  case ENUMERATOR:
                    EnumeratorQuestionDefinition enumeratorQuestion =
                        (EnumeratorQuestionDefinition) question;
                    return new EnumeratorQuestionDefinition(
                        newConfig, enumeratorQuestion.getEntityType());
                  case FILEUPLOAD:
                    return new FileUploadQuestionDefinition(newConfig);
                  case ID:
                    return new IdQuestionDefinition(newConfig);
                  case NAME:
                    return new NameQuestionDefinition(newConfig);
                  case NUMBER:
                    return new NumberQuestionDefinition(newConfig);
                  case PHONE:
                    return new PhoneQuestionDefinition(newConfig);
                  case STATIC:
                    return new StaticContentQuestionDefinition(newConfig);
                  case TEXT:
                    return new TextQuestionDefinition(newConfig);
                  case NULL_QUESTION: // fallthrough intended
                  default:
                    throw new RuntimeException(
                        String.format("Unknown QuestionType %s", question.getQuestionType()));
                }
              } else {
                return question;
              }
            })
        .collect(ImmutableList.toImmutableList());
  }
}
