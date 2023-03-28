package conf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.Test;

public class MessagesTest {

  // The file path of the primary language file.
  String PRIMARY_LANGUAGE_FILE = "conf/messages";

  // The file paths of all non-primary language files, including `en-US`.
  private static final Set<String> OTHER_LANGUAGE_FILES =
      Set.of(
          "conf/messages.am",
          "conf/messages.en-US",
          "conf/messages.es-US",
          "conf/messages.ko",
          "conf/messages.so",
          "conf/messages.tl",
          "conf/messages.vi",
          "conf/messages.zh-TW");

  // A set of keys that are present in the primary language file, but for which
  // we do *not* expect translations to be present. This is useful for keys that
  // are checked into the primary language file but for which
  // we are waiting for translations for.
  //
  // TODO(#4505): remove keys that are checked in with 3/16 batch.
  private static final Set<String> IGNORE_LIST =
      Set.of(
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
  public void testMessages() throws Exception {
    TreeSet<String> keysInPrimaryFile = keysInFile(PRIMARY_LANGUAGE_FILE);
    // Pretend that the keys in IGNORE_LIST are not in the primary message
    // file. These may be keys for features in development, for example. Keys
    // should only be in IGNORE_LIST temporarily, until translations come in
    // and are merged.
    keysInPrimaryFile.removeAll(IGNORE_LIST);

    for (String file : OTHER_LANGUAGE_FILES) {
      TreeSet<String> keysInForeignLangFile = keysInFile(file);

      // Checks that the language file contains exactly the same message keys as the
      // primary language file, *in the same order*.
      //
      // TODO(MichaelZetune): change containsSubsequence to containsExactlyElementsOf.
      //  Currently this is not possible, even with IGNORE_LIST, because there are keys
      //  that are present in some but not all of the non-primary language files.
      assertThat(keysInForeignLangFile).containsSubsequence(keysInPrimaryFile);
    }
  }

  /**
   * Returns the set of String keys present in a given messages file. The keys are returned in
   * sorted order, even though the {@link Properties} class reads them into a HashMap with no
   * ordering guarantees (regardless of the ordering of the language files themselves).
   */
  private TreeSet<String> keysInFile(String filePath) throws Exception {
    InputStream input = new FileInputStream(filePath);

    Properties prop = new Properties();
    prop.load(input);

    return prop.keySet().stream()
        .map(Object::toString)
        .collect(Collectors.toCollection(TreeSet::new));
  }
}
