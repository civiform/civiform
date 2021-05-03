package views.questiontypes;

import j2html.tags.Tag;
import play.i18n.Messages;

public interface ApplicantQuestionRenderer {

  Tag render(Messages messages);
}
