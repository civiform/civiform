package services.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableList;
import controllers.admin.ProgramMigrationWrapper;
import models.DisplayMode;
import org.junit.Test;
import repository.ResetPostgres;
import services.ErrorAnd;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.question.types.QuestionDefinition;
import support.ProgramBuilder;

public final class ProgramMigrationServiceTest extends ResetPostgres {
  private final ProgramMigrationService service =
      new ProgramMigrationService(instanceOf(ObjectMapper.class));

  @Test
  public void serialize_mapperThrowsException_returnsError() throws JsonProcessingException {
    ObjectMapper badObjectMapper = spy(new ObjectMapper());
    ObjectWriter badObjectWriter = spy(badObjectMapper.writerWithDefaultPrettyPrinter());
    when(badObjectMapper.writerWithDefaultPrettyPrinter()).thenReturn(badObjectWriter);
    when(badObjectWriter.writeValueAsString(any()))
        .thenThrow(new JsonProcessingException("Test exception!") {});

    ProgramMigrationService badMapperService = new ProgramMigrationService(badObjectMapper);

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
        testQuestionBank.applicantAddress().getQuestionDefinition();
    QuestionDefinition nameQuestionDefinition =
        testQuestionBank.applicantName().getQuestionDefinition();
    QuestionDefinition emailQuestionDefinition =
        testQuestionBank.applicantEmail().getQuestionDefinition();

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
    ErrorAnd<ProgramMigrationWrapper, String> result = service.deserialize(EXAMPLE_PROGRAM_JSON);

    assertThat(result.isError()).isFalse();
    ProgramMigrationWrapper wrapperResult = result.getResult();
    assertThat(wrapperResult.getProgram()).isNotNull();
    ProgramDefinition program = wrapperResult.getProgram();
    assertThat(program.adminName()).isEqualTo("import-program-sample");
    assertThat(program.adminDescription()).isEqualTo("desc");
    assertThat(program.externalLink()).isEqualTo("https://github.com/civiform/civiform");
    assertThat(program.displayMode()).isEqualTo(DisplayMode.PUBLIC);
    assertThat(program.programType()).isEqualTo(ProgramType.DEFAULT);
  }

  /**
   * This contains the bare minimum needed to parse JSON into a program definition. The
   * admin_program_migration.test.ts browser test has tests for a program with many blocks and
   * questions.
   */
  public static final String EXAMPLE_PROGRAM_JSON =
      "{\n"
          + "  \"program\": {\n"
          + "    \"id\": 34,\n"
          + "    \"adminName\": \"import-program-sample\",\n"
          + "    \"adminDescription\": \"desc\",\n"
          + "    \"externalLink\": \"https://github.com/civiform/civiform\",\n"
          + "    \"displayMode\": \"PUBLIC\",\n"
          + "    \"localizedName\": {\n"
          + "      \"translations\": {\n"
          + "        \"en_US\": \"Import Sample Program\"\n"
          + "      },\n"
          + "      \"isRequired\": true\n"
          + "    },\n"
          + "    \"localizedDescription\": {\n"
          + "      \"translations\": {\n"
          + "        \"en_US\": \"A sample program for testing program import\"\n"
          + "      },\n"
          + "      \"isRequired\": true\n"
          + "    },\n"
          + "    \"localizedConfirmationMessage\": {\n"
          + "      \"translations\": {\n"
          + "        \"en_US\": \"\"\n"
          + "      },\n"
          + "      \"isRequired\": true\n"
          + "    },\n"
          + "    \"programType\": \"DEFAULT\",\n"
          + "    \"eligibilityIsGating\": true,\n"
          + "    \"acls\": {\n"
          + "      \"tiProgramViewAcls\": []\n"
          + "    },\n"
          + "    \"localizedSummaryImageDescription\": {\n"
          + "      \"translations\": {\n"
          + "        \"en_US\": \"Test summary image description\"\n"
          + "      },\n"
          + "      \"isRequired\": true\n"
          + "    },\n"
          + "    \"blockDefinitions\": [\n"
          + "      {\n"
          + "        \"id\": 1,\n"
          + "        \"name\": \"Screen 1\",\n"
          + "        \"description\": \"block 1\",\n"
          + "        \"repeaterId\": null,\n"
          + "        \"hidePredicate\": null,\n"
          + "        \"optionalPredicate\": null,\n"
          + "        \"questionDefinitions\": [\n"
          + "          {\n"
          + "            \"id\": 18,\n"
          + "            \"optional\": false,\n"
          + "            \"addressCorrectionEnabled\": false\n"
          + "          },\n"
          + "          {\n"
          + "            \"id\": 5,\n"
          + "            \"optional\": false,\n"
          + "            \"addressCorrectionEnabled\": true\n"
          + "          }\n"
          + "        ]\n"
          + "      }\n"
          + "      ],\n"
          + "    \"statusDefinitions\" : {\n"
          + "      \"statuses\" : [ ]\n"
          + "    }"
          + "  }\n"
          + "}\n";
}
