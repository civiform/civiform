package controllers.admin;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http;
import play.mvc.Result;
import play.twirl.api.Content;
import repository.VersionRepository;
import services.apikey.ApiKeyService;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramDefinition;
import services.program.ProgramService;
import support.ProgramBuilder;
import auth.ProfileUtils;
import views.admin.apikeys.ApiKeyCredentialsView;
import views.admin.apikeys.ApiKeyIndexView;
import views.admin.apikeys.ApiKeyNewOneView;
import org.junit.Before;
import play.data.FormFactory;
import play.inject.Injector;
import play.test.WithApplication;

import static org.assertj.core.api.Assertions.assertThat;

public class AdminApiKeysControllerTest extends WithApplication {

        private ApiKeyService apiKeyService;
        private ApiKeyIndexView indexView;
        private ApiKeyNewOneView apiKeyNewOneView;
        private ApiKeyCredentialsView apiKeyCredentialsView;
        private ProgramService programService;
        private FormFactory formFactory;
        private ProfileUtils profileUtils;
        private VersionRepository versionRepository;
        private AdminApiKeysController controller;

        @Override
        protected Application provideApplication() {
                return new GuiceApplicationBuilder().build();
        }

        @Before
        public void setUp() {
                Injector injector = app.injector();
                ProgramBuilder.setInjector(injector);

                apiKeyService = mock(ApiKeyService.class);
                indexView = mock(ApiKeyIndexView.class);
                apiKeyNewOneView = mock(ApiKeyNewOneView.class);
                apiKeyCredentialsView = mock(ApiKeyCredentialsView.class);
                programService = mock(ProgramService.class);
                formFactory = mock(FormFactory.class);
                profileUtils = mock(ProfileUtils.class);
                versionRepository = mock(VersionRepository.class);

                controller = new AdminApiKeysController(
                                apiKeyService,
                                indexView,
                                apiKeyNewOneView,
                                apiKeyCredentialsView,
                                programService,
                                formFactory,
                                profileUtils,
                                versionRepository);
        }

        @Test
        public void newOne_rendersInternalProgramsOnly() {
                ProgramDefinition internalProgram1 = ProgramBuilder.newActiveProgram("Internal One", "desc1")
                                .buildDefinition();
                ProgramDefinition externalProgram = ProgramBuilder.newActiveProgram("External", "desc2")
                                .buildDefinition().toBuilder().setExternalLink("https://external.com").build();
                ProgramDefinition internalProgram2 = ProgramBuilder.newActiveProgram("Internal Two", "desc3")
                                .buildDefinition();

                ImmutableList<ProgramDefinition> activePrograms = ImmutableList.of(internalProgram1, externalProgram,
                                internalProgram2);
                ActiveAndDraftPrograms allPrograms = mock(ActiveAndDraftPrograms.class);
                when(allPrograms.getActivePrograms()).thenReturn(activePrograms);
                when(programService.getActiveAndDraftPrograms()).thenReturn(allPrograms);

                ImmutableSet<String> expectedProgramNames = activePrograms.stream()
                                .filter(program -> program.externalLink() == null || program.externalLink().isBlank())
                                .map(ProgramDefinition::adminName)
                                .collect(ImmutableSet.toImmutableSet());

                Content mockContent = mock(Content.class);
                when(mockContent.body()).thenReturn("Mocked content body");

                when(apiKeyNewOneView.render(any(Http.Request.class), eq(expectedProgramNames)))
                                .thenReturn(mockContent);

                Result result = controller.newOne(fakeRequest().build());

                assertThat(result.status()).isEqualTo(200);
                verify(apiKeyNewOneView).render(any(Http.Request.class), eq(expectedProgramNames));
                verify(apiKeyNewOneView, never()).renderNoPrograms(any());
        }

        @Test
        public void newOne_rendersNoProgramsIfAllAreExternal() {
                ProgramDefinition externalProgram1 = ProgramBuilder.newActiveProgram("External 1", "desc1")
                                .buildDefinition().toBuilder().setExternalLink("https://link1").build();
                ProgramDefinition externalProgram2 = ProgramBuilder.newActiveProgram("External 2", "desc2")
                                .buildDefinition().toBuilder().setExternalLink("https://link2").build();

                ImmutableList<ProgramDefinition> activePrograms = ImmutableList.of(externalProgram1, externalProgram2);
                ActiveAndDraftPrograms allPrograms = mock(ActiveAndDraftPrograms.class);
                when(allPrograms.getActivePrograms()).thenReturn(activePrograms);
                when(programService.getActiveAndDraftPrograms()).thenReturn(allPrograms);

                ImmutableSet<String> programNames = activePrograms.stream()
                                .filter(program -> program.externalLink() == null || program.externalLink().isBlank())
                                .map(ProgramDefinition::adminName)
                                .collect(ImmutableSet.toImmutableSet());
                assert programNames.isEmpty();

                Content mockContent = mock(Content.class);
                when(mockContent.body()).thenReturn("Mocked content body");

                when(apiKeyNewOneView.renderNoPrograms(any(Http.Request.class))).thenReturn(mockContent);

                Result result = controller.newOne(fakeRequest().build());

                assertThat(result.status()).isEqualTo(200);
                verify(apiKeyNewOneView).renderNoPrograms(any(Http.Request.class));
                verify(apiKeyNewOneView, never()).render(any(), any());
        }

        @Test
        public void newOne_rendersAllInternalIfNoneAreExternal() {
                ProgramDefinition internal1 = ProgramBuilder.newActiveProgram("Alpha", "desc").buildDefinition();
                ProgramDefinition internal2 = ProgramBuilder.newActiveProgram("Beta", "desc").buildDefinition();

                ImmutableList<ProgramDefinition> activePrograms = ImmutableList.of(internal1, internal2);
                ActiveAndDraftPrograms allPrograms = mock(ActiveAndDraftPrograms.class);
                when(allPrograms.getActivePrograms()).thenReturn(activePrograms);
                when(programService.getActiveAndDraftPrograms()).thenReturn(allPrograms);

                ImmutableSet<String> expectedProgramNames = activePrograms.stream()
                                .filter(program -> program.externalLink() == null || program.externalLink().isBlank())
                                .map(ProgramDefinition::adminName)
                                .collect(ImmutableSet.toImmutableSet());

                Content mockContent = mock(Content.class);
                when(mockContent.body()).thenReturn("Mocked content body");

                when(apiKeyNewOneView.render(any(Http.Request.class), eq(expectedProgramNames)))
                                .thenReturn(mockContent);

                Result result = controller.newOne(fakeRequest().build());

                assertThat(result.status()).isEqualTo(200);
                verify(apiKeyNewOneView).render(any(Http.Request.class), eq(expectedProgramNames));
                verify(apiKeyNewOneView, never()).renderNoPrograms(any());
        }
}