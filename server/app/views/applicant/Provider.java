package views.applicant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Provider {

  private String name;
  private String address;
  private double latitude;
  private double longitude;

  public Provider(
          @JsonProperty("address") String address,
          @JsonProperty("name") String name,
          @JsonProperty("latitude") int latitude,
          @JsonProperty("longitude") int longitude) {
    this.address = address;
    this.name = name;
    this.latitude = latitude;
    this.longitude = longitude;
  }

  @Override
  public String toString() {
    return "{"
        + "\"name\":\""
        + name
        + "\", "
        + "\"address\":\""
        + address
        + "\", "
        + "\"latitude\":"
        + latitude
        + ", "
        + "\"longitude\":"
        + longitude
        + "}";
  }
}

