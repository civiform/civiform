package services.openapi.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import io.swagger.models.AuthorizationType;
import io.swagger.models.Contact;
import io.swagger.models.Info;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.SecurityRequirement;
import io.swagger.models.Swagger;
import io.swagger.models.auth.BasicAuthDefinition;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.DateTimeProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.StringProperty;
import io.swagger.util.Yaml;
import java.util.Locale;
import services.applicant.question.Scalar;
import services.export.enums.ApiPathSegment;
import services.openapi.AbstractOpenApiSchemaGenerator;
import services.openapi.OpenApiSchemaGenerator;
import services.openapi.OpenApiSchemaSettings;
import services.openapi.QuestionDefinitionNode;
import services.program.ProgramDefinition;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

/** Configuration used to generate a program specific swagger 2 schema */
public class Swagger2SchemaGenerator extends AbstractOpenApiSchemaGenerator
    implements OpenApiSchemaGenerator {

  public Swagger2SchemaGenerator(OpenApiSchemaSettings openApiSchemaSettings) {
    super(openApiSchemaSettings);
  }

  @Override
  public String createSchema(ProgramDefinition programDefinition) {
    try {
      Swagger swaggerRoot =
          new Swagger()
              .basePath("/api/v1/admin/programs/" + programDefinition.slug())
              .host(getHostName())
              .info(
                  new Info()
                      .title(programDefinition.adminName())
                      .version(Long.toString(programDefinition.id()))
                      .description(programDefinition.adminDescription())
                      .contact(
                          new Contact()
                              .name("CiviForm Technical Support")
                              .email(openApiSchemaSettings.itEmailAddress())))
              .schemes(getSchemes())
              .security(
                  new SecurityRequirement().requirement(AuthorizationType.BASIC_AUTH.toValue()))
              .securityDefinition(AuthorizationType.BASIC_AUTH.toValue(), new BasicAuthDefinition())
              .path(
                  "/applications",
                  new Path()
                      .get(
                          new Operation()
                              .operationId("list_applications")
                              .description("List Applications")
                              .summary("Export applications")
                              .produces("application/json")
                              .tag("programs")
                              .response(
                                  200,
                                  new Response()
                                      .description("For valid requests.")
                                      .responseSchema(new RefModel("#/definitions/result"))
                                      .header(
                                          "x-next",
                                          new StringProperty()
                                              .description("A link to the next page of responses")))
                              .response(
                                  400,
                                  new Response()
                                      .description(
                                          "Returned if any request parameters fail validation."))
                              .response(
                                  401,
                                  new Response()
                                      .description(
                                          "Returned if the API key is invalid or does not have"
                                              + " access to the program."))
                              .parameter(
                                  new QueryParameter()
                                      .name("fromDate")
                                      .type(DefinitionType.STRING.toString())
                                      .description(
                                          "An ISO-8601 formatted date (i.e. YYYY-MM-DD). Limits"
                                              + " results to applications submitted on or after the"
                                              + " provided date, in the CiviForm instance's local"
                                              + " time."))
                              .parameter(
                                  new QueryParameter()
                                      .name("toDate")
                                      .type(DefinitionType.STRING.toString())
                                      .description(
                                          "An ISO-8601 formatted date (i.e. YYYY-MM-DD). Limits"
                                              + " results to applications submitted on or after the"
                                              + " provided date, in the CiviForm instance's local"
                                              + " time."))
                              .parameter(
                                  new QueryParameter()
                                      .name("pageSize")
                                      .type(DefinitionType.INTEGER.toString())
                                      .description(
                                          "A positive integer. Limits the number of results per"
                                              + " page. If pageSize is larger than CiviForm's"
                                              + " maximum page size then the maximum will be used."
                                              + " The default maximum is 1,000 and is"
                                              + " configurable."))
                              .parameter(
                                  new QueryParameter()
                                      .name("nextPageToken")
                                      .type(DefinitionType.STRING.toString())
                                      .description(
                                          "An opaque, alphanumeric identifier for a specific page"
                                              + " of results. When included CiviForm will return a"
                                              + " page of results corresponding to the token."))));

      swaggerRoot.addDefinition(
          "result",
          new ModelImpl()
              .type("object")
              .property(
                  "payload",
                  new ArrayProperty(
                      new ObjectProperty()
                          .property("applicant_id", new IntegerProperty())
                          .property("application", buildApplicationDefinitions(programDefinition))
                          .property("application_id", new IntegerProperty())
                          .property("create_time", new DateTimeProperty())
                          .property("language", new StringProperty())
                          .property("program_name", new StringProperty())
                          .property("program_version_id", new IntegerProperty())
                          .property("revision_state", new StringProperty())
                          .property(
                              "status", new StringProperty().vendorExtension("x-nullable", true))
                          .property("submit_time", new DateTimeProperty())
                          .property("submitter_type", new StringProperty())
                          .property(
                              "ti_email", new StringProperty().vendorExtension("x-nullable", true))
                          .property(
                              "ti_organization",
                              new StringProperty().vendorExtension("x-nullable", true))))
              .property("nextPageToken", new StringProperty()));

      return Yaml.pretty().writeValueAsString(swaggerRoot);
    } catch (RuntimeException
        | InvalidQuestionTypeException
        | UnsupportedQuestionTypeException
        | JsonProcessingException ex) {
      throw new RuntimeException("Unable to generate OpenAPI schema for Swagger 2.", ex);
    }
  }

  /***
   * Entry point to start building the program specific definitions for questions
   */
  private Property buildApplicationDefinitions(ProgramDefinition programDefinition)
      throws InvalidQuestionTypeException, UnsupportedQuestionTypeException {
    QuestionDefinitionNode rootNode = getQuestionDefinitionRootNode(programDefinition);
    var results = buildApplicationDefinitions(rootNode);
    var objectProperty = new ObjectProperty();

    results.entrySet().stream()
        .forEach(entry -> objectProperty.property(entry.getKey(), entry.getValue()));

    return objectProperty;
  }

  /**
   * Recursive method used to build out the full object graph from the tree of QuestionDefinitions
   */
  protected ImmutableMap<String, Property> buildApplicationDefinitions(
      QuestionDefinitionNode parentQuestionDefinitionNode)
      throws InvalidQuestionTypeException, UnsupportedQuestionTypeException {
    ImmutableMap.Builder<String, Property> definitionList = ImmutableMap.builder();

    for (QuestionDefinitionNode childQuestionDefinitionNode :
        parentQuestionDefinitionNode.getChildren()) {
      QuestionDefinition questionDefinition = childQuestionDefinitionNode.getQuestionDefinition();

      if (excludeQuestionFromSchemaOutput(questionDefinition)) {
        continue;
      }

      var containerDefinition = new ObjectProperty();
      containerDefinition.property(
          ApiPathSegment.QUESTION_TYPE.name().toLowerCase(Locale.ROOT), new StringProperty());

      // Enumerators require special handling
      if (questionDefinition.getQuestionType() != QuestionType.ENUMERATOR) {
        for (Scalar scalar : getScalarsSortedByName(questionDefinition)) {
          String fieldName = getFieldNameFromScalar(scalar);
          DefinitionType definitionType = getDefinitionTypeFromSwaggerType(scalar);
          DefinitionType arrayItemDefinitionType = getArrayItemDefinitionType(scalar);

          containerDefinition.property(
              fieldName,
              getPropertyFromType(
                  definitionType, getSwaggerFormat(scalar), arrayItemDefinitionType));
        }
      } else {
        ArrayProperty enumeratorEntitiesDefinition =
            new ArrayProperty(
                new ObjectProperty()
                    .properties(
                        ImmutableMap.<String, Property>builder()
                            .putAll(buildApplicationDefinitions(childQuestionDefinitionNode))
                            .put(
                                Scalar.ENTITY_NAME.name().toLowerCase(Locale.ROOT),
                                new StringProperty())
                            .build()));

        containerDefinition.property(
            ApiPathSegment.ENTITIES.name().toLowerCase(Locale.ROOT), enumeratorEntitiesDefinition);
      }

      definitionList.put(
          questionDefinition.getQuestionNameKey().toLowerCase(Locale.ROOT), containerDefinition);
    }

    return definitionList.build();
  }
}
