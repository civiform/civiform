package services.settings;

/** The access mode for a server setting. */
public enum SettingMode {
  // Hidden settings should never be shown in the UI or otherwise revealed.
  // They include secrets such as cryptographic values and passwords.
  HIDDEN,
  // May be displayed to admins.
  ADMIN_READABLE,
  // May be displayed to and updated by admins.
  ADMIN_WRITEABLE;
}
