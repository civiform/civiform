package support;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import models.Applicant;
import models.Program;
import play.inject.Injector;
import services.program.ProgramDefinition;
import services.program.ProgramService;
import services.question.NameQuestionDefinition;
import services.question.QuestionDefinition;
import services.question.QuestionService;

public class ResourceFabricator {

  private final Injector injector;
  private final ProgramService programService;
  private final QuestionService questionService;

  public ResourceFabricator(Injector injector) {
    this.injector = checkNotNull(injector);
    this.programService = injector.instanceOf(ProgramService.class);
    this.questionService = injector.instanceOf(QuestionService.class);
  }

  public Program insertProgram(String name) {
    Program program = new Program(name, "description");
    program.save();
    return program;
  }

  public QuestionDefinition insertQuestionDefinition() {
    return questionService
        .create(
            new NameQuestionDefinition(
                123L,
                1L,
                "my name",
                "my.path.name",
                "description",
                ImmutableMap.of(Locale.ENGLISH, "question?"),
                Optional.of(ImmutableMap.of(Locale.ENGLISH, "help text"))))
        .get();
  }

  public ProgramDefinition insertProgramWithOneBlock(String name) {
    try {
      ProgramDefinition programDefinition =
          programService.createProgramDefinition("test program", "desc");
      programDefinition =
          programService.addBlockToProgram(
              programDefinition.id(), "test block", "test block description");
      programDefinition =
          programService.setBlockQuestions(
              programDefinition.id(),
              programDefinition.blockDefinitions().get(0).id(),
              ImmutableList.of(insertQuestionDefinition()));

      return programDefinition;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Applicant insertApplicant() {
    Applicant applicant = new Applicant();
    applicant.save();
    return applicant;
  }

  public <T> T instanceOf(Class<T> clazz) {
    return injector.instanceOf(clazz);
  }
}
