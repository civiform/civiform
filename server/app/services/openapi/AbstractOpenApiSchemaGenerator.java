package services.openapi;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import io.swagger.models.Scheme;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import services.applicant.question.Scalar;
import services.export.enums.ApiPathSegment;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

public abstract class AbstractOpenApiSchemaGenerator {
  protected final OpenApiSchemaSettings openApiSchemaSettings;

  protected AbstractOpenApiSchemaGenerator(OpenApiSchemaSettings openApiSchemaSettings) {
    this.openApiSchemaSettings = checkNotNull(openApiSchemaSettings);
  }

  /**
   * Returns the list of schemes to allow in the swagger schema. Special case to allow http for
   * non-prod environments for testing purposes.
   */
  protected ImmutableList<Scheme> getSchemes() {
    if (openApiSchemaSettings.allowHttpScheme()) {
      return ImmutableList.of(Scheme.HTTP, Scheme.HTTPS);
    }

    return ImmutableList.of(Scheme.HTTPS);
  }

  /** Gets the baseurl without scheme */
  protected String getHostName() {
    return openApiSchemaSettings.baseUrl().replace("https://", "").replace("http://", "");
  }

  /** Get the list of scalars for a question sorted alphabetically */
  protected static ImmutableList<Scalar> getScalarsSortedByName(
      QuestionDefinition questionDefinition)
      throws InvalidQuestionTypeException, UnsupportedQuestionTypeException {
    return Scalar.getScalars(questionDefinition.getQuestionType()).stream()
        .filter(scalar -> !excludeScalarFromSchemaOutput(scalar))
        .map(AbstractOpenApiSchemaGenerator::remapScalar)
        .sorted(Comparator.comparing(Enum::name))
        .collect(ImmutableList.toImmutableList());
  }

  /** Get the field name from the scalar */
  protected static String getFieldNameFromScalar(Scalar scalar) {
    // Over output property name where needed
    return switch (scalar) {
      case CURRENCY_CENTS -> ApiPathSegment.CURRENCY_DOLLARS.toString().toLowerCase(Locale.ROOT);
      case FILE_KEY_LIST -> ApiPathSegment.FILE_URLS.toString().toLowerCase(Locale.ROOT);
      case NAME_SUFFIX -> ApiPathSegment.SUFFIX.toString().toLowerCase(Locale.ROOT);
      default -> scalar.name().toLowerCase(Locale.ROOT);
    };
  }

  /** Map Scalar to DefinitionType */
  protected static DefinitionType getDefinitionTypeFromSwaggerType(Scalar scalar) {
    // Override type because we messed up
    if (scalar == Scalar.LATITUDE || scalar == Scalar.LONGITUDE || scalar == Scalar.WELL_KNOWN_ID) {
      return DefinitionType.STRING;
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
  protected static Optional<Format> getSwaggerFormat(Scalar scalar) {
    // Override type because we messed up
    if (scalar == Scalar.LATITUDE || scalar == Scalar.LONGITUDE || scalar == Scalar.WELL_KNOWN_ID) {
      return Optional.empty();
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
  protected static DefinitionType getArrayItemDefinitionType(Scalar scalar) {
    return switch (scalar) {
      case SELECTIONS, FILE_KEY_LIST -> DefinitionType.STRING;
      default -> DefinitionType.OBJECT;
    };
  }

  /** Determines if the question should be flagged as nullable */
  protected static Boolean isNullable(DefinitionType definitionType) {
    return definitionType != DefinitionType.ARRAY && definitionType != DefinitionType.OBJECT;
  }

  /** Rules to determine if need to remap scalar to another scalar */
  protected static Scalar remapScalar(Scalar scalar) {
    return switch (scalar) {
        // Special handling of service area as a string until we migrate the API to returning a
        // complex array
      case SERVICE_AREAS -> Scalar.SERVICE_AREA;
      default -> scalar;
    };
  }

  /** Rules to determine if a question is included in the schema generation output. */
  protected static Boolean excludeQuestionFromSchemaOutput(QuestionDefinition questionDefinition) {
    // Static questions are not in the api results
    return questionDefinition.getQuestionType() == QuestionType.STATIC;
  }

  /** Rules to determine if a scalar is included in the schema generation output */
  protected static Boolean excludeScalarFromSchemaOutput(Scalar scalar) {
    return switch (scalar) {
      case ORIGINAL_FILE_NAME -> true;
      default -> false;
    };
  }

  /** Build an n-ary tree from the flat of QuestionDefinition list */
  protected static QuestionDefinitionNode getQuestionDefinitionRootNode(
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
}
