/**
 * Enum for YES/NO question option values.
 * These values match the backend Java YesNoQuestionOption enum
 * to ensure consistency across the application.
 */
export enum YesNoOptionValue {
  YES = '1',
  NO = '0',
  NOT_SURE = '2',
  MAYBE = '3',
}

/**
 * Admin IDs for YES/NO question checkboxes
 */
export enum YesNoOptionAdminId {
  YES = 'yes',
  NO = 'no',
  NOT_SURE = 'not-sure',
  MAYBE = 'maybe',
}

/**
 * Display text for YES/NO question options
 */
export const YesNoOptionText = {
  YES: 'Yes',
  NO: 'No',
  NOT_SURE: 'Not sure',
  MAYBE: 'Maybe',
} as const

/**
 * Maps option values to their admin IDs
 */
export const VALUE_TO_ADMIN_ID: Record<YesNoOptionValue, YesNoOptionAdminId> = {
  [YesNoOptionValue.YES]: YesNoOptionAdminId.YES,
  [YesNoOptionValue.NO]: YesNoOptionAdminId.NO,
  [YesNoOptionValue.NOT_SURE]: YesNoOptionAdminId.NOT_SURE,
  [YesNoOptionValue.MAYBE]: YesNoOptionAdminId.MAYBE,
}

/**
 * Helper to check if an option should always be visible
 */
export function isAlwaysVisibleOption(adminId: string): boolean {
  return adminId === YesNoOptionAdminId.YES || adminId === YesNoOptionAdminId.NO
}

/**
 * Helper to check if an option is optional (can be toggled)
 */
export function isOptionalOption(adminId: string): boolean {
  return (
    adminId === YesNoOptionAdminId.NOT_SURE ||
    adminId === YesNoOptionAdminId.MAYBE
  )
}
