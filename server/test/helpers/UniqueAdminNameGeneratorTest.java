package helpers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import repository.QuestionRepository;
import repository.ResetPostgres;

public class UniqueAdminNameGeneratorTest extends ResetPostgres {
  private final UniqueAdminNameGenerator generator =
      new UniqueAdminNameGenerator(instanceOf(QuestionRepository.class));

  @Test
  public void generate_generatesCorrectAdminNames() {
    resourceCreator.insertQuestion("name-question");
    resourceCreator.insertQuestion("name-question -_- a");
    resourceCreator.insertQuestion("name-question -_- b");

    String newAdminName = generator.generate("name-question", new ArrayList<>());
    assertThat(newAdminName).isEqualTo("name-question -_- c");
    // Even though there is no existing match, this method should still return a unique name, since
    // it assumed that the caller has checked for an existing match before calling.
    String unmatchedAdminName = generator.generate("admin-name-unmatched", new ArrayList<>());
    assertThat(unmatchedAdminName).isEqualTo("admin-name-unmatched -_- a");
  }

  @Test
  public void generate_generatesCorrectAdminNamesForAdminNamesWithSuffixes() {
    resourceCreator.insertQuestion("name-question");
    resourceCreator.insertQuestion("name-question -_- a");
    resourceCreator.insertQuestion("name-question -_- b");

    String newAdminName = generator.generate("name-question -_- a", new ArrayList<>());
    assertThat(newAdminName).isEqualTo("name-question -_- c");
  }

  @Test
  public void generate_generatesCorrectAdminNamesWhenAlreadyGeneratedNameMightConflict() {
    List<String> namesSoFar = List.of("name-question -_- a", "name-question -_- b");

    String newAdminName = generator.generate("name-question -_- a", namesSoFar);
    assertThat(newAdminName).isEqualTo("name-question -_- c");
  }

  @Test
  public void convertNumberToSuffix_generatesCorrectSuffix() {
    assertThat(UniqueAdminNameGenerator.convertNumberToSuffix(10)).isEqualTo("j");
    assertThat(UniqueAdminNameGenerator.convertNumberToSuffix(26)).isEqualTo("z");
    assertThat(UniqueAdminNameGenerator.convertNumberToSuffix(26 + 2)).isEqualTo("ab");
    assertThat(UniqueAdminNameGenerator.convertNumberToSuffix(26 + 26)).isEqualTo("az");
    assertThat(UniqueAdminNameGenerator.convertNumberToSuffix(26 + 26 + 26)).isEqualTo("bz");
    assertThat(UniqueAdminNameGenerator.convertNumberToSuffix(26 * 26 + 11)).isEqualTo("zk");
    assertThat(UniqueAdminNameGenerator.convertNumberToSuffix(27 * 26 + 11)).isEqualTo("aak");
  }

  @Test
  public void nameHasConflict_happyPath_noConflict() {
    resourceCreator.insertQuestion("name-question");
    assertThat(generator.nameHasConflict("new-name-question", new ArrayList<>())).isEqualTo(false);
  }

  @Test
  public void nameHasConflict_existingQuestionContainsConflict_findsConflict() {
    resourceCreator.insertQuestion("name-question");
    assertThat(generator.nameHasConflict("name-question", new ArrayList<>())).isEqualTo(true);
  }

  @Test
  public void nameHasConflict_newNamesSoFarContainsConflict_findsConflict() {
    ArrayList<String> newNamesSoFar = new ArrayList<>();
    newNamesSoFar.add("name-question");
    assertThat(generator.nameHasConflict("name-question", newNamesSoFar)).isEqualTo(true);
  }
}
