package services.openapi.v2;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;

/**
 * This is the root document object for the API specification. It combines what previously was the
 * Resource Listing and API Declaration (version 1.2 and earlier) together into one document.
 *
 * <p>https://swagger.io/specification/v2/#swagger-object
 */
@AutoValue
public abstract class Swagger {
  private static final String swagger = "2.0";

  /**
   * Required.
   *
   * <p>Specifies the Swagger Specification version being used. It can be used by the Swagger UI and
   * other clients to interpret the API listing. The value MUST be "2.0".
   */
  public final String getSwagger() {
    return swagger;
  }

  /**
   * The base path on which the API is served, which is relative to the host. If it is not included,
   * the API is served directly under the host. The value MUST start with a leading slash (/). The
   * basePath does not support path templating.
   */
  public abstract Optional<String> getBasePath();

  /**
   * The host (name or ip) serving the API. This MUST be the host only and does not include the
   * scheme nor sub-paths. It MAY include a port. If the host is not included, the host serving the
   * documentation is to be used (including the port). The host does not support path templating.
   */
  public abstract Optional<String> getHost();

  /**
   * Required.
   *
   * <p>Provides metadata about the API. The metadata can be used by the clients if needed.
   */
  public abstract Info getInfo();

  /**
   * The transfer protocol of the API. Values MUST be from the list: "http", "https", "ws", "wss".
   * If the schemes is not included, the default scheme to be used is the one used to access the
   * Swagger definition itself.
   */
  public abstract ImmutableList<Scheme> getSchemes();

  /**
   * A declaration of which security schemes are applied for the API as a whole. The list of values
   * describes alternative security schemes that can be used (that is, there is a logical OR between
   * the security requirements). Individual operations can override this definition.
   */
  public abstract ImmutableList<SecurityRequirement> getSecurityRequirements();

  /** Security scheme definitions that can be used across the specification. */
  public abstract ImmutableList<SecurityDefinition> getSecurityDefinitions();

  /**
   * A list of tags used by the specification with additional metadata. The order of the tags can be
   * used to reflect on their order by the parsing tools. Not all tags that are used by the
   * Operation Object must be declared. The tags that are not declared may be organized randomly or
   * based on the tools' logic. Each tag name in the list MUST be unique.
   */
  public abstract ImmutableList<Tag> getTags();

  /**
   * Required.
   *
   * <p>The available paths and operations for the API.
   */
  public abstract Optional<Paths> getPaths();

  /** An object to hold data types produced and consumed by operations. */
  public abstract ImmutableList<Definition> getDefinitions();

  public static Swagger.Builder builder() {
    return new AutoValue_Swagger.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Swagger.Builder setBasePath(String basePath);

    public abstract Swagger.Builder setHost(String host);

    public abstract Swagger.Builder setInfo(Info info);

    public abstract Swagger.Builder setSchemes(ImmutableList<Scheme> schemes);

    public abstract Swagger.Builder setPaths(Paths paths);

    protected abstract ImmutableList.Builder<SecurityRequirement> securityRequirementsBuilder();

    protected abstract ImmutableList.Builder<SecurityDefinition> securityDefinitionsBuilder();

    protected abstract ImmutableList.Builder<Tag> tagsBuilder();

    protected abstract ImmutableList.Builder<Scheme> schemesBuilder();

    protected abstract ImmutableList.Builder<Definition> definitionsBuilder();

    public Swagger.Builder addSecurityRequirement(SecurityRequirement securityRequirement) {
      securityRequirementsBuilder().add(securityRequirement);
      return this;
    }

    public Swagger.Builder addSecurityDefinition(SecurityDefinition securityDefinition) {
      securityDefinitionsBuilder().add(securityDefinition);
      return this;
    }

    public Swagger.Builder addTag(Tag tag) {
      tagsBuilder().add(tag);
      return this;
    }

    public Swagger.Builder addScheme(Scheme scheme) {
      schemesBuilder().add(scheme);
      return this;
    }

    public Swagger.Builder addDefinition(Definition definition) {
      definitionsBuilder().add(definition);
      return this;
    }

    public abstract Swagger build();
  }
}
