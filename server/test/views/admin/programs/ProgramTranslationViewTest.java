package views.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableList;
import forms.translation.ProgramTranslationForm;
import java.util.Locale;
import java.util.Optional;
import models.ApplicationStep;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import play.data.FormFactory;
import play.mvc.Http;
import play.twirl.api.Content;
import repository.ApplicationStatusesRepository;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import services.statuses.StatusDefinitions;
import support.ProgramBuilder;

public class ProgramTranslationViewTest extends ResetPostgres {

  private ProgramTranslationView programTranslationView;
  private ApplicationStatusesRepository applicationStatusesRepository;
  private FormFactory formFactory;

  @Before
  public void setup() {
    programTranslationView = instanceOf(ProgramTranslationView.class);
    applicationStatusesRepository = instanceOf(ApplicationStatusesRepository.class);
    formFactory = instanceOf(FormFactory.class);
  }

  @Test
  public void render_applicationStepsDisplayedInOwnSections() {
    // Create a program with multiple application steps
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test-program", "Test Program Description")
            .withApplicationSteps(
                ImmutableList.of(
                    new ApplicationStep(
                        LocalizedStrings.withDefaultValue("Step One Title"),
                        LocalizedStrings.withDefaultValue("Step One Description")),
                    new ApplicationStep(
                        LocalizedStrings.withDefaultValue("Step Two Title"),
                        LocalizedStrings.withDefaultValue("Step Two Description")),
                    new ApplicationStep(
                        LocalizedStrings.withDefaultValue("Step Three Title"),
                        LocalizedStrings.withDefaultValue("Step Three Description"))))
            .build();

    ProgramDefinition programDef = program.getProgramDefinition();
    StatusDefinitions statusDefinitions =
        applicationStatusesRepository.lookupActiveStatusDefinitions(programDef.adminName());
    ProgramTranslationForm translationForm =
        ProgramTranslationForm.fromProgram(
            programDef, Locale.FRENCH, formFactory, statusDefinitions);

    Http.Request request = fakeRequestBuilder().build();
    Content content =
        programTranslationView.render(
            request,
            Locale.FRENCH,
            programDef,
            statusDefinitions,
            translationForm,
            Optional.empty());

    String renderedHtml = content.body();

    // Verify each application step has its own section with numbered header
    assertThat(renderedHtml).contains("Application step 1");
    assertThat(renderedHtml).contains("Application step 2");
    assertThat(renderedHtml).contains("Application step 3");
  }

  @Test
  public void render_applicationStepFieldsHaveSimplifiedLabels() {
    // Create a program with an application step
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test-program", "Test Program Description")
            .withApplicationSteps(
                ImmutableList.of(
                    new ApplicationStep(
                        LocalizedStrings.withDefaultValue("Step Title"),
                        LocalizedStrings.withDefaultValue("Step Description"))))
            .build();

    ProgramDefinition programDef = program.getProgramDefinition();
    StatusDefinitions statusDefinitions =
        applicationStatusesRepository.lookupActiveStatusDefinitions(programDef.adminName());
    ProgramTranslationForm translationForm =
        ProgramTranslationForm.fromProgram(
            programDef, Locale.FRENCH, formFactory, statusDefinitions);

    Http.Request request = fakeRequestBuilder().build();
    Content content =
        programTranslationView.render(
            request,
            Locale.FRENCH,
            programDef,
            statusDefinitions,
            translationForm,
            Optional.empty());

    String renderedHtml = content.body();

    // Within application step sections, labels should be simplified to just "Title" and
    // "Description"
    // The section header "Application step N" provides context
    assertThat(renderedHtml).contains("Application step 1");

    // The form should contain inputs for application step title and description
    assertThat(renderedHtml).contains("name=\"application-step-title-0\"");
    assertThat(renderedHtml).contains("name=\"application-step-description-0\"");
  }

  @Test
  public void render_applicationStepsHaveSimplifiedLabels() {
    // Create a program with application steps
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test-program", "Test Program Description")
            .withApplicationSteps(
                ImmutableList.of(
                    new ApplicationStep(
                        LocalizedStrings.withDefaultValue("Step Title"),
                        LocalizedStrings.withDefaultValue("Step Description"))))
            .build();

    ProgramDefinition programDef = program.getProgramDefinition();
    StatusDefinitions statusDefinitions =
        applicationStatusesRepository.lookupActiveStatusDefinitions(programDef.adminName());
    ProgramTranslationForm translationForm =
        ProgramTranslationForm.fromProgram(
            programDef, Locale.FRENCH, formFactory, statusDefinitions);

    Http.Request request = fakeRequestBuilder().build();
    Content content =
        programTranslationView.render(
            request,
            Locale.FRENCH,
            programDef,
            statusDefinitions,
            translationForm,
            Optional.empty());

    String renderedHtml = content.body();

    // Labels within application step sections should be simplified to "Title" and "Description"
    // since the section header "Application step N" provides the context
    assertThat(renderedHtml).contains(">Title<");
    assertThat(renderedHtml).contains(">Description<");
  }

  @Test
  public void render_noApplicationSteps_noApplicationStepSections() {
    // Create a program without application steps (empty list)
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test-program", "Test Program Description")
            .withApplicationSteps(ImmutableList.of())
            .build();

    ProgramDefinition programDef = program.getProgramDefinition();
    StatusDefinitions statusDefinitions =
        applicationStatusesRepository.lookupActiveStatusDefinitions(programDef.adminName());
    ProgramTranslationForm translationForm =
        ProgramTranslationForm.fromProgram(
            programDef, Locale.FRENCH, formFactory, statusDefinitions);

    Http.Request request = fakeRequestBuilder().build();
    Content content =
        programTranslationView.render(
            request,
            Locale.FRENCH,
            programDef,
            statusDefinitions,
            translationForm,
            Optional.empty());

    String renderedHtml = content.body();

    // Should not contain any application step sections
    assertThat(renderedHtml).doesNotContain("Application step");
  }

  @Test
  public void render_applicationStepsWithEmptyTitleNotDisplayed() {
    // Create a program with an application step that has an empty title
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test-program", "Test Program Description")
            .withApplicationSteps(
                ImmutableList.of(
                    new ApplicationStep(
                        LocalizedStrings.withDefaultValue(""),
                        LocalizedStrings.withDefaultValue("Description without title"))))
            .build();

    ProgramDefinition programDef = program.getProgramDefinition();
    StatusDefinitions statusDefinitions =
        applicationStatusesRepository.lookupActiveStatusDefinitions(programDef.adminName());
    ProgramTranslationForm translationForm =
        ProgramTranslationForm.fromProgram(
            programDef, Locale.FRENCH, formFactory, statusDefinitions);

    Http.Request request = fakeRequestBuilder().build();
    Content content =
        programTranslationView.render(
            request,
            Locale.FRENCH,
            programDef,
            statusDefinitions,
            translationForm,
            Optional.empty());

    String renderedHtml = content.body();

    // Steps with empty title should not be displayed (per the condition in addApplicationSteps)
    assertThat(renderedHtml).doesNotContain("Application step 1");
  }

  @Test
  public void render_applicationStepSectionsContainEditDefaultLink() {
    // Create a program with an application step
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test-program", "Test Program Description")
            .withApplicationSteps(
                ImmutableList.of(
                    new ApplicationStep(
                        LocalizedStrings.withDefaultValue("Step Title"),
                        LocalizedStrings.withDefaultValue("Step Description"))))
            .build();

    ProgramDefinition programDef = program.getProgramDefinition();
    StatusDefinitions statusDefinitions =
        applicationStatusesRepository.lookupActiveStatusDefinitions(programDef.adminName());
    ProgramTranslationForm translationForm =
        ProgramTranslationForm.fromProgram(
            programDef, Locale.FRENCH, formFactory, statusDefinitions);

    Http.Request request = fakeRequestBuilder().build();
    Content content =
        programTranslationView.render(
            request,
            Locale.FRENCH,
            programDef,
            statusDefinitions,
            translationForm,
            Optional.empty());

    String renderedHtml = content.body();

    // Each application step section should have an "(edit default)" link
    assertThat(renderedHtml).contains("(edit default)");
  }
}
