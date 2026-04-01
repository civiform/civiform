package views.admin.shared;

import lombok.Builder;
import services.JsonUtils;
import services.ObjectMapperSingleton;

@Builder
public record DevInfo(
    String layoutTemplate,
    String pageTemplate,
    String controller,
    String method,
    String verb,
    String path,
    Object model) {

  public String toJson() {
    return JsonUtils.writeValueAsString(ObjectMapperSingleton.instance(), this);
  }
}
