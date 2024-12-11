package services.openapi.v2;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.swagger.models.AuthorizationType;
import io.swagger.models.Contact;
import io.swagger.models.Format;
import io.swagger.models.Info;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Scheme;
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
import io.swagger.models.properties.UntypedProperty;
import io.swagger.util.Yaml;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import services.applicant.question.Scalar;
import services.export.enums.ApiPathSegment;
import services.openapi.OpenApiSchemaGenerator;
import services.openapi.OpenApiSchemaSettings;
import services.openapi.QuestionDefinitionNode;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

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
                              .produces(MimeType.Json.getCode())
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
                                              + " provided date."))
                              .parameter(
                                  new QueryParameter()
                                      .name("toDate")
                                      .type(DefinitionType.STRING.toString())
                                      .description(
                                          "An ISO-8601 formatted date (i.e. YYYY-MM-DD). Limits"
                                              + " results to applications submitted on or after the"
                                              + " provided date."))
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

  /**
   * Returns the list of schemes to allow in the swagger schema. Special case to allow http for
   * non-prod environments for testing purposes.
   */
  private ImmutableList<Scheme> getSchemes() {
    if (openApiSchemaSettings.allowHttpScheme()) {
      return ImmutableList.of(Scheme.HTTP, Scheme.HTTPS);
    }

    return ImmutableList.of(Scheme.HTTPS);
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
  private ImmutableMap<String, Property> buildApplicationDefinitions(
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

          // Boolean nullable = isNullable(definitionType);

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

  private Property getPropertyFromType(
      DefinitionType definitionType,
      Optional<Format> swaggerFormat,
      DefinitionType arrayItemDefinitionType) {

    if (definitionType == DefinitionType.ARRAY) {
      return new ArrayProperty()
          .items(
              arrayItemDefinitionType == DefinitionType.STRING
                  ? new StringProperty()
                  : new ObjectProperty());
    }

    var property = new UntypedProperty();

    property.setType(definitionType.toString());

    if (swaggerFormat.isPresent()) {
      property.setFormat(swaggerFormat.get().toString());
    }

    return property;
  }

  /** Build an n-ary tree from the flat of QuestionDefinition list */
  private QuestionDefinitionNode getQuestionDefinitionRootNode(
      ProgramDefinition programDefinition) {
    // Getting a sorted list to allow placing the enumerator questions
    // into the tree before the questions that are children to the enumerator.
    // An enumerator already has to be created before a question can be added to it
    // and questions can only be assigned a parent enumerator when first created.
    ImmutableList<QuestionDefinition> sortedList =
        programDefinition.blockDefinitions().stream()
            .map(BlockDefinition::programQuestionDefinitions)
            .flatMap(ImmutableList::stream)
            .map(ProgramQuestionDefinition::getQuestionDefinition)
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
        .filter(scalar -> !excludeScalarFromSchemaOutput(scalar))
        .map(scalar -> remapScalar(scalar))
        .sorted(Comparator.comparing(Enum::name))
        .collect(ImmutableList.toImmutableList());
  }

  /** Get the field name from the scalar */
  private String getFieldNameFromScalar(Scalar scalar) {
    // Over output property name where needed
    return switch (scalar) {
      case CURRENCY_CENTS -> ApiPathSegment.CURRENCY_DOLLARS.toString().toLowerCase(Locale.ROOT);
      case FILE_KEY_LIST -> ApiPathSegment.FILE_URLS.toString().toLowerCase(Locale.ROOT);
      case NAME_SUFFIX -> ApiPathSegment.SUFFIX.toString().toLowerCase(Locale.ROOT);
      default -> scalar.name().toLowerCase(Locale.ROOT);
    };
  }

  /** Gets the baseurl without scheme */
  private String getHostName() {
    return openApiSchemaSettings.baseUrl().replace("https://", "").replace("http://", "");
  }

  /** Map Scalar to DefinitionType */
  private DefinitionType getDefinitionTypeFromSwaggerType(Scalar scalar) {
    // Override type because we messed up
    switch (scalar) {
      case LATITUDE, LONGITUDE, WELL_KNOWN_ID -> {
        return DefinitionType.STRING;
      }
    }

    switch (scalar.toScalarType()) {
      case LONG:
        return DefinitionType.INTEGER;
      case CURRENCY_CENTS:
      case DOUBLE:
        return DefinitionType.NUMBER;
      case LIST_OF_STRINGS:
        return DefinitionType.ARRAY;
      case DATE:
      case STRING:
      case PHONE_NUMBER:
      case SERVICE_AREA:
      default:
        return DefinitionType.STRING;
    }
  }

  /** Map ScalarType to Format */
  private Optional<Format> getSwaggerFormat(Scalar scalar) {
    // Override type because we messed up
    switch (scalar) {
      case LATITUDE, LONGITUDE, WELL_KNOWN_ID -> {
        return Optional.empty();
      }
    }

    switch (scalar.toScalarType()) {
      case DATE:
        return Optional.of(Format.DATE);
      case LONG:
        return Optional.of(Format.INT64);
      case CURRENCY_CENTS:
      case DOUBLE:
        return Optional.of(Format.DOUBLE);
      case LIST_OF_STRINGS:
      case STRING:
      case PHONE_NUMBER:
      case SERVICE_AREA:
      default:
        return Optional.empty();
    }
  }

  /** Map Scalar to the DefinitionType of an array */
  private DefinitionType getArrayItemDefinitionType(Scalar scalar) {
    return switch (scalar) {
      case SELECTIONS, FILE_KEY_LIST -> DefinitionType.STRING;
      default -> DefinitionType.OBJECT;
    };
  }

  /** Determines if the question should be flagged as nullable */
  private Boolean isNullable(DefinitionType definitionType) {
    return definitionType != DefinitionType.ARRAY && definitionType != DefinitionType.OBJECT;
  }

  /** Rules to determine if need to remap scalar to another scalar */
  private Scalar remapScalar(Scalar scalar) {
    return switch (scalar) {
        // Special handling of service area as a string until we migrate the API to returning a
        // complex array
      case SERVICE_AREAS -> Scalar.SERVICE_AREA;
      default -> scalar;
    };
  }

  /** Rules to determine if a question is included in the schema generation output. */
  private Boolean excludeQuestionFromSchemaOutput(QuestionDefinition questionDefinition) {
    // Static questions are not in the api results
    return questionDefinition.getQuestionType() == QuestionType.STATIC;
  }

  /** Rules to determine if a scalar is included in the schema generation output */
  private Boolean excludeScalarFromSchemaOutput(Scalar scalar) {
    return switch (scalar) {
      case ORIGINAL_FILE_NAME -> true;
      default -> false;
    };
  }
}
