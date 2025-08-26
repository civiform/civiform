package controllers.dev.seeding;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.inject.Inject;
import models.CategoryModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;
import play.i18n.Lang;
import play.i18n.Langs;

/**
 * Parses the program translation files to create {@code CategoryModel}s used to seed the database
 * with pre-defined categories.
 */
public final class CategoryTranslationFileParser {

  public static final String CATEGORY_TRANSLATIONS_DIRECTORY = "conf/i18n/";
  private static final String CATEGORY_PREFIX = "category.tag.";
  private static final Logger logger = LoggerFactory.getLogger(CategoryTranslationFileParser.class);

  private final Environment environment;
  private final Langs langs;

  @Inject
  public CategoryTranslationFileParser(Environment environment, Langs langs) {
    this.environment = checkNotNull(environment);
    this.langs = checkNotNull(langs);
  }

  public List<CategoryModel> createCategoryModelList() {
    return createCategoryModelList(this.environment);
  }

  public List<CategoryModel> createCategoryModelList(Environment env) {
    Map<String, Map<String, String>> categoryTranslations = new HashMap<>();
    ImmutableSet<String> configuredLanguages =
        langs.availables().stream().map(Lang::code).collect(ImmutableSet.toImmutableSet());

    if (configuredLanguages.isEmpty()) {
      logger.warn("No languages configured in play.i18n.langs");
      return ImmutableList.of();
    }

    File directory = env.getFile(CATEGORY_TRANSLATIONS_DIRECTORY);

    if (!directory.exists()) {
      logger.error("Directory does not exist: {}", CATEGORY_TRANSLATIONS_DIRECTORY);
      return ImmutableList.of();
    }

    if (!directory.isDirectory()) {
      logger.error("File is not a directory: {}", CATEGORY_TRANSLATIONS_DIRECTORY);
      return ImmutableList.of();
    }

    for (String langCode : configuredLanguages) {
      // Default messages file is for en-US, others have the language as extension
      String fileName = langCode.equals("en-US") ? "messages" : "messages." + langCode;
      File file = new File(directory, fileName);

      if (!file.exists()) {
        logger.warn("Message file not found for configured language {}: {}", langCode, fileName);
        continue;
      }

      try (BufferedReader reader = Files.newBufferedReader(Paths.get(file.getPath()), UTF_8)) {
        Properties prop = new Properties();
        prop.load(reader);

        prop.entrySet().stream()
            .filter(entry -> entry.getKey().toString().startsWith(CATEGORY_PREFIX))
            .forEach(
                entry -> {
                  String fullKey = entry.getKey().toString();
                  String categoryKey = fullKey.substring(CATEGORY_PREFIX.length());

                  categoryTranslations
                      .computeIfAbsent(categoryKey, k -> new HashMap<>())
                      .put(langCode, entry.getValue().toString());
                });

      } catch (IOException e) {
        logger.error("Error reading file: {}", fileName, e);
      }
    }

    return createCategoryModels(categoryTranslations, configuredLanguages);
  }

  private List<CategoryModel> createCategoryModels(
      Map<String, Map<String, String>> categoryTranslations,
      ImmutableSet<String> configuredLanguages) {
    // Find categories that exist in ALL configured languages and create models
    ImmutableList.Builder<CategoryModel> categoryModels = ImmutableList.builder();

    for (String categoryKey : categoryTranslations.keySet()) {
      Map<String, String> translations = categoryTranslations.get(categoryKey);

      if (translations.keySet().equals(configuredLanguages)) {
        ImmutableMap.Builder<Locale, String> categoryBuilder = ImmutableMap.builder();
        translations.forEach(
            (lang, translation) -> categoryBuilder.put(Lang.forCode(lang).toLocale(), translation));

        categoryModels.add(new CategoryModel(categoryBuilder.build()));
      } else {
        Set<String> missingLanguages = Sets.difference(configuredLanguages, translations.keySet());
        logger.info(
            "Excluding category '{}' - missing translations for: {}",
            categoryKey,
            missingLanguages);
      }
    }

    return categoryModels.build();
  }
}
