package conf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;

import com.google.common.collect.ImmutableSet;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

/**
 * Tests that the messages files are in sync. Reads in the keys from the primary language file,
 * `messages`, and ensures the keys are in sync with the `messages.*` files.
 */
public class MessagesTest {

  // The file path of the primary language file.
  private static final String PRIMARY_LANGUAGE_FILE = "conf/messages";

  // A set of keys that are present in the primary language file, but for which
  // we do *not* expect translations to be present. This is useful for keys that
  // are checked into the primary language file but for which
  // we are waiting for translations for.
  //
  // TODO(#4505): remove keys that are checked in with 3/16 batch.
  private static final ImmutableSet<String> IGNORE_LIST =
      ImmutableSet.of(
          "button.editCommonIntakeSr",
          "button.startHere",
          "button.startHereCommonIntakeSr",
          "button.continueCommonIntakeSr",
          "content.mustMeetRequirementsTi",
          "title.allProgramsSection",
          "title.commonIntakeSummary",
          "email.applicationUpdateSubject",
          "email.tiApplicationUpdateBody",
          "email.tiApplicationUpdateSubject",
          "title.findServicesSection");

  @Test
  public void ignoreListIsUpToDate() throws Exception {
    // Assert that we don't have keys in the ignore list that aren't in the
    // primary language file.
    assertThat(keysInFile(PRIMARY_LANGUAGE_FILE)).containsAll(IGNORE_LIST);
  }

  @Test
  public void messages_keysInPrimaryFileInAllOtherFiles() throws Exception {
    TreeSet<String> keysInPrimaryFile = keysInFile(PRIMARY_LANGUAGE_FILE);
    // Pretend that the keys in IGNORE_LIST are not in the primary message
    // file. These may be keys for features in development, for example. Keys
    // should only be in IGNORE_LIST temporarily, until translations come in
    // and are merged.
    keysInPrimaryFile.removeAll(IGNORE_LIST);

    for (String file : otherLanguageFiles()) {
      TreeSet<String> keysInForeignLangFile = keysInFile(file);

      // Checks that the language file contains exactly the same message keys as the
      // primary language file.
      //
      // TODO(#4520): Modify this to ensure the keys are the same, not just that all
      //  keys in primary file exist in foreign language files. Currently this is not possible,
      //  even with IGNORE_LIST, because there are keys that are present in some but not all
      //  of the non-primary language files.
      TreeSet<String> keysInPrimaryFileCopy = new TreeSet<>(keysInPrimaryFile);
      keysInPrimaryFileCopy.removeAll(keysInForeignLangFile);
      assertThat(keysInPrimaryFileCopy)
          .withFailMessage(
              "%s found in primary language file but not in %s. Add these keys to %s or to the"
                  + " ignore list in %s to resolve this issue.",
              keysInPrimaryFileCopy, file, file, getClass().getName())
          .isEmpty();
    }
  }

  /**
   * Returns the set of String keys present in a given messages file. The keys are returned in
   * sorted order, even though the {@link Properties} class reads them into a HashMap with no
   * ordering guarantees (regardless of the ordering of the language files themselves).
   */
  private static TreeSet<String> keysInFile(String filePath) throws Exception {
    InputStream input = new FileInputStream(filePath);

    Properties prop = new Properties();
    prop.load(input);

    return prop.keySet().stream()
        .map(Object::toString)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  // The file paths of all non-primary language files, including `en-US`.
  private static Set<String> otherLanguageFiles() throws Exception {
    try (Stream<Path> stream = Files.list(Paths.get("conf/"))) {
      return stream
          .filter(path -> path.getFileName().toString().matches("messages.*"))
          // Exclude primary language file.
          .filter(path -> !path.getFileName().toString().equals("messages"))
          .map(Path::toString)
          .collect(Collectors.toSet());
    }
  }
}
