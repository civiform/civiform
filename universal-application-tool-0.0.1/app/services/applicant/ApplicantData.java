package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.mapper.MappingException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

public class ApplicantData {
  public static final String APPLICANT_PREFIX = "applicant";
  public static final String METADATA_PREFIX = "metadata";
  private static final Splitter JSON_SPLITTER = Splitter.on('.');
  private static final Joiner JSON_JOINER = Joiner.on('.');

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

  // TODO: consider using Jackson or GSON for writing and JsonPath for reading.
  // Perhaps use Jackson or gson for reading as well, and only use JsonPath for predicates.
  public void put(Path path, Object value) {
    List<String> pathElements = JSON_SPLITTER.splitToList(path.withApplicantPrefix());
    this.jsonData.put(
        JSON_JOINER.join(pathElements.subList(0, pathElements.size() - 1)),
        pathElements.get(pathElements.size() - 1),
        value);
  }

  /**
   * Returns the value at the given path, if it exists; otherwise returns {@link Optional#empty}.
   *
   * @param path the {@link Path} for the desired scalar
   * @param type the expected type of the scalar
   * @param <T> the expected type of the scalar
   * @return optionally returns the value at the path if it exists, or empty if not
   * @throws JsonPathTypeMismatchException if the scalar at that path is not the expected type
   */
  public <T> Optional<T> read(Path path, Class<T> type) throws JsonPathTypeMismatchException {
    try {
      return Optional.of(this.jsonData.read(path.withApplicantPrefix(), type));
    } catch (PathNotFoundException e) {
      return Optional.empty();
    } catch (MappingException e) {
      throw new JsonPathTypeMismatchException(path.path(), type, e);
    }
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
