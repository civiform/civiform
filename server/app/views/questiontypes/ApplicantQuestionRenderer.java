package views.questiontypes;

import j2html.tags.specialized.DivTag;

/**
 * Interface for all applicant question renderers. An applicant question renderer renders a question
 * to be seen by an applicant
 */
public interface ApplicantQuestionRenderer {

  String getReferenceClass();

  /**
   * Renders an applicant question's text, help text, errors, and the given form content.
   *
   * <p>In some cases, like text questions and number questions, the question errors are rendered in
   * the form content, so we offer the ability to specify whether or not this method should render
   * the question errors here.
   */
  DivTag render(ApplicantQuestionRendererParams params);
}
