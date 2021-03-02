package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

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

  /**
   * Attempts to read a string at the given path in the applicant's answer data. Returns an {@code
   * Optional.empty()} if the path does not exist.
   *
   * <p>The design suggestion for these methods is to have one per {@link
   * services.question.ScalarType}
   */
  public Optional<String> readString(String path) {
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

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof ApplicantData) {
      ApplicantData that = (ApplicantData) object;
      // Need to compare the JSON strings rather than the DocumentContexts themselves since
      // DocumentContext does not override equals.
      return this.jsonData.jsonString().equals(that.jsonData.jsonString());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(jsonData.jsonString());
  }
}
