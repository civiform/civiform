package controllers.dev.seeding;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import models.CategoryModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.Lang;

/**
 * Parses the program category translation files to create {@code CategoryModel}s used to seed the
 * database with pre-defined categories.
 */
public final class CategoryTranslationFileParser {

  public static final String CATEGORY_TRANSLATIONS_DIRECTORY = "conf/i18n/categories/";
  public static final String CATEGORY_TRANSLATIONS_FILE_EXTENSION = ".txt";

  /** Maps each English category tag to its translation in Amharic. */
  public static final ImmutableMap<String, String> AM_CATEGORY_TRANSLATIONS =
      parseFileForCategoryTranslations(
          CATEGORY_TRANSLATIONS_DIRECTORY
              + "am_program_categories"
              + CATEGORY_TRANSLATIONS_FILE_EXTENSION);

  /** Maps each English category tag to its translation in English. */
  public static final ImmutableMap<String, String> EN_CATEGORY_TRANSLATIONS =
      parseFileForCategoryTranslations(
          CATEGORY_TRANSLATIONS_DIRECTORY
              + "en-US_program_categories"
              + CATEGORY_TRANSLATIONS_FILE_EXTENSION);

  /** Maps each English category tag to its translation in Spanish. */
  public static final ImmutableMap<String, String> ES_CATEGORY_TRANSLATIONS =
      parseFileForCategoryTranslations(
          CATEGORY_TRANSLATIONS_DIRECTORY
              + "es-US_program_categories"
              + CATEGORY_TRANSLATIONS_FILE_EXTENSION);

  /** Maps each English category tag to its translation in Korean. */
  public static final ImmutableMap<String, String> KO_CATEGORY_TRANSLATIONS =
      parseFileForCategoryTranslations(
          CATEGORY_TRANSLATIONS_DIRECTORY
              + "ko_program_categories"
              + CATEGORY_TRANSLATIONS_FILE_EXTENSION);

  /** Maps each English category tag to its translation in Lao. */
  public static final ImmutableMap<String, String> LO_CATEGORY_TRANSLATIONS =
      parseFileForCategoryTranslations(
          CATEGORY_TRANSLATIONS_DIRECTORY
              + "lo_program_categories"
              + CATEGORY_TRANSLATIONS_FILE_EXTENSION);

  /** Maps each English category tag to its translation in Somali. */
  public static final ImmutableMap<String, String> SO_CATEGORY_TRANSLATIONS =
      parseFileForCategoryTranslations(
          CATEGORY_TRANSLATIONS_DIRECTORY
              + "so_program_categories"
              + CATEGORY_TRANSLATIONS_FILE_EXTENSION);

  /** Maps each English category tag to its translation in Tagalog. */
  public static final ImmutableMap<String, String> TL_CATEGORY_TRANSLATIONS =
      parseFileForCategoryTranslations(
          CATEGORY_TRANSLATIONS_DIRECTORY
              + "tl_program_categories"
              + CATEGORY_TRANSLATIONS_FILE_EXTENSION);

  /** Maps each English category tag to its translation in Vietnamese. */
  public static final ImmutableMap<String, String> VI_CATEGORY_TRANSLATIONS =
      parseFileForCategoryTranslations(
          CATEGORY_TRANSLATIONS_DIRECTORY
              + "vi_program_categories"
              + CATEGORY_TRANSLATIONS_FILE_EXTENSION);

  /** Maps each English category tag to its translation in Chinese. */
  public static final ImmutableMap<String, String> ZH_CATEGORY_TRANSLATIONS =
      parseFileForCategoryTranslations(
          CATEGORY_TRANSLATIONS_DIRECTORY
              + "zh-TW_program_categories"
              + CATEGORY_TRANSLATIONS_FILE_EXTENSION);

  public static final ImmutableList<String> PROGRAM_CATEGORY_NAMES =
      getCategoryNamesFromTranslationMap(AM_CATEGORY_TRANSLATIONS);

  private static final Logger logger = LoggerFactory.getLogger(CategoryTranslationFileParser.class);

  public static CategoryModel createCategoryModelFromTranslationsMap(String category) {
    String key = "tag." + category.toLowerCase(Locale.ROOT);
    return new CategoryModel(
        ImmutableMap.of(
            Lang.forCode("am").toLocale(),
            AM_CATEGORY_TRANSLATIONS.get(key),
            Lang.forCode("ko").toLocale(),
            KO_CATEGORY_TRANSLATIONS.get(key),
            Lang.forCode("so").toLocale(),
            SO_CATEGORY_TRANSLATIONS.get(key),
            Lang.forCode("lo").toLocale(),
            LO_CATEGORY_TRANSLATIONS.get(key),
            Lang.forCode("tl").toLocale(),
            TL_CATEGORY_TRANSLATIONS.get(key),
            Lang.forCode("vi").toLocale(),
            VI_CATEGORY_TRANSLATIONS.get(key),
            Lang.forCode("en-US").toLocale(),
            EN_CATEGORY_TRANSLATIONS.get(key),
            Lang.forCode("es-US").toLocale(),
            ES_CATEGORY_TRANSLATIONS.get(key),
            Lang.forCode("zh-TW").toLocale(),
            ZH_CATEGORY_TRANSLATIONS.get(key)));
  }

  private static ImmutableMap<String, String> parseFileForCategoryTranslations(String fileName) {
    ImmutableMap.Builder<String, String> mapBuilder = ImmutableMap.builder();

    try (BufferedReader reader = Files.newBufferedReader(Paths.get(fileName), UTF_8)) {
      String line;
      while ((line = reader.readLine()) != null) {
        String[] parts = line.split("=", 2);
        if (parts.length >= 2) {
          String key = parts[0];
          String value = parts[1];
          mapBuilder.put(key, value);
        }
      }
    } catch (FileNotFoundException e) {
      logger.error("File not found: " + fileName);
    } catch (IOException e) {
      logger.error("Error reading file: " + fileName, e);
    }

    return mapBuilder.build();
  }

  private static ImmutableList<String> getCategoryNamesFromTranslationMap(
      ImmutableMap<String, String> translationMap) {
    ImmutableList.Builder<String> categoryNamesBuilder = ImmutableList.builder();
    for (String key : translationMap.keySet()) {
      categoryNamesBuilder.add(key.substring(4)); // Remove "tag." prefix
    }
    return categoryNamesBuilder.build();
  }
}
