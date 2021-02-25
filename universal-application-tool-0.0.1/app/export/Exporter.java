package export;

import java.io.Writer;
import models.Applicant;

public interface Exporter {
  public void export(Applicant a, Writer w);
}
