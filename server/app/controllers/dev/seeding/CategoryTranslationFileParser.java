package controllers.dev.seeding;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

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
 * Parses the program category translation files to create {@code CategoryModel}s used to seed the
 * database with pre-defined categories.
 */
public final class CategoryTranslationFileParser {

  public static final String CATEGORY_TRANSLATIONS_DIRECTORY = "conf/i18n/categories/";
  private final Environment environment;
  private Map<String, String> childcareMap = new HashMap<>();
  private Map<String, String> economicMap = new HashMap<>();
  private Map<String, String> educationMap = new HashMap<>();
  private Map<String, String> foodMap = new HashMap<>();
  private Map<String, String> generalMap = new HashMap<>();
  private Map<String, String> healthcareMap = new HashMap<>();
  private Map<String, String> housingMap = new HashMap<>();
  private Map<String, String> internetMap = new HashMap<>();
  private Map<String, String> transportationMap = new HashMap<>();
  private Map<String, String> utilitiesMap = new HashMap<>();
  private static final Logger logger = LoggerFactory.getLogger(CategoryTranslationFileParser.class);

  public CategoryTranslationFileParser(Environment environment) {
    this.environment = checkNotNull(environment);
  }

  public List<CategoryModel> createCategoryModelList() {
    parseCategoryTranslationFiles();

    List<CategoryModel> categoryModels = new ArrayList<>();
    categoryModels.add(createCategoryModelFromCategoryMap(childcareMap));
    categoryModels.add(createCategoryModelFromCategoryMap(economicMap));
    categoryModels.add(createCategoryModelFromCategoryMap(educationMap));
    categoryModels.add(createCategoryModelFromCategoryMap(foodMap));
    categoryModels.add(createCategoryModelFromCategoryMap(generalMap));
    categoryModels.add(createCategoryModelFromCategoryMap(healthcareMap));
    categoryModels.add(createCategoryModelFromCategoryMap(housingMap));
    categoryModels.add(createCategoryModelFromCategoryMap(internetMap));
    categoryModels.add(createCategoryModelFromCategoryMap(transportationMap));
    categoryModels.add(createCategoryModelFromCategoryMap(utilitiesMap));
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
    File[] files = directory.listFiles();
    for (File file : files) {
      String fileLanguage = FilenameUtils.getExtension(file.getName());
      try (BufferedReader reader = Files.newBufferedReader(Paths.get(file.getPath()), UTF_8)) {

        Properties prop = new Properties();
        prop.load(reader);

        prop.entrySet()
            .forEach(
                entry -> {
                  switch ((String) entry.getKey()) {
                    case "tag.childcare":
                      childcareMap.put(fileLanguage, (String) entry.getValue());
                      break;
                    case "tag.economic":
                      economicMap.put(fileLanguage, (String) entry.getValue());
                      break;
                    case "tag.education":
                      educationMap.put(fileLanguage, (String) entry.getValue());
                      break;
                    case "tag.food":
                      foodMap.put(fileLanguage, (String) entry.getValue());
                      break;
                    case "tag.general":
                      generalMap.put(fileLanguage, (String) entry.getValue());
                      break;
                    case "tag.healthcare":
                      healthcareMap.put(fileLanguage, (String) entry.getValue());
                      break;
                    case "tag.housing":
                      housingMap.put(fileLanguage, (String) entry.getValue());
                      break;
                    case "tag.internet":
                      internetMap.put(fileLanguage, (String) entry.getValue());
                      break;
                    case "tag.transportation":
                      transportationMap.put(fileLanguage, (String) entry.getValue());
                      break;
                    case "tag.utilities":
                      utilitiesMap.put(fileLanguage, (String) entry.getValue());
                      break;
                    default:
                      logger.error("Unknown category: " + entry.getKey());
                  }
                });

      } catch (FileNotFoundException e) {
        logger.error("File not found: " + file.getName());
      } catch (IOException e) {
        logger.error("Error reading file: " + file.getName(), e);
      }
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
