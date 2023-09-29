package services.openApi.v2;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import services.applicant.question.Scalar;
import services.openApi.OpenApiGenerationException;
import services.openApi.OpenApiSchemaGenerator;
import services.openApi.OpenApiSchemaSettings;
import services.openApi.OpenApiVersion;
import services.openApi.QuestionDefinitionNode;
import services.openApi.v2.serializers.Swagger2YamlMapper;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import services.question.types.ScalarType;

/** Configuration used to generate a program specific swagger 2 schema */
public class Swagger2SchemaGenerator implements OpenApiSchemaGenerator {
  private final OpenApiSchemaSettings openApiSchemaSettings;

  public Swagger2SchemaGenerator(OpenApiSchemaSettings openApiSchemaSettings) {
    this.openApiSchemaSettings = checkNotNull(openApiSchemaSettings);
  }

  @Override
  public String createSchema(ProgramDefinition programDefinition) {
    try {
      Swagger swaggerRoot =
          Swagger.builder()
              .setBasePath("/api/v1/admin/programs/" + programDefinition.slug())
              .setHost(getHostName())
              .setInfo(
                  Info.builder(programDefinition.adminName(), Long.toString(programDefinition.id()))
                      .setDescription(programDefinition.adminDescription())
                      .setContact(
                          Contact.builder()
                              .setName("CiviForm Technical Support")
                              .setEmail(openApiSchemaSettings.getItEmailAddress())
                              .build())
                      .build())
              .setSchemes(getSchemes())
              .addSecurityRequirement(SecurityRequirement.builder(SecurityType.BASIC).build())
              .addSecurityDefinition(SecurityDefinition.basicBuilder().build())
              .setPaths(
                  Paths.builder()
                      .addPathItem(
                          PathItem.builder("/applications")
                              .addOperation(
                                  Operation.builder(OperationType.GET)
                                      .setOperationId("get_applications")
                                      .setDescription("Get Applications")
                                      .setSummary("Export applications")
                                      .addProduces(MimeType.Json)
                                      .addTag("programs")
                                      .addResponse(
                                          Response.builder(HttpStatusCode.OK, "For valid requests.")
                                              .setSchema("#/definitions/result")
                                              .addHeader(
                                                  Header.builder(DefinitionType.STRING, "x-next")
                                                      .setDescription(
                                                          "A link to the next page of responses")
                                                      .build())
                                              .build())
                                      .addResponse(
                                          Response.builder(
                                                  HttpStatusCode.BadRequest,
                                                  "Returned if any request parameters fail"
                                                      + " validation.")
                                              .build())
                                      .addResponse(
                                          Response.builder(
                                                  HttpStatusCode.Unauthorized,
                                                  "Returned if the API key is invalid or does not"
                                                      + " have access to the program.")
                                              .build())
                                      .addParameter(
                                          Parameter.builder(
                                                  "fromDate", In.QUERY, DefinitionType.STRING)
                                              .setDescription(
                                                  "An ISO-8601 formatted date (i.e. YYYY-MM-DD)."
                                                      + " Limits results to applications submitted"
                                                      + " on or after the provided date.")
                                              .build())
                                      .addParameter(
                                          Parameter.builder(
                                                  "toDate", In.QUERY, DefinitionType.STRING)
                                              .setDescription(
                                                  "An ISO-8601 formatted date (i.e. YYYY-MM-DD)."
                                                      + " Limits results to applications submitted"
                                                      + " before the provided date.")
                                              .build())
                                      .addParameter(
                                          Parameter.builder(
                                                  "pageSize", In.QUERY, DefinitionType.INTEGER)
                                              .setDescription(
                                                  "A positive integer. Limits the number of"
                                                      + " results per page. If pageSize is larger"
                                                      + " than CiviForm's maximum page size then"
                                                      + " the maximum will be used. The default"
                                                      + " maximum is 1,000 and is configurable.")
                                              .build())
                                      .addParameter(
                                          Parameter.builder(
                                                  "nextPageToken", In.QUERY, DefinitionType.STRING)
                                              .setDescription(
                                                  "An opaque, alphanumeric identifier for a"
                                                      + " specific page of results. When included"
                                                      + " CiviForm will return a page of results"
                                                      + " corresponding to the token.")
                                              .build())
                                      .build())
                              .build())
                      .build())
              .addDefinition(
                  Definition.builder(
                          "result",
                          DefinitionType.OBJECT,
                          ImmutableList.of(
                              Definition.builder(
                                      "payload",
                                      DefinitionType.ARRAY,
                                      ImmutableList.of(
                                          Definition.builder("applicant_id", DefinitionType.INTEGER)
                                              .setFormat(Format.INT32)
                                              .build(),
                                          Definition.builder(
                                                  "application",
                                                  DefinitionType.OBJECT,
                                                  buildApplicationDefinitions(programDefinition))
                                              .build(),
                                          Definition.builder(
                                                  "application_id", DefinitionType.INTEGER)
                                              .setFormat(Format.INT32)
                                              .build(),
                                          Definition.builder("create_time", DefinitionType.STRING)
                                              .build(),
                                          Definition.builder("language", DefinitionType.STRING)
                                              .build(),
                                          Definition.builder("program_name", DefinitionType.STRING)
                                              .build(),
                                          Definition.builder(
                                                  "program_version_id", DefinitionType.INTEGER)
                                              .setFormat(Format.INT32)
                                              .build(),
                                          Definition.builder("status", DefinitionType.STRING)
                                              .build(),
                                          Definition.builder("submit_time", DefinitionType.STRING)
                                              .build(),
                                          Definition.builder(
                                                  "submitter_email", DefinitionType.STRING)
                                              .build()))
                                  .build(),
                              Definition.builder("nextPageToken", DefinitionType.STRING).build()))
                      .build())
              .build();

      ObjectMapper mapper = Swagger2YamlMapper.getMapper();
      return mapper.writeValueAsString(swaggerRoot);
    } catch (RuntimeException
        | InvalidQuestionTypeException
        | UnsupportedQuestionTypeException
        | JsonProcessingException ex) {
      throw new OpenApiGenerationException(
          OpenApiVersion.SWAGGER_V2, "Unable to generate OpenAPI schema.", ex);
    }
  }

  /**
   * Returns the list of schemes to allow in the swagger schema. Special case to allow http for
   * non-prod environments for testing purposes.
   */
  private ImmutableList<Scheme> getSchemes() {
    if (openApiSchemaSettings.getAllowHttpScheme()) {
      return ImmutableList.of(Scheme.HTTP, Scheme.HTTPS);
    }

    return ImmutableList.of(Scheme.HTTPS);
  }

  /***
   * Entry point to start building the program specific definitions for questions
   */
  private ImmutableList<Definition> buildApplicationDefinitions(ProgramDefinition programDefinition)
      throws InvalidQuestionTypeException, UnsupportedQuestionTypeException {

    QuestionDefinitionNode rootNode = getQuestionDefinitionRootNode(programDefinition);

    return buildApplicationDefinitions(rootNode).stream()
        .sorted(Comparator.comparing(Definition::getName))
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Recursive method used to build out the full object graph from the tree of QuestionDefinitions
   */
  private ImmutableList<Definition> buildApplicationDefinitions(
      QuestionDefinitionNode parentQuestionDefinitionNode)
      throws InvalidQuestionTypeException, UnsupportedQuestionTypeException {
    ArrayList<Definition> definitionList = new ArrayList<>();

    for (QuestionDefinitionNode childQuestionDefinitionNode :
        parentQuestionDefinitionNode.getChildren()) {
      QuestionDefinition questionDefinition = childQuestionDefinitionNode.getQuestionDefinition();

      if (excludeFromSchemaOutput(questionDefinition)) {
        continue;
      }

      Definition.Builder containerDefinition =
          Definition.builder(
              questionDefinition.getQuestionNameKey().toLowerCase(Locale.ROOT),
              DefinitionType.OBJECT);

      containerDefinition.addDefinition(
          Definition.builder("question_type", DefinitionType.STRING).build());

      // Enumerators require special handling
      if (questionDefinition.getQuestionType() != QuestionType.ENUMERATOR) {
        for (Scalar scalar : getScalarsSortedByName(questionDefinition)) {
          String fieldName = scalar.name().toLowerCase(Locale.ROOT);
          DefinitionType definitionType = getDefinitionTypeFromSwaggerType(scalar.toScalarType());
          Format swaggerFormat = getSwaggerFormat(scalar.toScalarType());
          Boolean nullable = isNullable(definitionType);

          containerDefinition.addDefinition(
              Definition.builder(fieldName, definitionType)
                  .setFormat(swaggerFormat)
                  .setNullable(nullable)
                  .build());
        }
      } else {
        Definition enumeratorEntitiesDefinition =
            Definition.builder("entities", DefinitionType.ARRAY)
                .addDefinitions(buildApplicationDefinitions(childQuestionDefinitionNode))
                .build();

        containerDefinition.addDefinition(enumeratorEntitiesDefinition);
      }

      definitionList.add(containerDefinition.build());
    }

    return definitionList.stream().collect(ImmutableList.toImmutableList());
  }

  /** Build an n-ary tree from the flat of QuestionDefinition list */
  private QuestionDefinitionNode getQuestionDefinitionRootNode(
      ProgramDefinition programDefinition) {
    ArrayList<QuestionDefinition> list = new ArrayList<>();

    for (BlockDefinition blockDefinition : programDefinition.blockDefinitions()) {
      for (int i = 0; i < blockDefinition.getQuestionCount(); i++) {
        list.add(blockDefinition.getQuestionDefinition(i));
      }
    }

    // Getting a sorted list to allow placing the enumerator questions
    // into the tree before the questions that are children to the enumerator.
    // An enumerator already has to be created before a question can be added to it
    // and questions can only be assigned a parent enumerator when first created.
    var sortedList =
        list.stream()
            .sorted(Comparator.comparing(QuestionDefinition::getId))
            .collect(ImmutableList.toImmutableList());

    QuestionDefinitionNode rootNode = QuestionDefinitionNode.createRootNode();

    for (QuestionDefinition questionDefinition : sortedList) {
      rootNode.addQuestionDefinition(questionDefinition);
    }

    return rootNode;
  }

  /** Get the list of scalars for a question sorted alphabetically */
  private ImmutableList<Scalar> getScalarsSortedByName(QuestionDefinition questionDefinition)
      throws InvalidQuestionTypeException, UnsupportedQuestionTypeException {
    return Scalar.getScalars(questionDefinition.getQuestionType()).stream()
        .sorted(Comparator.comparing(Enum::name))
        .collect(ImmutableList.toImmutableList());
  }

  /** Gets the baseurl without scheme */
  private String getHostName() {
    return openApiSchemaSettings.getBaseUrl().replace("https://", "").replace("http://", "");
  }

  /** Map ScalarType to DefinitionType */
  private DefinitionType getDefinitionTypeFromSwaggerType(ScalarType scalarType) {
    switch (scalarType) {
      case LONG:
        return DefinitionType.INTEGER;
      case DOUBLE:
        return DefinitionType.NUMBER;
      case LIST_OF_STRINGS:
        return DefinitionType.ARRAY;
      case CURRENCY_CENTS:
      case DATE:
      case STRING:
      case PHONE_NUMBER:
      case SERVICE_AREA:
      default:
        return DefinitionType.STRING;
    }
  }

  /** Map ScalarType to Format */
  private Format getSwaggerFormat(ScalarType scalarType) {
    switch (scalarType) {
      case DATE:
        return Format.DATE;
      case LONG:
        return Format.INT64;
      case CURRENCY_CENTS:
      case DOUBLE:
        return Format.DOUBLE;
      case LIST_OF_STRINGS:
        return Format.ARRAY;
      case STRING:
      case PHONE_NUMBER:
      case SERVICE_AREA:
      default:
        return Format.STRING;
    }
  }

  /** Determines if the question should be flagged as nullable */
  private Boolean isNullable(DefinitionType definitionType) {
    return definitionType != DefinitionType.ARRAY && definitionType != DefinitionType.OBJECT;
  }

  /** Rules to determine if a question is included in the schema generation output. */
  private Boolean excludeFromSchemaOutput(QuestionDefinition questionDefinition) {
    // Static questions are not in the api results
    return questionDefinition.getQuestionType() == QuestionType.STATIC;
  }
}
