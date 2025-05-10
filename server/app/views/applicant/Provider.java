package views.applicant;

import lombok.Getter;

@Getter
public class Provider {

  private String name;
  private String address;
  private double latitude;
  private double longitude;

  public void setName(String name) {
    this.name = name;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  public void setLongitude(double longitude) {
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
