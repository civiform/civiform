import {getUniqueName} from './file_upload'

describe('getUniqueName', () => {
  it('finds a unique name', () => {
    const existingNames = ['file.txt', 'file-2.txt']
    expect(getUniqueName('file.txt', existingNames)).toEqual('file-3.txt')
  })

  it('returns same name if it is already unique', () => {
    const existingNames = ['file.txt', 'file-1.txt']
    expect(getUniqueName('unique.txt', existingNames)).toEqual('unique.txt')
  })

  it('works on name without a file extension', () => {
    const existingNames = ['file']
    expect(getUniqueName('file', existingNames)).toEqual('file-2')
  })

  it('works on name with multiple file extensions ', () => {
    const existingNames = ['file.txt.jpg']
    expect(getUniqueName('file.txt.jpg', existingNames)).toEqual(
      'file.txt-2.jpg',
    )
  })

  it('works on name with digits in the middle ', () => {
    const existingNames = ['file-2-test-4.png']
    expect(getUniqueName('file-2-test-4.png', existingNames)).toEqual(
      'file-2-test-5.png',
    )
  })

  it('works on name with - and no number ', () => {
    const existingNames = ['file-test.png']
    expect(getUniqueName('file-test.png', existingNames)).toEqual(
      'file-test-2.png',
    )
  })

  it('increments existing number if uploading file with a suffix', () => {
    const existingNames = ['file.txt', 'file-4.txt']
    expect(getUniqueName('file-4.txt', existingNames)).toEqual('file-5.txt')
  })

  it('works on name without a file extension, but with an existing number ', () => {
    const existingNames = ['file', 'file-4']
    expect(getUniqueName('file-4', existingNames)).toEqual('file-5')
  })

  it('returns same name if array is empty', () => {
    expect(getUniqueName('unique.txt', [])).toEqual('unique.txt')
  })
})
