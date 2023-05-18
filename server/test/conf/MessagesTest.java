package conf;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

  // A set of keys that are present in the primary language file, but for which
  // we do *not* expect translations to be present. This is useful for keys that
  // are checked into the primary language file but for which
  // we are waiting for translations for.
  private static final ImmutableSet<String> IGNORE_LIST =
      ImmutableSet.of(
          // TODO(#4640): Translations Batch 6 - Insert items awaiting translations here
          "button.continueToApplication", "toast.sessionEnded", "content.initialLoginModalPrompt");

  // Slanted quotes like “ and ’ show up as â in tests; this is a common error when
  // copy-pasting text. Check for this error in all messages files.
  private static final Set<String> PROHIBITED_CHARACTERS = Set.of("â");

  @Test
  public void ignoreListIsUpToDate() throws Exception {
    // Assert that we don't have keys in the ignore list that aren't in the
    // primary language file.
    assertThat(keysInFile(PRIMARY_LANGUAGE_FILE_PATH)).containsAll(IGNORE_LIST);
  }

  @Test
  @Parameters(method = "otherLanguageFiles")
  public void messages_keysInPrimaryFileInAllOtherFiles(String otherLanguageFile) throws Exception {
    TreeSet<String> keysInPrimaryFile = keysInFile(PRIMARY_LANGUAGE_FILE_PATH);

    // Pretend that the keys in IGNORE_LIST are not in the primary message
    // file. These may be keys for features in development, for example. Keys
    // should only be in IGNORE_LIST temporarily, until translations come in
    // and are merged.
    keysInPrimaryFile.removeAll(IGNORE_LIST);

    TreeSet<String> keysInForeignLangFile = keysInFile(otherLanguageFile);

    // Checks that the language file contains exactly the same message keys as the
    // primary language file.
    assertThat(keysInPrimaryFile)
        .withFailMessage(errorMessage(keysInPrimaryFile, keysInForeignLangFile, otherLanguageFile))
        .containsExactlyElementsOf(keysInForeignLangFile);
  }

  @Test
  public void messages_MessagesEnUs_isEmpty() throws Exception {
    TreeMap<String, String> entriesInEnUsLanguageFile = entriesInFile(EN_US_LANGUAGE_FILE_PATH);

    // messages.en-US should be empty and allow all keys to fall back to the messages file.
    assertThat(entriesInEnUsLanguageFile).isEmpty();
  }

  @Test
  public void messages_primaryFile_containsNoProhibitedCharacters() throws Exception {
    TreeMap<String, String> entriesInPrimaryLanguageFile =
        entriesInFile(PRIMARY_LANGUAGE_FILE_PATH);

    assertThat(entriesInPrimaryLanguageFile)
        .withFailMessage("Prohibited characters found in primary language file..")
        .allSatisfy(
            (key, value) -> {
              assertThat(key).doesNotContain(PROHIBITED_CHARACTERS);
              assertThat(value).doesNotContain(PROHIBITED_CHARACTERS);
            });
  }

  @Test
  @Parameters(method = "otherLanguageFiles")
  public void messages_otherLanguageFiles_containNoProhibitedCharacters(String otherLanguageFile)
      throws Exception {
    TreeMap<String, String> entriesInOtherLanguageFile = entriesInFile(otherLanguageFile);

    assertThat(entriesInOtherLanguageFile)
        .withFailMessage("Prohibited characters found in " + otherLanguageFile + ".")
        .allSatisfy(
            (key, value) -> {
              assertThat(key).doesNotContain(PROHIBITED_CHARACTERS);
              assertThat(value).doesNotContain(PROHIBITED_CHARACTERS);
            });
  }

  private static TreeSet<String> keysInFile(String filePath) throws Exception {
    return new TreeSet<>(entriesInFile(filePath).keySet());
  }

  /**
   * Returns the String-String entries present in a given messages file. The keys are returned in
   * sorted order, even though the {@link Properties} class reads them into a HashMap with no
   * ordering guarantees (regardless of the ordering of the language files themselves).
   */
  private static TreeMap<String, String> entriesInFile(String filePath) throws Exception {
    InputStream input = new FileInputStream(filePath);

    Properties prop = new Properties();
    prop.load(input);

    return prop.entrySet().stream()
        .collect(
            Collectors.toMap(
                entry -> (String) entry.getKey(),
                entry -> (String) entry.getValue(),
                (v1, v2) -> v1,
                TreeMap::new));
  }

  // The file paths of all non-primary language files, including `en-US`.
  private static String[] otherLanguageFiles() throws Exception {
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
      TreeSet<String> primaryLangKeys, TreeSet<String> foreignLangKeys, String otherLanguageFile) {
    StringBuilder stringBuilder = new StringBuilder();

    TreeSet<String> keysInPrimaryFileCopy = new TreeSet<>(primaryLangKeys);
    keysInPrimaryFileCopy.removeAll(foreignLangKeys);
    if (!keysInPrimaryFileCopy.isEmpty()) {
      stringBuilder.append(
          String.format(
              "%s found in primary language file but not in %s. Add these keys to %s or to the"
                  + " ignore list in %s to resolve this issue.",
              keysInPrimaryFileCopy, otherLanguageFile, otherLanguageFile, getClass().getName()));
    }

    stringBuilder.append("\n\n");

    TreeSet<String> keysInForeignLangFileCopy = new TreeSet<>(foreignLangKeys);
    keysInForeignLangFileCopy.removeAll(primaryLangKeys);
    if (!keysInForeignLangFileCopy.isEmpty()) {
      stringBuilder.append(
          String.format(
              "%s found in %s file but not in primary language file. Add these keys to primary"
                  + " language file or to the ignore list in %s to resolve this issue.",
              keysInForeignLangFileCopy, otherLanguageFile, getClass().getName()));
    }

    return stringBuilder.toString();
  }
}
