package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.mapper.MappingException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import services.Path;
import services.WellKnownPaths;

public class ApplicantData {
  private static final String EMPTY_APPLICANT_DATA_JSON = "{ \"applicant\": {}, \"metadata\": {} }";
  private static final Locale DEFAULT_LOCALE = Locale.US;
  private static final Joiner LIST_JOINER = Joiner.on('`');
  private static final Splitter LIST_SPLITTER = Splitter.on('`');

  private Locale preferredLocale;
  private final DocumentContext jsonData;

  public ApplicantData() {
    this(EMPTY_APPLICANT_DATA_JSON);
  }

  public ApplicantData(String jsonData) {
    this(DEFAULT_LOCALE, jsonData);
  }

  public ApplicantData(Locale preferredLocale, String jsonData) {
    this.preferredLocale = preferredLocale;
    this.jsonData = JsonPath.parse(checkNotNull(jsonData));
  }

  public Locale preferredLocale() {
    return this.preferredLocale;
  }

  public void setPreferredLocale(Locale locale) {
    this.preferredLocale = locale;
  }

  /**
   * Checks whether the given path exists in the JSON data. Returns true if the path is present;
   * false otherwise. Semantically, this checks whether the applicant has answered this question
   * before.
   *
   * @param path the {@link Path} to check
   * @return true if path is present for this applicant; false otherwise
   */
  public boolean hasPath(Path path) {
    try {
      this.jsonData.read(path.path());
    } catch (PathNotFoundException e) {
      return false;
    }
    return true;
  }

  /**
   * Returns true if there is a non-null value at the given {@link Path}; false otherwise. Will
   * return false if there is a null value at the path.
   *
   * @param path the {@link Path} to check
   * @return true if there is a non-null value at the given path; false otherwise
   */
  public boolean hasValueAtPath(Path path) {
    try {
      return read(path, Object.class).isPresent();
    } catch (JsonPathTypeMismatchException e) {
      return false;
    }
  }

  /**
   * Write the given string at the given {@link Path}. If the string is empty, it will write a null
   * value instead.
   */
  public void putString(Path path, String value) {
    if (value.isEmpty()) {
      putNull(path);
    } else {
      put(path, value);
    }
  }

  public void putLong(Path path, long value) {
    put(path, value);
  }

  /**
   * Parses and writes a long value, given as a string. If the string is empty, a null value is
   * written.
   */
  public void putLong(Path path, String value) {
    if (value.isEmpty()) {
      putNull(path);
    } else {
      put(path, Long.parseLong(value));
    }
  }

  public void putList(Path path, ImmutableList<String> value) {
    if (value.isEmpty()) {
      putNull(path);
    } else {
      put(path, LIST_JOINER.join(value));
    }
  }

  private void putNull(Path path) {
    put(path, null);
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
    if (path.parentPath().isEmpty()) {
      jsonData.put(Path.JSON_PATH_START_TOKEN, path.keyName(), value);
      return;
    }
    if (!hasPath(path.parentPath())) {
      put(path.parentPath(), new HashMap<>());
    }
    jsonData.put(path.parentPath().toString(), path.keyName(), value);
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
  public Optional<Long> readLong(Path path) {
    try {
      return this.read(path, Long.class);
    } catch (JsonPathTypeMismatchException e) {
      return Optional.empty();
    }
  }

  /**
   * Attempt to read a list at the given {@link Path}. Returns {@code Optional#empty} if the path
   * does not exist or a value other than an {@link ImmutableList} of strings is found.
   */
  public Optional<ImmutableList<String>> readList(Path path) {
    Optional<String> listAsString = readString(path);
    if (listAsString.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(ImmutableList.copyOf(LIST_SPLITTER.splitToList(listAsString.get())));
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
    if (!hasPath(WellKnownPaths.APPLICANT_FIRST_NAME)) {
      putString(WellKnownPaths.APPLICANT_FIRST_NAME, firstName);
    }
    if (middleName != null && !hasPath(WellKnownPaths.APPLICANT_MIDDLE_NAME)) {
      putString(WellKnownPaths.APPLICANT_MIDDLE_NAME, middleName);
    }
    if (lastName != null && !hasPath(WellKnownPaths.APPLICANT_LAST_NAME)) {
      putString(WellKnownPaths.APPLICANT_LAST_NAME, lastName);
    }
  }
}
