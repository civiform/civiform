package controllers.dev.seeding;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.startsWith;

import com.google.common.collect.ImmutableMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import models.CategoryModel;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;
import play.i18n.Lang;

/**
 * Parses the program translation files to create {@code CategoryModel}s used to seed the
 * database with pre-defined categories.
 */
public final class CategoryTranslationFileParser {

  public static final String CATEGORY_TRANSLATIONS_DIRECTORY = "conf/i18n/";
  private final Environment environment;
  private Map<String, String> childcareMap = new HashMap<>();
  private Map<String, String> economicMap = new HashMap<>();
  private Map<String, String> educationMap = new HashMap<>();
  private Map<String, String> employmentMap = new HashMap<>();
  private Map<String, String> foodMap = new HashMap<>();
  private Map<String, String> generalMap = new HashMap<>();
  private Map<String, String> healthcareMap = new HashMap<>();
  private Map<String, String> housingMap = new HashMap<>();
  private Map<String, String> internetMap = new HashMap<>();
  private Map<String, String> trainingMap = new HashMap<>();
  private Map<String, String> transportationMap = new HashMap<>();
  private Map<String, String> utilitiesMap = new HashMap<>();
  private static final Logger logger = LoggerFactory.getLogger(CategoryTranslationFileParser.class);

  public CategoryTranslationFileParser(Environment environment) {
    this.environment = checkNotNull(environment);
  }

  public List<CategoryModel> createCategoryModelList() {
    parseCategoryTranslationFiles();

    List<CategoryModel> categoryModels = new ArrayList<>();

    List<Map<String, String>> categoryMaps =
        List.of(
            childcareMap,
            economicMap,
            educationMap,
            employmentMap,
            foodMap,
            generalMap,
            healthcareMap,
            housingMap,
            internetMap,
            trainingMap,
            transportationMap,
            utilitiesMap);

    categoryMaps.stream()
        .filter(map -> !map.isEmpty())
        .map(this::createCategoryModelFromCategoryMap)
        .forEach(categoryModels::add);

    return categoryModels;
  }

  private void parseCategoryTranslationFiles() {
    File directory = environment.getFile(CATEGORY_TRANSLATIONS_DIRECTORY);

    if (!directory.exists()) {
      logger.error("Directory does not exist: " + CATEGORY_TRANSLATIONS_DIRECTORY);
      return;
    }

    if (!directory.isDirectory()) {
      logger.error("File is not a directory: " + CATEGORY_TRANSLATIONS_DIRECTORY);
      return;
    }

    File[] files;
    try {
      files = directory.listFiles();
      if (files == null) {
        logger.error(
            "Unable to list files in directory: "
                + CATEGORY_TRANSLATIONS_DIRECTORY
                + ".  "
                + "There may have been an I/O error.");
        return;
      }
      for (File file : files) {
        // We should only parse messages files. We skip the english file, since that is blank.
        if (!file.getName().startsWith("messages") || file.getName().equals("messages.en-US")) {
          continue;
        }
        String fileLanguage = file.getName().equals("messages") ? "en-US" : FilenameUtils.getExtension(file.getName());
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(file.getPath()), UTF_8)) {

          Properties prop = new Properties();
          prop.load(reader);

          prop.entrySet()
              .forEach(
                  entry -> {
                    // Ignore any non-category strings
                    if (!entry.getKey().toString().startsWith("category")) {
                      return;
                    }
                    switch ((String) entry.getKey()) {
                      case "category.tag.childcare":
                        childcareMap.put(fileLanguage, (String) entry.getValue());
                        break;
                      case "category.tag.economic":
                        economicMap.put(fileLanguage, (String) entry.getValue());
                        break;
                      case "category.tag.education":
                        educationMap.put(fileLanguage, (String) entry.getValue());
                        break;
                      case "category.tag.employment":
                        employmentMap.put(fileLanguage, (String) entry.getValue());
                        break;
                      case "category.tag.food":
                        foodMap.put(fileLanguage, (String) entry.getValue());
                        break;
                      case "category.tag.general":
                        generalMap.put(fileLanguage, (String) entry.getValue());
                        break;
                      case "category.tag.healthcare":
                        healthcareMap.put(fileLanguage, (String) entry.getValue());
                        break;
                      case "category.tag.housing":
                        housingMap.put(fileLanguage, (String) entry.getValue());
                        break;
                      case "category.tag.internet":
                        internetMap.put(fileLanguage, (String) entry.getValue());
                        break;
                      case "category.tag.training":
                        trainingMap.put(fileLanguage, (String) entry.getValue());
                        break;
                      case "category.tag.transportation":
                        transportationMap.put(fileLanguage, (String) entry.getValue());
                        break;
                      case "category.tag.utilities":
                        utilitiesMap.put(fileLanguage, (String) entry.getValue());
                        break;
                      default:
                        logger.error("Unknown category: " + entry.getKey());
                    }
                  });

        } catch (FileNotFoundException e) {
          logger.error("File not found: " + file.getName(), e);
        } catch (IOException e) {
          logger.error("Error reading file: " + file.getName(), e);
        }
      }
    } catch (SecurityException e) {
      logger.error(
          "There was a security exception listing files in directory: "
              + CATEGORY_TRANSLATIONS_DIRECTORY,
          e);
    }
  }

  private CategoryModel createCategoryModelFromCategoryMap(Map<String, String> categoryMap) {
    ImmutableMap.Builder<Locale, String> categoryBuilder = ImmutableMap.builder();
    for (String key : categoryMap.keySet()) {
      categoryBuilder.put(Lang.forCode(key).toLocale(), categoryMap.get(key));
    }
    return new CategoryModel(categoryBuilder.build());
  }
}
