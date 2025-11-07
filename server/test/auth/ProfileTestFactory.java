package auth;

import javax.inject.Inject;
import models.AccountModel;

public class ProfileTestFactory {
  ProfileFactory profileFactory;

  @Inject
  public ProfileTestFactory(ProfileFactory profileFactory) {
    this.profileFactory = profileFactory;
  }

  public CiviFormProfile wrap(AccountModel account) {
    return profileFactory.wrapProfileData(
        new CiviFormProfileData(account.id, profileFactory.getClock()));
  }

  public CiviFormProfile wrapTi(AccountModel account) {
    var data = new CiviFormProfileData(account.id, profileFactory.getClock());
    data.addRole(Role.ROLE_TI.toString());
    return profileFactory.wrapProfileData(data);
  }
}
