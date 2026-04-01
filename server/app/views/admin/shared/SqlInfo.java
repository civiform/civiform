package views.admin.shared;

import lombok.Builder;
import services.JsonUtils;
import services.ObjectMapperSingleton;

@Builder
public record SqlInfo(Object model) {

  public String toJson() {
    return JsonUtils.writeValueAsString(ObjectMapperSingleton.instance(), this);
  }
}
