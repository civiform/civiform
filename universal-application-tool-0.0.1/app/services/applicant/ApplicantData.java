package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.mapper.MappingException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.Path;

public class ApplicantData {
  // Suppress errors thrown by JsonPath and instead return null if a path does not exist in a JSON
  // blob.
  private static final Configuration CONFIGURATION =
      Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS);
  private static final String EMPTY_APPLICANT_DATA_JSON = "{ \"applicant\": {}, \"metadata\": {} }";

  private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
  private static final Logger LOG = LoggerFactory.getLogger(ApplicantData.class);
  private static final Locale DEFAULT_LOCALE = Locale.US;

  private Locale preferredLocale;
  private DocumentContext jsonData;

  public ApplicantData() {
    this(EMPTY_APPLICANT_DATA_JSON);
  }

  public ApplicantData(String jsonData) {
    this(DEFAULT_LOCALE, jsonData);
  }

  public ApplicantData(Locale preferredLocale, String jsonData) {
    this.preferredLocale = preferredLocale;
    this.jsonData = JsonPath.using(CONFIGURATION).parse(checkNotNull(jsonData));
  }

  public Locale preferredLocale() {
    return this.preferredLocale;
  }

  public void setPreferredLocale(Locale locale) {
    this.preferredLocale = locale;
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
   * Attempt to read a string at the given path. Returns {@code Optional#empty} if the path does not
   * exist or a value other than String is found.
   */
  public Optional<String> readString(Path path) {
    try {
      return this.read(path, String.class);
    } catch (JsonPathTypeMismatchException e) {
      return Optional.empty();
    }
  }

  /**
   * Attempt to read a integer at the given path. Returns {@code Optional#empty} if the path does
   * not exist or a value other than Integer is found.
   */
  public Optional<Integer> readInteger(Path path) {
    try {
      return this.read(path, Integer.class);
    } catch (JsonPathTypeMismatchException e) {
      return Optional.empty();
    }
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
  private <T> Optional<T> read(Path path, Class<T> type) throws JsonPathTypeMismatchException {
    try {
      return Optional.ofNullable(this.jsonData.read(path.path(), type));
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

  public boolean hasPath(Path path) {
    try {
      return read(path, Object.class).isPresent();
    } catch (JsonPathTypeMismatchException e) {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(jsonData.jsonString());
  }

  /**
   * Copies all keys from {@code other}, recursively. All lists are merged. No values will be
   * overwritten.
   *
   * @return A list of {@code Path}s whose values could not be copied due to conflicts.
   */
  public ImmutableList<Path> mergeFrom(ApplicantData other) {
    Map<?, ?> rootAsMap = other.jsonData.read("$", Map.class);
    return mergeFrom(Path.empty(), rootAsMap);
  }

  private ImmutableList<Path> mergeFrom(Path rootKey, Map<?, ?> other) {
    ImmutableList.Builder<Path> pathsRemoved = new ImmutableList.Builder<>();
    for (Map.Entry<?, ?> entry : other.entrySet()) {
      String key = entry.getKey().toString();
      Path path = rootKey.toBuilder().append(key).build();
      if (hasPath(path)) {
        if (entry.getValue() instanceof Map) {
          // Recurse into maps.
          pathsRemoved.addAll(mergeFrom(path, (Map) entry.getValue()));
        } else if (entry.getValue() instanceof List) {
          // Add items from lists.
          // TODO(github.com/seattle-uat/civiform/issues/405): improve merge for repeated fields.
          for (Object item : (List) entry.getValue()) {
            this.jsonData.add(path.path(), item);
          }
        } else {
          try {
            if (!this.read(path, Object.class).equals(entry.getValue())) {
              pathsRemoved.add(path);
            }
          } catch (JsonPathTypeMismatchException e) {
            // If we can't confirm they're equal, consider it removed.
            pathsRemoved.add(path);
          }
        }
      } else {
        // currently empty, can add.
        this.put(path, entry.getValue());
      }
    }
    return pathsRemoved.build();
  }

  public void setUserName(String displayName) {
    String firstName;
    String lastName = null;
    String middleName = null;
    List<String> listSplit = Splitter.on(' ').splitToList(displayName);
    switch (listSplit.size()) {
      case 2:
        firstName = listSplit.get(0);
        lastName = listSplit.get(1);
        break;
      case 3:
        firstName = listSplit.get(0);
        middleName = listSplit.get(1);
        lastName = listSplit.get(2);
        break;
      case 1:
        // fallthrough
      default:
        // Too many names - put them all in first name.
        firstName = displayName;
    }
    setUserName(firstName, middleName, lastName);
  }

  private void setUserName(
      String firstName, @Nullable String middleName, @Nullable String lastName) {
    // We don't have a question for this yet, so this is unimplemented right now.
    LOG.warn(
        "Have not implemented setUserName yet - %s, %s, %s are our first, middle, last.",
        firstName, middleName, lastName);
  }
}
