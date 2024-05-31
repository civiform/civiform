package conf;

import static com.google.auto.common.MoreStreams.toImmutableList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.MessageKey;

/**
 * Tests that the messages files are in sync. Reads in the keys from the primary language file,
 * `messages`, and ensures the keys are in sync with the `messages.*` files.
 */
@RunWith(JUnitParamsRunner.class)
public class MessagesTest {

  // The directory where messages files are stored.
  private static final String PREFIX_PATH = "conf/i18n/";

  // The file name of the primary language file.
  private static final String PRIMARY_LANGUAGE_FILE_NAME = "messages";

  // The file name of the en-US file.
  private static final String EN_US_LANGUAGE_FILE_NAME = "messages.en-US";

  private static final String PRIMARY_LANGUAGE_FILE_PATH = PREFIX_PATH + PRIMARY_LANGUAGE_FILE_NAME;

  private static final String EN_US_LANGUAGE_FILE_PATH = PREFIX_PATH + EN_US_LANGUAGE_FILE_NAME;

  // Check for prohibited characters, such as smart quotes, that may have been added by mistake by
  // Excel or Sheets
  private static final Set<String> PROHIBITED_CHARACTERS =
      Set.of(
          // "Smart quotes"
          "\u2019", // RIGHT SINGLE QUOTATION MARK
          "\u201C", // LEFT DOUBLE QUOTATION MARK
          "\u201D" // RIGHT DOUBLE QUOTATION MARK
          );

  // Words that should not be in the internationalization files, such as civic entity-specific
  // names.
  private static final Set<String> PROHIBITED_WORDS =
      Set.of(
          "Seattle",
          "Washington",
          "Bloomington",
          "Indiana",
          "Arkansas",
          "Charlotte",
          "North Carolina");

  @Test
  @Parameters(method = "foreignLanguageFiles")
  public void messages_keysInForeignLanguageFileAreInPrimaryLanguageFile(String foreignLanguageFile)
      throws Exception {
    Set<String> keysInPrimaryFile = keysInFile(PRIMARY_LANGUAGE_FILE_PATH);
    Set<String> keysInForeignLangFile = keysInFile(foreignLanguageFile);

    // Checks that the foreign language file is a subset of the primary language file.
    assertThat(keysInPrimaryFile)
        .withFailMessage(
            errorMessage(keysInPrimaryFile, keysInForeignLangFile, foreignLanguageFile))
        .containsAll(keysInForeignLangFile);
  }

  @Test
  public void messages_MessagesEnUs_isEmpty() throws Exception {
    Map<String, String> entriesInEnUsLanguageFile = entriesInFile(EN_US_LANGUAGE_FILE_PATH);

    // messages.en-US should be empty and allow all keys to fall back to the messages file.
    assertThat(entriesInEnUsLanguageFile).isEmpty();
  }

  @Test
  public void messages_primaryFile_containsNoProhibitedWords() throws Exception {
    Map<String, String> entriesInPrimaryLanguageFile = entriesInFile(PRIMARY_LANGUAGE_FILE_PATH);

    assertThat(entriesInPrimaryLanguageFile.values())
        .allSatisfy(
            sourceString -> {
              PROHIBITED_WORDS.forEach(
                  prohibitedWord -> {
                    assertThat(sourceString.toLowerCase(Locale.US))
                        .withFailMessage(prohibitedWord + " found in primary language file.")
                        .doesNotContain(prohibitedWord.toLowerCase(Locale.US));
                  });
            });
  }

  @Test
  public void messages_primaryFile_containsNoProhibitedCharacters() throws Exception {
    Map<String, String> entriesInPrimaryLanguageFile = entriesInFile(PRIMARY_LANGUAGE_FILE_PATH);

    assertThat(entriesInPrimaryLanguageFile)
        .withFailMessage("Prohibited characters found in primary language file..")
        .allSatisfy(
            (key, value) -> {
              assertThat(key).doesNotContain(PROHIBITED_CHARACTERS);
              assertThat(value).doesNotContain(PROHIBITED_CHARACTERS);
            });
  }

  @Test
  @Parameters(method = "foreignLanguageFiles")
  public void messages_foreignLanguageFiles_containNoProhibitedCharacters(
      String foreignLanguageFile) throws Exception {
    Map<String, String> entriesInforeignLanguageFile = entriesInFile(foreignLanguageFile);

    assertThat(entriesInforeignLanguageFile)
        .withFailMessage("Prohibited characters found in " + foreignLanguageFile + ".")
        .allSatisfy(
            (key, value) -> {
              assertThat(key).doesNotContain(PROHIBITED_CHARACTERS);
              assertThat(value).doesNotContain(PROHIBITED_CHARACTERS);
            });
  }

  @Test
  public void messageKeyValuesAndMessagesFileKeysAreIdentical() throws Exception {
    Set<String> keysInPrimaryFile = keysInFile(PRIMARY_LANGUAGE_FILE_PATH);

    ImmutableList<String> messageKeys =
        Arrays.stream(MessageKey.values()).map(MessageKey::getKeyName).collect(toImmutableList());

    assertThat(keysInPrimaryFile).containsExactlyInAnyOrderElementsOf(messageKeys);
  }

  private static Set<String> keysInFile(String filePath) throws Exception {
    return new TreeSet<>(entriesInFile(filePath).keySet());
  }

  /**
   * Returns the String-String entries present in a given messages file. The keys are returned in
   * sorted order, even though the {@link Properties} class reads them into a HashMap with no
   * ordering guarantees (regardless of the ordering of the language files themselves).
   */
  private static Map<String, String> entriesInFile(String filePath) throws IOException {
    try (InputStream input = new FileInputStream(filePath);
        InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {

      Properties prop = new Properties();
      prop.load(reader);

      return prop.entrySet().stream()
          .collect(
              Collectors.toMap(
                  entry -> (String) entry.getKey(),
                  entry -> (String) entry.getValue(),
                  (v1, v2) -> v1,
                  TreeMap::new));
    }
  }

  // The file paths of all foreign language files.
  private static String[] foreignLanguageFiles() throws Exception {
    try (Stream<Path> stream = Files.list(Paths.get(PREFIX_PATH))) {
      return stream
          .filter(path -> path.getFileName().toString().matches("messages.*"))
          // Exclude primary language file.
          .filter(path -> !path.getFileName().toString().equals(PRIMARY_LANGUAGE_FILE_NAME))
          // Exclude messages.en-US, which should be empty
          .filter(path -> !path.getFileName().toString().equals(EN_US_LANGUAGE_FILE_NAME))
          .map(Path::toString)
          .toArray(String[]::new);
    }
  }

  private String errorMessage(
      Set<String> primaryLangKeys, Set<String> foreignLangKeys, String foreignLanguageFile) {

    Set<String> keysInForeignLangFileCopy = new TreeSet<>(foreignLangKeys);
    keysInForeignLangFileCopy.removeAll(primaryLangKeys);
    if (!keysInForeignLangFileCopy.isEmpty()) {
      return String.format(
          "%s found in %s file but not in primary language file. Add these keys to primary"
              + " language file to resolve this issue.",
          keysInForeignLangFileCopy, foreignLanguageFile);
    }

    return "No fail message available";
  }
}
