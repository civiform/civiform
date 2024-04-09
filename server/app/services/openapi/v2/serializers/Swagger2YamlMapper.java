package services.openapi.v2.serializers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import services.openapi.v2.Contact;
import services.openapi.v2.Definition;
import services.openapi.v2.Header;
import services.openapi.v2.Info;
import services.openapi.v2.Operation;
import services.openapi.v2.Parameter;
import services.openapi.v2.Paths;
import services.openapi.v2.Response;
import services.openapi.v2.SecurityDefinition;
import services.openapi.v2.SecurityRequirement;
import services.openapi.v2.Swagger;
import services.openapi.v2.Tag;

public final class Swagger2YamlMapper {
  public static ObjectMapper getMapper() {

    ObjectMapper mapper =
        new YAMLMapper()
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR)
            .enable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS)
            .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
            .disable(YAMLGenerator.Feature.SPLIT_LINES)
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    SimpleModule module =
        new SimpleModule()
            .addSerializer(Swagger.class, new SwaggerSerializer())
            .addSerializer(Info.class, new InfoSerializer())
            .addSerializer(Contact.class, new ContactSerializer())
            .addSerializer(SecurityRequirement.class, new SecurityRequirementSerializer())
            .addSerializer(SecurityDefinition.class, new SecurityDefinitionSerializer())
            .addSerializer(Tag.class, new TagSerializer())
            .addSerializer(Paths.class, new PathsSerializer())
            .addSerializer(Operation.class, new OperationSerializer())
            .addSerializer(Parameter.class, new ParameterSerializer())
            .addSerializer(Definition.class, new DefinitionSerializer())
            .addSerializer(Response.class, new ResponseSerializer())
            .addSerializer(Header.class, new HeaderSerializer());

    mapper.registerModule(module);

    return mapper;
  }
}
