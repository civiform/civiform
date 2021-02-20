package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.jayway.jsonpath.DocumentContext;

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

  public String asJsonString() {
    return this.jsonData.jsonString();
  }
}
