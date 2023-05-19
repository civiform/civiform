package models;

import static org.junit.Assert.assertThrows;

import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.question.exceptions.QuestionNotFoundException;

// TODO(#4912): Add more tests for Version.java.
public class VersionTest extends ResetPostgres {

  private VersionRepository versionRepository;

  @Before
  public void setupProgramRepository() {
    versionRepository = instanceOf(VersionRepository.class);
  }

  @Test
  public void addTombstoneForQuestion_questionNotInVersion_throwsException() throws Exception {
    Version activeVersion = versionRepository.getActiveVersion();
    Version draftVersion = versionRepository.getDraftVersion();

    Question question = resourceCreator.insertQuestion("question");
    question.addVersion(activeVersion).save();

    assertThrows(
        QuestionNotFoundException.class, () -> draftVersion.addTombstoneForQuestion(question));
  }
}
