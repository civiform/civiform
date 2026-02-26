package helpers;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import repository.QuestionRepository;
import services.LocalizedStrings;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.TextQuestionDefinition;

/** Class to generate a new, unique admin name for program questions. */
public final class UniqueAdminNameGenerator {
  private static final Pattern SUFFIX_PATTERN = Pattern.compile(" -_- [a-z]+$");
  private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";

  private final QuestionRepository questionRepository;

  @Inject
  public UniqueAdminNameGenerator(QuestionRepository questionRepository) {
    this.questionRepository = checkNotNull(questionRepository);
  }

  /**
   * Generate a new admin name for questions of the format "orginal admin name -_- a" where "a" is
   * the next consecutive letter such that we don't already have a question with that admin name
   * saved. For example if the admin name is "sample question" and we already have <br>
   * "sample question -_- a" and "sample question -_- b" saved in the db, the generated name will be
   * "sample question -_- c".
   *
   * <p>Note: always generates a new name. Callers should ensure that the {@code originalName}
   * already exists before calling this method.
   */
  public String generate(String originalName, List<String> newNamesSoFar) {
    // If the question name contains a suffix of the form " -_- a" (for example "admin name -_- a"),
    // we want to strip off the " -_- n" to find the base name. This also allows us to correctly
    // increment the suffix of the base admin name so we don't end up with admin names like "admin
    // name -_- a -_- a".
    Matcher matcher = SUFFIX_PATTERN.matcher(originalName);
    String newNameBase = originalName;
    if (matcher.find()) {
      newNameBase = originalName.substring(0, matcher.start());
    }

    int extension = 0;
    String newName = "";
    do {
      extension++;

      // We use `-_-` as the delimiter because it's unlikely to already be used in a question with a
      // name like `name - parent`. It will transform to a key formatted like `%s__%s`
      newName = "%s -_- %s".formatted(newNameBase, convertNumberToSuffix(extension));
    } while (nameHasConflict(newName, newNamesSoFar));

    return newName;
  }

  /**
   * Checks whether {@code name} is contained in {@code newNamesSoFar}, or whether there is an
   * already existing question with the same admin name.
   */
  public boolean nameHasConflict(String name, List<String> newNamesSoFar) {
    // Check if any of the names we've already generated might conflict with this one.
    // We can compare raw names, rather than keys, because everything we generate
    // follows the same pattern and will reduce to keys in the same way.
    if (newNamesSoFar.contains(name)) {
      return true;
    }
    QuestionDefinition testQuestion =
        new TextQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
                .build());

    return questionRepository.findConflictingQuestion(testQuestion).isPresent();
  }

  /**
   * Convert a number to the equivalent "excel column name".
   *
   * <p>For example, 5 maps to "e", and 28 maps to "ab".
   *
   * @param num to convert
   * @return The "excel column name" form of the number
   */
  static String convertNumberToSuffix(int num) {
    String result = "";

    // Division algorithm to convert from base 10 to "base 26"
    int dividend = num; // 28
    while (dividend > 0) {
      // Subtract one so we're doing math with a zero-based index.
      // We need "a" to be 0, and "z" to be 25, so that 26 wraps around
      // to be "aa". "a" is "ten" in base 26.
      dividend = dividend - 1;
      int remainder = dividend % 26;
      result = ALPHABET.charAt(remainder) + result;
      dividend = dividend / 26;
    }

    return result;
  }
}
