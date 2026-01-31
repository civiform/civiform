// USWDS typescript module declaration for imports
declare module '@uswds/uswds/js/usa-modal' {
  export interface USWDSModal {
    toggleModal: (event: {target: HTMLElement; type: string}) => boolean
    teardown: (element: HTMLElement) => void
  }

  const modal: USWDSModal
  export default modal
}
