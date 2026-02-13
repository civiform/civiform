package mapping.admin.docs;

import static services.export.JsonPrettifier.asPrettyJsonString;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import play.mvc.Http;
import repository.ExportServiceRepository;
import services.TranslationNotFoundException;
import services.export.ProgramJsonSampler;
import services.program.ProgramDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import views.admin.docs.ApiDocsPageViewModel;
import views.admin.docs.ApiDocsPageViewModel.ProgramSlugOption;
import views.admin.docs.ApiDocsPageViewModel.QuestionDoc;

/** Maps program definition and slug data to the ApiDocsPageViewModel. */
public final class ApiDocsPageMapper {

  public ApiDocsPageViewModel map(
      Http.Request request,
      String selectedProgramSlug,
      Optional<ProgramDefinition> programDefinition,
      ImmutableSet<String> allProgramSlugs,
      boolean isActiveVersion,
      ProgramJsonSampler programJsonSampler,
      ExportServiceRepository exportServiceRepository) {

    List<ProgramSlugOption> slugOptions =
        allProgramSlugs.stream()
            .sorted()
            .map(
                slug ->
                    ProgramSlugOption.builder()
                        .slug(slug)
                        .selected(slug.equals(selectedProgramSlug))
                        .build())
            .collect(Collectors.toList());

    List<QuestionDoc> questionDocs = List.of();
    String jsonPreview = "";

    if (programDefinition.isPresent()) {
      ProgramDefinition progDef = programDefinition.get();

      jsonPreview = asPrettyJsonString(programJsonSampler.getSampleJson(progDef));

      questionDocs =
          progDef
              .streamQuestionDefinitions()
              .sorted(Comparator.comparing(QuestionDefinition::getQuestionNameKey))
              .map(qd -> mapQuestionDoc(qd, exportServiceRepository))
              .collect(Collectors.toList());
    }

    String apiDocsLink = controllers.docs.routes.ApiDocsController.index().absoluteURL(request);

    return ApiDocsPageViewModel.builder()
        .selectedProgramSlug(selectedProgramSlug)
        .activeVersion(isActiveVersion)
        .programSlugs(slugOptions)
        .programFound(programDefinition.isPresent())
        .questions(questionDocs)
        .jsonPreview(jsonPreview)
        .apiDocsLink(apiDocsLink)
        .build();
  }

  private QuestionDoc mapQuestionDoc(
      QuestionDefinition qd, ExportServiceRepository exportServiceRepository) {
    String questionText = "";
    try {
      questionText = qd.getQuestionText().get(Locale.US);
    } catch (TranslationNotFoundException e) {
      // leave empty
    }

    boolean isMultiOption = qd.getQuestionType().isMultiOptionType();
    ImmutableList<String> currentOptions = ImmutableList.of();
    ImmutableList<String> allHistoricOptions = ImmutableList.of();

    if (isMultiOption) {
      MultiOptionQuestionDefinition multiOptionQD = (MultiOptionQuestionDefinition) qd;
      currentOptions = multiOptionQD.getDisplayableOptionAdminNames();
      allHistoricOptions =
          exportServiceRepository.getAllHistoricMultiOptionAdminNames(multiOptionQD);
    }

    return QuestionDoc.builder()
        .name(qd.getName())
        .questionNameKey(qd.getQuestionNameKey().toLowerCase(Locale.US))
        .type(qd.getQuestionType().toString())
        .questionText(questionText)
        .multiOption(isMultiOption)
        .currentOptions(currentOptions)
        .allHistoricOptions(allHistoricOptions)
        .build();
  }
}
