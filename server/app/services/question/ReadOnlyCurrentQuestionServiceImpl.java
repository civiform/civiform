package services.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import models.VersionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.VersionRepository;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.NullQuestionDefinition;
import services.question.types.QuestionDefinition;

/**
 * Implementation class for {@link ReadOnlyQuestionService} interface. It contains all questions
 * that are in the current active version and the current draft version.
 *
 * <p>See {@link QuestionService#getReadOnlyQuestionService}.
 */
public final class ReadOnlyCurrentQuestionServiceImpl implements ReadOnlyQuestionService {

  private final ImmutableMap<Long, QuestionDefinition> questionsById;
  private final ImmutableSet<QuestionDefinition> upToDateQuestions;
  private final ActiveAndDraftQuestions activeAndDraftQuestions;
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ReadOnlyCurrentQuestionServiceImpl.class);

  public ReadOnlyCurrentQuestionServiceImpl(VersionRepository repository) {
    VersionModel activeVersion = repository.getActiveVersion();
    VersionModel draftVersion = repository.getDraftVersionOrCreate();
    ImmutableMap.Builder<Long, QuestionDefinition> questionIdMap = ImmutableMap.builder();
    ImmutableSet.Builder<QuestionDefinition> upToDateBuilder = ImmutableSet.builder();
    Set<String> namesFoundInDraft = new HashSet<>();
    for (QuestionDefinition qd :
        repository.getQuestionDefinitionsForVersion(draftVersion).stream()
            .collect(Collectors.toList())) {
      if (!draftVersion.getTombstonedQuestionNames().contains(qd.getName())) {
        // If the question is about to be deleted, it is not "up to date."
        upToDateBuilder.add(qd);
      }
      questionIdMap.put(qd.getId(), qd);
      namesFoundInDraft.add(qd.getName());
    }
    for (QuestionDefinition qd :
        repository.getQuestionDefinitionsForVersion(activeVersion).stream()
            .collect(Collectors.toList())) {

      questionIdMap.put(qd.getId(), qd);
      if (!namesFoundInDraft.contains(qd.getName())
          && !draftVersion.getTombstonedQuestionNames().contains(qd.getName())) {
        upToDateBuilder.add(qd);
      }
    }
    questionsById = questionIdMap.build();
    upToDateQuestions = upToDateBuilder.build();
    activeAndDraftQuestions = ActiveAndDraftQuestions.buildFromCurrentVersions(repository);
  }

  @Override
  public ActiveAndDraftQuestions getActiveAndDraftQuestions() {
    return activeAndDraftQuestions;
  }

  @Override
  public ImmutableList<QuestionDefinition> getAllQuestions() {
    return questionsById.values().asList();
  }

  @Override
  public ImmutableList<QuestionDefinition> getUpToDateQuestions() {
    return upToDateQuestions.asList();
  }

  @Override
  public ImmutableList<EnumeratorQuestionDefinition> getUpToDateEnumeratorQuestions() {
    return getUpToDateQuestions().stream()
        .filter(QuestionDefinition::isEnumerator)
        .map(questionDefinition -> (EnumeratorQuestionDefinition) questionDefinition)
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public ImmutableList<EnumeratorQuestionDefinition> getAllEnumeratorQuestions() {
    return getAllQuestions().stream()
        .filter(QuestionDefinition::isEnumerator)
        .map(questionDefinition -> (EnumeratorQuestionDefinition) questionDefinition)
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public QuestionDefinition getQuestionDefinition(long id) throws QuestionNotFoundException {
    if (questionsById.containsKey(id)) {
      return questionsById.get(id);
    }

    LOGGER.error("Question not found for ID: {}", id);
    return new NullQuestionDefinition(id);
  }
}
