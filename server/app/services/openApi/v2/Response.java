package services.openApi.v2;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;

/**
 * https://swagger.io/specification/v2/#responses-object
 *
 * <p>Describes a single response from an API Operation.
 */
@AutoValue
public abstract class Response {
  public abstract HttpStatusCode getHttpStatusCode();

  /**
   * Required.
   *
   * <p>A short description of the response. GFM syntax can be used for rich text representation.
   */
  public abstract String getDescription();

  /** Get all defined headers */
  public abstract ImmutableList<Header> getHeaders();

  /**
   * A definition of the response structure. It can be a primitive, an array or an object. If this
   * field does not exist, it means no content is returned as part of the response. As an extension
   * to the Schema Object, its root type value may also be "file". This SHOULD be accompanied by a
   * relevant produces mime-type.
   */
  public abstract Optional<String> getSchema();

  public static Response.Builder builder(HttpStatusCode httpStatusCode, String description) {
    return new AutoValue_Response.Builder()
        .setHttpStatusCode(httpStatusCode)
        .setDescription(description);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    protected abstract Response.Builder setHttpStatusCode(HttpStatusCode httpStatusCode);

    protected abstract Response.Builder setDescription(String description);

    public abstract Response.Builder setSchema(String schema);

    protected abstract ImmutableList.Builder<Header> headersBuilder();

    public Response.Builder addHeader(Header header) {
      headersBuilder().add(header);
      return this;
    }

    public abstract Response build();
  }
}
