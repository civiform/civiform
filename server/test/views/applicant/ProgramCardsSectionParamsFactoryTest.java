package views.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import auth.ProfileUtils;
import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.applicant.ApplicantRoutes;
import java.util.Locale;
import java.util.Optional;
import models.DisplayMode;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.LocalizedStrings;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import services.applicant.ApplicantService.ApplicantProgramData;
import services.cloud.PublicStorageClient;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import views.applicant.ProgramCardsSectionParamsFactory.ProgramCardParams;

public class ProgramCardsSectionParamsFactoryTest {
  private ProgramCardsSectionParamsFactory factory;

  private static final String NORMAL_URL = "https://civiform.us";

  // Map each test URL to the expected result
  private static final ImmutableMap<String, String> TEST_URLS_AND_EXPECTED =
      ImmutableMap.of(
          NORMAL_URL,
          NORMAL_URL,
          "<script>alert('XSS')</script>",
          "",
          "javascript:alert('XSS')",
          "",
          "https://civiform.us/foo?query=javascript:alert('XSS')",
          "https://civiform.us/foo?query&#61;javascript:alert(&#39;XSS&#39;)",
          "https://civiform.us/foo?query=<script>alert('XSS')</script>",
          "");

  @Before
  public void setUp() {
    ApplicantRoutes applicantRoutes = new ApplicantRoutes();

    ProfileUtils profileUtils = mock(ProfileUtils.class);
    PublicStorageClient publicStorageClient = mock(PublicStorageClient.class);
    ApplicantService applicantService = mock(ApplicantService.class);

    factory =
        new ProgramCardsSectionParamsFactory(
            applicantRoutes, profileUtils, publicStorageClient, applicantService);
  }

  @Test
  public void testDetailsURLs() {
    for (String key : TEST_URLS_AND_EXPECTED.keySet()) {
      exerciseSanitization(key, TEST_URLS_AND_EXPECTED.get(key));
    }
  }

  public void exerciseSanitization(String input, String expectedResult) {
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(1)
            .setAdminName("Test admin name")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "Test name"))
            .setAdminDescription("Test admin description")
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "Test description"))
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(false)
            .setAcls(mock(ProgramAcls.class))
            .setCategories(ImmutableList.of())
            .setExternalLink(input)
            .build();

    Messages messages = mock(Messages.class);
    when(messages.at(anyString())).thenReturn("placeholder");

    ApplicantProgramData programDatum = mock(ApplicantProgramData.class);
    when(programDatum.program()).thenReturn(program);

    ProgramCardParams params =
        factory.getCard(
            mock(Request.class),
            messages,
            MessageKey.BUTTON_APPLY,
            programDatum,
            Locale.US,
            Optional.empty(),
            Optional.empty(),
            mock(ApplicantPersonalInfo.class));

    assertThat(params.detailsUrl()).isEqualTo(expectedResult);
  }
}
