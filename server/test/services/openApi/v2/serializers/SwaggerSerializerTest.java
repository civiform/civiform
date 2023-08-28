package services.openApi.v2.serializers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import services.openApi.v2.Info;
import services.openApi.v2.PathItem;
import services.openApi.v2.Paths;
import services.openApi.v2.Scheme;
import services.openApi.v2.SecurityDefinition;
import services.openApi.v2.SecurityRequirement;
import services.openApi.v2.SecurityType;
import services.openApi.v2.Swagger;
import services.openApi.v2.Tag;

public class SwaggerSerializerTest extends OpenApiSerializationAsserter {
  @Test
  public void canSerializeFullObject() throws JsonProcessingException {
    Swagger model =
        Swagger.builder()
            .setBasePath("basePath1")
            .setHost("host1")
            .setInfo(Info.builder("title1", "version1").build())
            .setPaths(Paths.builder().addPathItem(PathItem.builder("/ref1").build()).build())
            .addScheme(Scheme.HTTPS)
            .addSecurityRequirement(SecurityRequirement.builder(SecurityType.BASIC).build())
            .addSecurityDefinition(SecurityDefinition.basicBuilder().build())
            .addTag(Tag.builder("tagname1").build())
            .build();

    String expected =
        new YamlFormatter()
            .appendLine("swagger: 2.0")
            .appendLine("basePath: basePath1")
            .appendLine("host: host1")
            .appendLine("info:")
            .appendLine("  title: title1")
            .appendLine("  version: version1")
            .appendLine("schemes:")
            .appendLine("  - https")
            .appendLine("security:")
            .appendLine("  - basicAuth: []")
            .appendLine("securityDefinitions:")
            .appendLine("  basicAuth:")
            .appendLine("    type: basic")
            .appendLine("tags:")
            .appendLine("  - name: tagname1")
            .appendLine("paths:")
            .appendLine("  /ref1: {}")
            .toString();

    assertSerialization(model, expected);
  }

  @Test
  public void canSerializeEmptyObject() throws JsonProcessingException {
    Swagger model = Swagger.builder().setInfo(Info.builder("title1", "version1").build()).build();

    String expected =
        new YamlFormatter()
            .appendLine("swagger: 2.0")
            .appendLine("info:")
            .appendLine("  title: title1")
            .appendLine("  version: version1")
            .toString();

    assertSerialization(model, expected);
  }
}
