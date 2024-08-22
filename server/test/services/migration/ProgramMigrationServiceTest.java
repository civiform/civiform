package services.migration;

import static controllers.admin.AdminImportControllerTest.PROGRAM_JSON_WITH_ONE_QUESTION;
import static controllers.admin.AdminImportControllerTest.PROGRAM_JSON_WITH_PREDICATES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.admin.ProgramMigrationWrapper;
import models.DisplayMode;
import org.junit.Before;
import org.junit.Test;
import repository.QuestionRepository;
import repository.ResetPostgres;
import services.ErrorAnd;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.question.types.QuestionDefinition;
import support.ProgramBuilder;

public final class ProgramMigrationServiceTest extends ResetPostgres {
  private final ProgramMigrationService service =
      new ProgramMigrationService(
          instanceOf(ObjectMapper.class), instanceOf(QuestionRepository.class));
  private QuestionRepository questionRepo;

  @Before
  public void setup() {
    questionRepo = instanceOf(QuestionRepository.class);
  }

  @Test
  public void serialize_mapperThrowsException_returnsError() throws JsonProcessingException {
    ObjectMapper badObjectMapper = spy(new ObjectMapper());
    ObjectWriter badObjectWriter = spy(badObjectMapper.writerWithDefaultPrettyPrinter());
    when(badObjectMapper.writerWithDefaultPrettyPrinter()).thenReturn(badObjectWriter);
    when(badObjectWriter.writeValueAsString(any()))
        .thenThrow(new JsonProcessingException("Test exception!") {});

    ProgramMigrationService badMapperService =
        new ProgramMigrationService(badObjectMapper, instanceOf(QuestionRepository.class));

    ErrorAnd<String, String> result =
        badMapperService.serialize(
            ProgramBuilder.newActiveProgram().build().getProgramDefinition(), ImmutableList.of());

    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors()).hasSize(1);
    String error = result.getErrors().stream().findFirst().get();
    assertThat(error).contains("Program could not be serialized");
    assertThat(error).contains("Test exception!");
  }

  @Test
  public void serialize_normalMapper_returnsStringWithProgramDefAndQuestionDefs() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("Active Program")
            .withProgramType(ProgramType.DEFAULT)
            .withBlock("Block A")
            .withBlock("Block B")
            .buildDefinition();

    QuestionDefinition addressQuestionDefinition =
        testQuestionBank.addressApplicantAddress().getQuestionDefinition();
    QuestionDefinition nameQuestionDefinition =
        testQuestionBank.nameApplicantName().getQuestionDefinition();
    QuestionDefinition emailQuestionDefinition =
        testQuestionBank.emailApplicantEmail().getQuestionDefinition();

    ImmutableList<QuestionDefinition> questions =
        ImmutableList.of(
            addressQuestionDefinition, nameQuestionDefinition, emailQuestionDefinition);

    ErrorAnd<String, String> result = service.serialize(programDefinition, questions);

    assertThat(result.isError()).isFalse();
    String resultString = result.getResult();
    assertThat(resultString).contains("\"adminName\" : \"Active Program\"");
    assertThat(resultString).contains("\"programType\" : \"DEFAULT\"");
    assertThat(resultString).contains("\"localizedName\"");
    assertThat(resultString).contains("\"en_US\" : \"Active Program\"");
    assertThat(resultString).contains("\"blockDefinitions\" : [");
    assertThat(resultString).contains("\"Block A\"");
    assertThat(resultString).contains("\"Block B\"");
    assertThat(resultString).contains("What is your address?");
    assertThat(resultString).contains("what is your name?");
    assertThat(resultString).contains("What is your Email?");
    // the enumeratorId field should only show up if there is an enumerator question in the programs
    assertFalse(resultString.contains("enumeratorId"));
  }

  @Test
  public void deserialize_malformattedJson_returnsError() {
    ErrorAnd<ProgramMigrationWrapper, String> result =
        service.deserialize("{\"adminName\" : \"admin-name\"");

    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors()).hasSize(1);
    String error = result.getErrors().stream().findFirst().get();
    assertThat(error).contains("JSON is incorrectly formatted");
  }

  @Test
  public void deserialize_notEnoughInfoToCreateProgramDef_returnsError() {
    ErrorAnd<ProgramMigrationWrapper, String> result =
        service.deserialize(
            "{ \"program\" : {\"id\" : 32, \"adminName\" : \"admin-name\","
                + " \"adminDescription\" : \"description\"}}");

    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors()).hasSize(1);
    String error = result.getErrors().stream().findFirst().get();
    assertThat(error).contains("JSON is incorrectly formatted");
  }

  @Test
  public void deserialize_jsonHasAllInfo_returnsResult() {
    ErrorAnd<ProgramMigrationWrapper, String> result =
        service.deserialize(PROGRAM_JSON_WITH_ONE_QUESTION);

    assertThat(result.isError()).isFalse();
    ProgramMigrationWrapper wrapperResult = result.getResult();
    assertThat(wrapperResult.getProgram()).isNotNull();

    ProgramDefinition program = wrapperResult.getProgram();
    QuestionDefinition question = wrapperResult.getQuestions().get(0);

    assertThat(program.adminName()).isEqualTo("minimal-sample-program");
    assertThat(program.adminDescription()).isEqualTo("desc");
    assertThat(program.externalLink()).isEqualTo("https://github.com/civiform/civiform");
    assertThat(program.displayMode()).isEqualTo(DisplayMode.PUBLIC);
    assertThat(program.programType()).isEqualTo(ProgramType.DEFAULT);
    assertThat(question.getName()).isEqualTo("Name");
    assertThat(question.getDescription()).isEqualTo("The applicant's name");
    assertThat(question.getQuestionText().getDefault())
        .isEqualTo("Please enter your first and last name");
  }

  @Test
  public void maybeOverwriteQuestionName_onlyOverwritesQuestionNamesIfAMatchIsFound() {
    ImmutableList<QuestionDefinition> questionsOne =
        service.deserialize(PROGRAM_JSON_WITH_PREDICATES).getResult().getQuestions();
    questionRepo.bulkCreateQuestions(questionsOne);

    // There are two questions in PROGRAM_JSON_WITH_PREDICATES: "id-test" and "text test"
    // We want to update the admin name of one of them so we can test that it is not changed by the
    // method
    String UPDATED_JSON = PROGRAM_JSON_WITH_PREDICATES.replace("text test", "new text test");

    ImmutableList<QuestionDefinition> questionsTwo =
        service.deserialize(UPDATED_JSON).getResult().getQuestions();
    ImmutableMap<String, QuestionDefinition> updatedQuestions =
        service.maybeOverwriteQuestionName(questionsTwo);

    // "id-test" should have been updated by the method
    assertThat(updatedQuestions.get("id-test").getName()).isEqualTo("id-test-1");
    // "new text test" should have not have been changed
    assertThat(updatedQuestions.get("new text test").getName()).isEqualTo("new text test");
  }

  @Test
  public void maybeGenerateNewAdminName_generatesCorrectAdminNames() {
    resourceCreator.insertQuestion("name-question");
    resourceCreator.insertQuestion("name-question-1");
    resourceCreator.insertQuestion("name-question-2");

    String newAdminNameOne = service.maybeGenerateNewAdminName("name-question");
    assertThat(newAdminNameOne).isEqualTo("name-question-3");

    String newAdminNameTwo = service.maybeGenerateNewAdminName("name-question-1");
    assertThat(newAdminNameTwo).isEqualTo("name-question-3");

    String unmatchedAdminName = service.maybeGenerateNewAdminName("new-admin-name");
    assertThat(unmatchedAdminName).isEqualTo("new-admin-name");
  }
}
