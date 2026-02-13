class FeatureFlags {
  private readonly flags: {
    isAdminUiMigrationScEnabled: boolean
    isAdminUiMigrationScExtendedEnabled: boolean
  }

  constructor() {
    this.flags = (window as any).featureFlags || {
      isAdminUiMigrationScEnabled: false,
      isAdminUiMigrationScExtendedEnabled: false,
    }
  }

  get isAdminUiMigrationScEnabled(): boolean {
    return this.flags.isAdminUiMigrationScEnabled
  }

  get isAdminUiMigrationScExtendedEnabled(): boolean {
    return this.flags.isAdminUiMigrationScExtendedEnabled
  }

  // Method to get all flags as an object
  getAll(): Readonly<typeof this.flags> {
    return {...this.flags}
  }
}

// Export a singleton instance
export const featureFlags = new FeatureFlags()
