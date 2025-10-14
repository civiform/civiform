package views.admin.apibridge.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.Builder;
import services.apibridge.ApiBridgeServiceDto;
import views.admin.BaseViewModel;

/** Contains all the properties for rendering the DiscoveryDetailsPartial.html */
@Builder
public record DiscoveryDetailsPartialViewModel(
    String hostUrl, ImmutableMap<String, ApiBridgeServiceDto.Endpoint> endpoints)
    implements BaseViewModel {

  @Builder
  public record SchemaField(String name, String title, String description, String type) {}

  public String hxAddUrl() {
    return controllers.admin.apibridge.routes.DiscoveryController.hxAdd().url();
  }

  public ImmutableList<SchemaField> getSchema(JsonNode jsonNode) {
    return jsonNode
        .get("properties")
        .propertyStream()
        .map(
            x ->
                SchemaField.builder()
                    .name(x.getKey())
                    .title(x.getValue().get("title").textValue())
                    .description(x.getValue().get("description").textValue())
                    .type(x.getValue().get("type").textValue())
                    .build())
        .collect(ImmutableList.toImmutableList());
  }
}
