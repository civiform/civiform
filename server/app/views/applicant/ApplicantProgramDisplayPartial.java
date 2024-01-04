package views.applicant;

import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.ol;

import auth.CiviFormProfile;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import j2html.tags.specialized.DivTag;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.Modal;
import views.style.ApplicantStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

final class ApplicantProgramDisplayPartial extends BaseHtmlView {

  private final ProgramCardViewRenderer programCardViewRenderer;
  private final ZoneId zoneId;

  @Inject
  ApplicantProgramDisplayPartial(ProgramCardViewRenderer programCardViewRenderer, ZoneId zoneId) {
    this.programCardViewRenderer = Preconditions.checkNotNull(programCardViewRenderer);
    this.zoneId = Preconditions.checkNotNull(zoneId);
  }

  DivTag programCardsSection(
      Http.Request request,
      Messages messages,
      ApplicantPersonalInfo personalInfo,
      Optional<MessageKey> sectionTitle,
      String cardContainerStyles,
      long applicantId,
      Locale preferredLocale,
      ImmutableList<ApplicantService.ApplicantProgramData> cards,
      MessageKey buttonTitle,
      MessageKey buttonSrText,
      HtmlBundle bundle,
      CiviFormProfile profile) {
    String sectionHeaderId = Modal.randomModalId();
    DivTag div = div().withClass(ReferenceClasses.APPLICATION_PROGRAM_SECTION);
    if (sectionTitle.isPresent()) {
      div.with(
          h3().withId(sectionHeaderId)
              .withText(messages.at(sectionTitle.get().getKeyName()))
              .withClasses(ApplicantStyles.PROGRAM_CARDS_SUBTITLE));
    }
    return div.with(
        ol().attr("aria-labelledby", sectionHeaderId)
            .withClasses(cardContainerStyles)
            .with(
                each(
                    cards,
                    (card) ->
                        programCardViewRenderer.createProgramCard(
                            request,
                            messages,
                            personalInfo.getType(),
                            card,
                            applicantId,
                            preferredLocale,
                            buttonTitle,
                            buttonSrText,
                            sectionTitle.isPresent(),
                            bundle,
                            profile,
                            zoneId))));
  }

  enum ContainerWidth {
    MEDIUM,
    FULL;
  }

  /**
   * This method generates a list of style classes with responsive column counts. The number of
   * columns should not exceed the number of programs, or the program card container will not be
   * centered.
   */
  String programCardsContainerStyles(ContainerWidth containerWidth, int numPrograms) {
    switch (containerWidth) {
      case FULL:
        return StyleUtils.joinStyles(
            ApplicantStyles.PROGRAM_CARDS_CONTAINER_BASE,
            numPrograms >= 2 ? StyleUtils.responsiveMedium("grid-cols-2") : "",
            numPrograms >= 3 ? StyleUtils.responsiveLarge("grid-cols-3") : "",
            numPrograms >= 4 ? StyleUtils.responsiveXLarge("grid-cols-4") : "",
            numPrograms >= 5 ? StyleUtils.responsive2XLarge("grid-cols-5") : "");
      case MEDIUM:
        return StyleUtils.joinStyles(
            ApplicantStyles.PROGRAM_CARDS_CONTAINER_BASE,
            numPrograms >= 2 ? StyleUtils.responsiveMedium("grid-cols-1") : "",
            numPrograms >= 3 ? StyleUtils.responsiveLarge("grid-cols-2") : "",
            numPrograms >= 4 ? StyleUtils.responsive2XLarge("grid-cols-3") : "",
            numPrograms >= 5 ? StyleUtils.responsive3XLarge("grid-cols-4") : "");
      default:
        throw new RuntimeException("Unrecognized container width: " + containerWidth);
    }
  }
}
