package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import services.applicant.AnswerData;
import services.question.types.QuestionType;
import services.applicant.question.MapQuestion;
import repository.GeoJsonDataRepository;
import models.GeoJsonDataModel;
import services.geojson.FeatureCollection;
import services.question.types.MapQuestionDefinition;
import java.util.Optional;

// Wrapper for AnswerData for ease of rendering in Thymeleaf.
// It's safer to process data in Java than at runtime in Thymeleaf.
public class NorthStarAnswerData implements Comparable<NorthStarAnswerData> {
  private final AnswerData answerData;
  private final long applicantId;
  private final GeoJsonDataRepository geoJsonDataRepository;

  public NorthStarAnswerData(AnswerData data, long applicantId, GeoJsonDataRepository geoJsonDataRepository) {
    this.answerData = checkNotNull(data);
    this.applicantId = applicantId;
    this.geoJsonDataRepository = checkNotNull(geoJsonDataRepository);
  }

  public String blockId() {
    return answerData.blockId();
  }

  public int questionIndex() {
    return answerData.questionIndex();
  }

  public String questionHtml(String ariaLabelForNewTabs) {
    return answerData.applicantQuestion().getFormattedQuestionText(ariaLabelForNewTabs);
  }

  public ImmutableList<String> multilineAnswerText() {
    String defaultAnswerString =
        answerData.applicantQuestion().getQuestion().getDefaultAnswerString();
    boolean hasAnswerText =
        !answerData.answerText().isBlank() && !answerData.answerText().equals(defaultAnswerString);
    boolean isAnswered = answerData.isAnswered() || hasAnswerText;
    boolean isFileUploadQuestion =
        answerData.questionDefinition().getQuestionType() == QuestionType.FILEUPLOAD;
    boolean isMapQuestion =
        answerData.questionDefinition().getQuestionType() == QuestionType.MAP;
    boolean hasFiles = !answerData.encodedFileKeys().isEmpty();

    if (isFileUploadQuestion && hasFiles) {
      // TODO(#8985): Allow user to download files on this page
      return fileNames();
    } else if (isMapQuestion && isAnswered) {
      return getMapDisplayText();
    } else if (isAnswered) {
      return ImmutableList.of(answerData.answerText());
    } else {
      return ImmutableList.of(defaultAnswerString);
    }
  }

  /**
   * Returns display text for MAP questions, converting IDs to names when possible.
   *
   * @return A list of location display names
   */
  private ImmutableList<String> getMapDisplayText() {
    MapQuestion mapQuestion = answerData.applicantQuestion().createMapQuestion();
    ImmutableList<String> selectedIds = mapQuestion.getSelectedLocationIds().orElse(ImmutableList.of());
    
    if (selectedIds.isEmpty()) {
      return ImmutableList.of("No locations selected");
    }
    
    // Get GeoJSON data for this MAP question to convert IDs to names
    try {
      MapQuestionDefinition mapQuestionDef = (MapQuestionDefinition) answerData.questionDefinition();
      String geoJsonEndpoint = mapQuestionDef.getMapValidationPredicates().geoJsonEndpoint();
      
      Optional<GeoJsonDataModel> geoJsonData = geoJsonDataRepository
          .getMostRecentGeoJsonDataRowForEndpoint(geoJsonEndpoint)
          .toCompletableFuture()
          .join();
          
      if (geoJsonData.isPresent()) {
        FeatureCollection featureCollection = geoJsonData.get().getGeoJson();
        return selectedIds.stream()
            .map(id -> mapQuestion.getLocationNameById(id, featureCollection))
            .collect(ImmutableList.toImmutableList());
      }
    } catch (RuntimeException e) {
      // Fallback to IDs if anything goes wrong
      System.err.println("Failed to convert MAP question IDs to names: " + e.getMessage());
    }
    
    // Fallback: show formatted IDs if GeoJSON lookup fails
    return selectedIds.stream()
        .map(id -> "Location: " + id.substring(0, Math.min(id.length(), 8)) + "...")
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Assumes this question is a file upload question.
   *
   * @return A list of file names (may be empty)
   */
  private ImmutableList<String> fileNames() {
    ArrayList<String> fileNames = new ArrayList<String>();
    for (int i = 0; i < answerData.encodedFileKeys().size(); i++) {
      String fileName = answerData.fileNames().get(i);
      fileNames.add(fileName);
    }
    return ImmutableList.copyOf(fileNames);
  }

  public ImmutableList<String> urls() {
    ArrayList<String> urls = new ArrayList<String>();

    AnswerData data = this.answerData;
    if (!data.encodedFileKeys().isEmpty()) {
      for (int i = 0; i < data.encodedFileKeys().size(); i++) {
        String encodedFileKey = data.encodedFileKeys().get(i);
        String fileUrl = controllers.routes.FileController.show(applicantId, encodedFileKey).url();
        urls.add(fileUrl);
      }
    } else if (data.encodedFileKey().isPresent()) {
      // TODO(#7493): When single encoded file key is deprecated, delete this branch
      String encodedFileKey = data.encodedFileKey().get();
      String fileUrl = controllers.routes.FileController.show(applicantId, encodedFileKey).url();
      urls.add(fileUrl);
    }
    return ImmutableList.copyOf(urls);
  }

  @Override
  public int compareTo(NorthStarAnswerData other) {
    return Integer.compare(this.questionIndex(), other.questionIndex());
  }
}
