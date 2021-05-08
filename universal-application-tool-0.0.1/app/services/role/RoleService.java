package services.role;

import com.google.common.collect.ImmutableList;

public interface RoleService {

  void makeProgramAdmins(long programId, ImmutableList<String> accountEmails);
}
