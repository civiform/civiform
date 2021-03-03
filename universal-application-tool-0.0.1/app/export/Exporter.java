package export;

import java.io.IOException;
import java.io.Writer;
import models.Applicant;

public interface Exporter {
  /** Write the data for the specified applicant to the specified writer. */
  void export(Applicant applicant, Writer writer) throws IOException;
}
