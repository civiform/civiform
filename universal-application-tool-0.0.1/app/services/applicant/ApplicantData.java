package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.mapper.MappingException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

public class ApplicantData {
  // Suppress errors thrown by JsonPath and instead return null if a path does not exist in a JSON
  // blob.
  private static final Configuration CONFIGURATION =
      Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS);

  private DocumentContext jsonData;
  private static final String EMPTY_APPLICANT_DATA_JSON = "{ \"applicant\": {}, \"metadata\": {} }";

  public ApplicantData() {
    this(EMPTY_APPLICANT_DATA_JSON);
  }

  public ApplicantData(String jsonData) {
    this.jsonData = JsonPath.using(CONFIGURATION).parse(checkNotNull(jsonData));
  }

  public Locale preferredLocale() {
    return Locale.ENGLISH;
  }

  public void putString(Path path, String value) {
    put(path, value);
  }

  public void putInteger(Path path, int value) {
    put(path, value);
  }

  public <K, V> void putObject(Path path, ImmutableMap<K, V> value) {
    put(path, value);
  }

  /**
   * Puts the given value at the given path in the underlying JSON data. Builds up the necessary
   * structure along the way, i.e., creates parent objects where necessary.
   *
   * @param path the {@link Path} with the fully specified path, e.g., "applicant.favorites.color"
   *     or the equivalent "$.applicant.favorite.colors".
   * @param value the value to place; values of type Map will create the equivalent JSON structure
   */
  private void put(Path path, Object value) {
    path.parentPaths()
        .forEach(
            segmentPath -> {
              if (this.jsonData.read(segmentPath.path()) == null) {
                this.jsonData.put(
                    segmentPath.parentPath().path(), segmentPath.keyName(), new HashMap<>());
              }
            });

    this.jsonData.put(path.parentPath().path(), path.keyName(), value);
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
      return Optional.ofNullable(this.jsonData.read(path.path(), type));
    } catch (MappingException e) {
      throw new JsonPathTypeMismatchException(path.path(), type, e);
    }
  }

  /** Same as the above, but accepts path as a string. */
  public <T> Optional<T> read(String pathAsString, Class<T> type)
      throws JsonPathTypeMismatchException {
    return this.read(Path.create(pathAsString), type);
  }

  /**
   * Attempt to read a string at the given path. Returns {@code Optional#empty} if the path does not
   * exist or a value other than String is found.
   */
  public Optional<String> readString(String pathAsString) {
    try {
      return this.read(pathAsString, String.class);
    } catch (JsonPathTypeMismatchException e) {
      return Optional.empty();
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
