package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import com.jayway.jsonpath.PathNotFoundException;
import java.util.Locale;
import java.util.Optional;

public class ApplicantData {

  private DocumentContext jsonData;
  private static final String EMPTY_APPLICANT_DATA_JSON = "{ \"applicant\": {}, \"metadata\": {} }";

  public ApplicantData() {
    this(JsonPath.parse(EMPTY_APPLICANT_DATA_JSON));
  }

  public ApplicantData(DocumentContext jsonData) {
    this.jsonData = checkNotNull(jsonData);
  }

  public Locale preferredLocale() {
    return Locale.ENGLISH;
  }

  public Optional<String> readText(String path) {
    try {
      return Optional.of(jsonData.read(path, String.class));
    } catch (PathNotFoundException e) {
      return Optional.empty();
    }
  }

  // TODO: Deprecate
  public void put(String path, String key, String value) {
    this.jsonData.put(path, key, value);
  }

  // TODO: Deprecate
  public <T> T read(String path, Class<T> type) {
    return this.jsonData.read(path, type);
  }

  public Instant getCreatedTime() {
    return Instant.parse(this.jsonData.read("$.metadata.created_time"));
  }

  public void setCreatedTime(Instant i) {
    this.jsonData.put("$.metadata", "created_time", i.toString());
  }

  public String asJsonString() {
    return this.jsonData.jsonString();
  }
}
