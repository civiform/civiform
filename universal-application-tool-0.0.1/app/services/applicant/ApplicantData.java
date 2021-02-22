package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.jayway.jsonpath.DocumentContext;
import java.time.Instant;

public class ApplicantData {

  private DocumentContext jsonData;

  public ApplicantData(DocumentContext jsonData) {
    this.jsonData = checkNotNull(jsonData);
  }

  public void put(String path, String key, String value) {
    this.jsonData.put(path, key, value);
  }

  public void put(String path, String key, Object value) {
    this.jsonData.put(path, key, value);
  }

  public <T> T read(String path, Class<T> type) {
    return this.jsonData.read(path, type);
  }

  public Instant getCreatedTime() {
    return Instant.parse(this.jsonData.read("$.metadata.created_time"));
  }

  public void setCreatedTime(Instant i) {
    this.jsonData.put("$.metadata", "created_time", i);
  }

  public String asJsonString() {
    return this.jsonData.jsonString();
  }
}
