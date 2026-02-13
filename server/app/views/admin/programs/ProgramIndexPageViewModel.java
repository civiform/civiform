package views.admin.programs;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

@Data
@Builder
public class ProgramIndexPageViewModel implements BaseViewModel {
  private final String civicEntityName;
  private final boolean hasDisabledPrograms;
  private final boolean inUseTabSelected;
  private final boolean showPublishAllButton;
  private final ImmutableList<ProgramCardData> programs;
  private final int draftProgramCount;
  private final int draftQuestionCount;
  private final Optional<String> successMessage;
  private final Optional<String> errorMessage;
  private final ImmutableList<PublishDraftProgramItem> draftProgramsForPublish;
  private final ImmutableList<PublishDraftQuestionItem> draftQuestionsForPublish;

  public String getNewProgramUrl() {
    return routes.AdminProgramController.newOne().url();
  }

  public String getImportProgramUrl() {
    return controllers.admin.routes.AdminImportController.index().url();
  }

  public String getPublishAllUrl() {
    return routes.AdminProgramController.publish().url();
  }

  public String getDemographicsCsvUrl() {
    return routes.AdminApplicationController.downloadDemographics(
            Optional.empty(), Optional.empty())
        .url();
  }

  public String getInUseTabUrl() {
    return routes.AdminProgramController.index().url();
  }

  public String getDisabledTabUrl() {
    return routes.AdminProgramController.indexDisabled().url();
  }

  public String getDraftProgramLabel() {
    return draftProgramCount == 1 ? "program" : "programs";
  }

  public String getDraftQuestionLabel() {
    return draftQuestionCount == 1 ? "question" : "questions";
  }

  @Data
  @Builder
  public static class ProgramCardData {
    private final String programName;
    private final String programTypeIndicator;
    private final String shortDescription;
    private final String longDescription;
    private final String adminName;
    private final String programType;
    private final long lastUpdatedMillis;
    private final List<String> categories;

    // Draft info
    private final Optional<DraftInfo> draft;
    // Active info
    private final Optional<ActiveInfo> active;

    public String getPublishHeadingText() {

      if (programType.equals("external")) {
        return "Are you sure you want to publish %s?".formatted(programName);
      }

      return "Are you sure you want to publish %s and all of its draft questions?"
          .formatted(programName);
    }
  }

  @Data
  @Builder
  public static class DraftInfo {
    private final long programId;
    private final String programAdminName;
    private final boolean isExternalProgram;
    private final Optional<String> universalQuestionsText;
    private final boolean translationComplete;
    private final boolean usesAllUniversalQuestions;

    public String getEditUrl() {
      return controllers.admin.routes.AdminProgramBlocksController.index(programId).url();
    }

    public String getPublishUrl() {
      return routes.AdminProgramController.publishProgram(programId).url();
    }

    public String getManageTranslationsUrl() {
      return controllers.admin.routes.AdminProgramTranslationsController.redirectToFirstLocale(
              programAdminName)
          .url();
    }

    public String getManageStatusesUrl() {
      return controllers.admin.routes.AdminProgramStatusesController.index(programId).url();
    }

    public String getManageProgramAdminsUrl() {
      return controllers.admin.routes.ProgramAdminManagementController.edit(programId).url();
    }

    public String getExportUrl() {
      return controllers.admin.routes.AdminExportController.index(programId).url();
    }
  }

  @Data
  @Builder
  public static class ActiveInfo {
    private final long programId;
    private final String programAdminName;
    private final String slug;
    private final String baseUrl;
    private final boolean isExternalProgram;
    private final boolean hasDraft;
    private final boolean isPreScreenerForm;
    private final Optional<String> universalQuestionsText;
    private final boolean translationComplete;
    private final boolean usesAllUniversalQuestions;

    public String getViewUrl() {
      return controllers.admin.routes.AdminProgramBlocksController.readOnlyIndex(programId).url();
    }

    public String getNewVersionUrl() {
      return routes.AdminProgramController.newVersionFrom(programId).url();
    }

    public Optional<String> getShareLink() {
      return isExternalProgram
          ? Optional.empty()
          : Optional.of(
              baseUrl + controllers.applicant.routes.ApplicantProgramsController.show(slug).url());
    }

    public String getApplicationsUrl() {
      return routes.AdminApplicationController.index(
              programId,
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty())
          .url();
    }

    public String getEditUrl() {
      return controllers.admin.routes.AdminProgramBlocksController.index(programId).url();
    }

    public String getManageProgramAdminsUrl() {
      return controllers.admin.routes.ProgramAdminManagementController.edit(programId).url();
    }

    public String getExportUrl() {
      return controllers.admin.routes.AdminExportController.index(programId).url();
    }

    public Optional<String> getManageTranslationsUrl() {
      return hasDraft
          ? Optional.empty()
          : Optional.of(
              controllers.admin.routes.AdminProgramTranslationsController.redirectToFirstLocale(
                      programAdminName)
                  .url());
    }
  }

  /** Data for a draft program item shown in the publish-all modal. */
  @Data
  @Builder
  public static class PublishDraftProgramItem {
    private final String name;
    private final String visibilityText;
    private final Optional<String> universalQuestionsText;
    private final String editUrl;
  }

  /** Data for a draft question item shown in the publish-all modal. */
  @Data
  @Builder
  public static class PublishDraftQuestionItem {
    private final String name;
    private final String editUrl;
  }
}
