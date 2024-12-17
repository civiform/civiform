package services.openapi.v3;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.Locale;
import java.util.Optional;
import services.applicant.question.Scalar;
import services.export.enums.ApiPathSegment;
import services.openapi.AbstractOpenApiSchemaGenerator;
import services.openapi.DefinitionType;
import services.openapi.Format;
import services.openapi.OpenApiSchemaGenerator;
import services.openapi.OpenApiSchemaSettings;
import services.openapi.QuestionDefinitionNode;
import services.program.ProgramDefinition;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

/** Configuration used to generate a program specific swagger 2 schema */
public class OpenApi3SchemaGenerator extends AbstractOpenApiSchemaGenerator
    implements OpenApiSchemaGenerator {

  public OpenApi3SchemaGenerator(OpenApiSchemaSettings openApiSchemaSettings) {
    super(openApiSchemaSettings);
  }

  @Override
  public String createSchema(ProgramDefinition programDefinition) {
    try {
      OpenAPI openAPI =
          new OpenAPI()
              .openapi("3.0.4") // 3.0.x has most compatibility with tools right now
              .info(
                  new Info()
                      .title(programDefinition.adminName())
                      .version(Long.toString(programDefinition.id()))
                      .description(programDefinition.adminDescription())
                      .contact(
                          new Contact()
                              .name("CiviForm Technical Support")
                              .email(openApiSchemaSettings.itEmailAddress())))
              .components(
                  new Components()
                      .addSchemas(
                          "result",
                          new ObjectSchema()
                              .addProperty(
                                  "payload",
                                  new ArraySchema()
                                      .items(
                                          new ObjectSchema()
                                              .addProperty("applicant_id", new IntegerSchema())
                                              .addProperty(
                                                  "application",
                                                  buildApplicationDefinitions(programDefinition))
                                              .addProperty("application_id", new IntegerSchema())
                                              .addProperty(
                                                  "create_time",
                                                  new StringSchema().format("date-time"))
                                              .addProperty(
                                                  "language", new StringSchema().example("en-US"))
                                              .addProperty(
                                                  "program_name",
                                                  new StringSchema().example("program-name-123"))
                                              .addProperty(
                                                  "program_version_id", new IntegerSchema())
                                              .addProperty(
                                                  "revision_state",
                                                  new StringSchema().example("CURRENT"))
                                              .addProperty(
                                                  "status", new StringSchema().nullable(true))
                                              .addProperty(
                                                  "submit_time",
                                                  new StringSchema().format("date-time"))
                                              .addProperty("submitter_type", new StringSchema())
                                              .addProperty(
                                                  "ti_email", new StringSchema().nullable(true))
                                              .addProperty(
                                                  "ti_organization",
                                                  new StringSchema().nullable(true))))
                              .addProperty("nextPageToken", new StringSchema()))
                      .addSecuritySchemes(
                          "basicAuth",
                          new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
              .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
              .paths(
                  new Paths()
                      .addPathItem(
                          "/applications",
                          new PathItem()
                              .get(
                                  new Operation()
                                      .operationId("list_applications")
                                      .description("List Applications")
                                      .summary("Export applications")
                                      .addTagsItem("programs")
                                      .responses(
                                          new ApiResponses()
                                              .addApiResponse(
                                                  "200",
                                                  new ApiResponse()
                                                      .description("For valid requests.")
                                                      .addHeaderObject(
                                                          "x-next",
                                                          new Header()
                                                              .description(
                                                                  "A link to the next page of"
                                                                      + " responses")
                                                              .schema(new StringSchema()))
                                                      .content(
                                                          new Content()
                                                              .addMediaType(
                                                                  "application/json",
                                                                  new MediaType()
                                                                      .schema(
                                                                          new Schema()
                                                                              .$ref(
                                                                                  "#/components/schemas/result")))))
                                              .addApiResponse(
                                                  "400",
                                                  new ApiResponse()
                                                      .description(
                                                          "Returned if any request parameters fail"
                                                              + " validation."))
                                              .addApiResponse(
                                                  "401",
                                                  new ApiResponse()
                                                      .description(
                                                          "Returned if the API key is invalid or"
                                                              + " does not have access to the"
                                                              + " program.")))
                                      .addParametersItem(
                                          new QueryParameter()
                                              .name("fromDate")
                                              .description(
                                                  "An ISO-8601 formatted date (i.e. YYYY-MM-DD)."
                                                      + " Limits results to applications submitted"
                                                      + " on or after the provided date, in the"
                                                      + " CiviForm instance's local time.")
                                              .schema(new StringSchema()))
                                      .addParametersItem(
                                          new QueryParameter()
                                              .name("toDate")
                                              .description(
                                                  "An ISO-8601 formatted date (i.e. YYYY-MM-DD)."
                                                      + " Limits results to applications submitted"
                                                      + " on or after the provided date, in the"
                                                      + " CiviForm instance's local time.")
                                              .schema(new StringSchema()))
                                      .addParametersItem(
                                          new QueryParameter()
                                              .name("pageSize")
                                              .description(
                                                  "A positive integer. Limits the number of results"
                                                      + " per page. If pageSize is larger than"
                                                      + " CiviForm's maximum page size then the"
                                                      + " maximum will be used. The default maximum"
                                                      + " is 1,000 and is configurable.")
                                              .schema(new IntegerSchema()))
                                      .addParametersItem(
                                          new QueryParameter()
                                              .name("nextPageToken")
                                              .description(
                                                  "An opaque, alphanumeric identifier for a"
                                                      + " specific page of results. When included"
                                                      + " CiviForm will return a page of results"
                                                      + " corresponding to the token.")
                                              .schema(new StringSchema())))));

      openAPI.setServers(getServers("/api/v1/admin/programs/" + programDefinition.slug()));

      return Yaml.pretty().writeValueAsString(openAPI);
    } catch (Exception ex) {
      throw new RuntimeException("Unable to generate OpenAPI 3.1 schema.", ex);
    }
  }

  private ImmutableList<Server> getServers(String path) {
    var result = ImmutableList.<Server>builder();

    // todo gwen: fix
    if (openApiSchemaSettings.allowHttpScheme()) {
      result.add(new Server().url(String.format("http://%s%s", getHostName(), path)));
    }

    result.add(new Server().url(String.format("https://%s%s", getHostName(), path)));

    return result.build();
  }

  /***
   * Entry point to start building the program specific definitions for questions
   */
  public ObjectSchema buildApplicationDefinitions(ProgramDefinition programDefinition)
      throws InvalidQuestionTypeException, UnsupportedQuestionTypeException {
    QuestionDefinitionNode rootNode = getQuestionDefinitionRootNode(programDefinition);
    var results = buildApplicationDefinitions(rootNode);
    var objectProperty = new ObjectSchema();

    results.entrySet().stream()
        .sorted(ImmutableMap.Entry.comparingByKey())
        .forEach(entry -> objectProperty.addProperty(entry.getKey(), entry.getValue()));

    return objectProperty;
  }

  /**
   * Recursive method used to build out the full object graph from the tree of QuestionDefinitions
   */
  protected ImmutableMap<String, Schema<?>> buildApplicationDefinitions(
      QuestionDefinitionNode parentQuestionDefinitionNode)
      throws InvalidQuestionTypeException, UnsupportedQuestionTypeException {
    ImmutableMap.Builder<String, Schema<?>> definitionList = ImmutableMap.builder();

    for (QuestionDefinitionNode childQuestionDefinitionNode :
        parentQuestionDefinitionNode.getChildren()) {
      QuestionDefinition questionDefinition = childQuestionDefinitionNode.getQuestionDefinition();

      if (excludeQuestionFromSchemaOutput(questionDefinition)) {
        continue;
      }

      var containerDefinition = new ObjectSchema();
      containerDefinition.addProperty(
          ApiPathSegment.QUESTION_TYPE.name().toLowerCase(Locale.ROOT), new StringSchema());

      // Enumerators require special handling
      if (questionDefinition.getQuestionType() != QuestionType.ENUMERATOR) {
        for (Scalar scalar : getScalarsSortedByName(questionDefinition)) {
          String fieldName = getFieldNameFromScalar(scalar);
          DefinitionType definitionType = getDefinitionTypeFromSwaggerType(scalar);
          DefinitionType arrayItemDefinitionType = getArrayItemDefinitionType(scalar);

          // Boolean nullable = isNullable(definitionType);

          containerDefinition.addProperty(
              fieldName,
              getPropertyFromType(
                  definitionType, getSwaggerFormat(scalar), arrayItemDefinitionType));
        }
      } else {
        var enumeratorProperties =
            ImmutableMap.<String, Schema>builder()
                .putAll(buildApplicationDefinitions(childQuestionDefinitionNode))
                .put(Scalar.ENTITY_NAME.name().toLowerCase(Locale.ROOT), new StringSchema())
                .build();

        ArraySchema enumeratorEntitiesDefinition =
            new ArraySchema().items(new ObjectSchema().properties(enumeratorProperties));

        containerDefinition.addProperty(
            ApiPathSegment.ENTITIES.name().toLowerCase(Locale.ROOT), enumeratorEntitiesDefinition);
      }

      definitionList.put(
          questionDefinition.getQuestionNameKey().toLowerCase(Locale.ROOT), containerDefinition);
    }

    return definitionList.build();
  }

  private static Schema<?> getPropertyFromType(
      DefinitionType definitionType,
      Optional<Format> swaggerFormat,
      DefinitionType arrayItemDefinitionType) {

    if (definitionType == DefinitionType.ARRAY) {
      return new ArraySchema()
          .items(
              arrayItemDefinitionType == DefinitionType.STRING
                  ? new StringSchema()
                  : new ObjectSchema());
    }

    var property = new Schema<>();

    property.setType(definitionType.toString());

    if (swaggerFormat.isPresent()) {
      property.setFormat(swaggerFormat.get().toString());
    }

    return property;
  }
}
