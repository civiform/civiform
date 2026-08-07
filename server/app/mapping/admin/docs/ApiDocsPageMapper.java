package mapping.admin.docs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import models.LifecycleStage;
import play.mvc.Http;
import services.TranslationNotFoundException;
import services.program.ProgramDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import views.admin.docs.ApiDocsPageViewModel;
import views.admin.docs.ApiDocsPageViewModel.ProgramSlugOption;
import views.admin.docs.ApiDocsPageViewModel.QuestionDoc;

/** Maps pre-fetched API docs data to the {@link ApiDocsPageViewModel}. */
public final class ApiDocsPageMapper {

  public ApiDocsPageViewModel map(
      Http.Request request,
      String selectedProgramSlug,
      LifecycleStage lifecycleStage,
      ImmutableSet<String> allNonExternalProgramSlugs,
      Optional<ProgramDefinition> programDefinition,
      String jsonPreview,
      ImmutableMap<String, ImmutableList<String>> historicOptionsByQuestionNameKey) {

    List<QuestionDoc> questionDocs =
        programDefinition
            .map(pd -> buildQuestionDocs(pd, historicOptionsByQuestionNameKey))
            .orElse(List.of());

    return ApiDocsPageViewModel.builder()
        .selectedProgramSlug(selectedProgramSlug)
        .activeVersion(lifecycleStage == LifecycleStage.ACTIVE)
        .programSlugs(buildSlugOptions(allNonExternalProgramSlugs, selectedProgramSlug))
        .programFound(programDefinition.isPresent())
        .questions(questionDocs)
        .jsonPreview(jsonPreview)
        .apiDocsLink(controllers.docs.routes.ApiDocsController.index().absoluteURL(request))
        .build();
  }

  private static List<ProgramSlugOption> buildSlugOptions(
      ImmutableSet<String> allNonExternalProgramSlugs, String selectedProgramSlug) {
    return allNonExternalProgramSlugs.stream()
        .sorted()
        .map(
            slug ->
                ProgramSlugOption.builder()
                    .slug(slug)
                    .selected(slug.equals(selectedProgramSlug))
                    .build())
        .collect(Collectors.toList());
  }

  private static List<QuestionDoc> buildQuestionDocs(
      ProgramDefinition programDefinition,
      ImmutableMap<String, ImmutableList<String>> historicOptionsByQuestionNameKey) {
    return programDefinition
        .streamQuestionDefinitions()
        .sorted(Comparator.comparing(QuestionDefinition::getQuestionNameKey))
        .map(qd -> mapQuestionDoc(qd, historicOptionsByQuestionNameKey))
        .collect(Collectors.toList());
  }

  private static QuestionDoc mapQuestionDoc(
      QuestionDefinition qd,
      ImmutableMap<String, ImmutableList<String>> historicOptionsByQuestionNameKey) {
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
          historicOptionsByQuestionNameKey.getOrDefault(
              qd.getQuestionNameKey(), ImmutableList.of());
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
