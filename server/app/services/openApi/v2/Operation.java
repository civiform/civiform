package services.openApi.v2;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;

/**
 * Describes a single API operation on a path.
 *
 * <p>https://swagger.io/specification/v2/#operation-object
 */
@AutoValue
public abstract class Operation {
  public abstract OperationType getOperationType();

  /**
   * Unique string used to identify the operation. The id MUST be unique among all operations
   * described in the API. Tools and libraries MAY use the operationId to uniquely identify an
   * operation, therefore, it is recommended to follow common programming naming conventions.
   */
  public abstract Optional<String> getOperationId();

  /**
   * A verbose explanation of the operation behavior. GFM syntax can be used for rich text
   * representation.
   */
  public abstract Optional<String> getDescription();

  /**
   * A short summary of what the operation does. For maximum readability in the swagger-ui, this
   * field SHOULD be less than 120 characters.
   */
  public abstract Optional<String> getSummary();

  /**
   * A list of MIME types the operation can produce. This overrides the produces definition at the
   * Swagger Object. An empty value MAY be used to clear the global definition. Value MUST be as
   * described under Mime Types.
   */
  public abstract ImmutableList<MimeType> getProduces();

  /**
   * Required.
   *
   * <p>The list of possible responses as they are returned from executing this operation.
   */
  public abstract ImmutableList<Response> getResponses();

  /**
   * A list of tags for API documentation control. Tags can be used for logical grouping of
   * operations by resources or any other qualifier.
   */
  public abstract ImmutableList<String> getTags();

  /**
   * A list of parameters that are applicable for this operation. If a parameter is already defined
   * at the Path Item, the new definition will override it, but can never remove it. The list MUST
   * NOT include duplicated parameters. A unique parameter is defined by a combination of a name and
   * location. The list can use the Reference Object to link to parameters that are defined at the
   * Swagger Object's parameters. There can be one "body" parameter at most.
   */
  public abstract ImmutableList<Parameter> getParameters();

  public static Operation.Builder builder(OperationType operationType) {
    return new AutoValue_Operation.Builder().setOperationType(operationType);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    protected abstract Operation.Builder setOperationType(OperationType operationType);

    protected abstract ImmutableList.Builder<MimeType> producesBuilder();

    protected abstract ImmutableList.Builder<Response> responsesBuilder();

    protected abstract ImmutableList.Builder<String> tagsBuilder();

    protected abstract ImmutableList.Builder<Parameter> parametersBuilder();

    public abstract Operation.Builder setSummary(String summary);

    public abstract Operation.Builder setOperationId(String operationId);

    public abstract Operation.Builder setDescription(String description);

    public Builder addProduces(MimeType produce) {
      producesBuilder().add(produce);
      return this;
    }

    public Builder addResponse(Response response) {
      responsesBuilder().add(response);
      return this;
    }

    public Builder addTag(String tag) {
      tagsBuilder().add(tag);
      return this;
    }

    public Builder addParameter(Parameter parameter) {
      parametersBuilder().add(parameter);
      return this;
    }

    public abstract Operation build();
  }
}
