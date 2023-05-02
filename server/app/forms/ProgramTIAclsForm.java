package forms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
public final class ProgramTIAclsForm {

  // Caution: This must be a mutable list type, or else Play's form binding cannot add elements to
  // the list. This means the constructors MUST set this field to a mutable List type, NOT
  // ImmutableList.
  List<String> tiNames;
  List<Long> tiIds;
  public ProgramTIAclsForm()
  {
    this.tiNames = new ArrayList<>();
    this.tiIds = new ArrayList<>();
  }

}
