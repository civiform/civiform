/* eslint-disable @typescript-eslint/no-unsafe-argument */
/* eslint-disable @typescript-eslint/no-unsafe-assignment */
/* eslint-disable @typescript-eslint/no-unsafe-member-access */
/* eslint-disable @typescript-eslint/no-unsafe-call */

import {glob} from 'glob'
import fs = require('fs')
import path = require('path')
import sharp = require('sharp')

async function globalTeardown() {
  // Copy actual image to updated_snapshots folder
  const updatedSnapshots = await glob('./tmp/test-output/**/*-actual.png', {})

  for (let i = 0; i < updatedSnapshots.length; i++) {
    const originalFile = updatedSnapshots[i]
    const newFile = path.join(
      './updated_snapshots',
      path.basename(path.dirname(originalFile)),
      path.basename(path.dirname(path.dirname(originalFile))),
      path.basename(originalFile.replace('-actual.png', '-received.png')),
    )

    // Make sure the parent directory structure exists and copy file over
    fs.mkdirSync(path.dirname(newFile), {recursive: true})
    fs.copyFileSync(originalFile, newFile)
  }

  // Stitch together image from expect, diff, and actual images and place in diff_snapshots
  const diffSnapshots = await glob('./tmp/test-output/**/*-diff.png', {})

  for (let i = 0; i < diffSnapshots.length; i++) {
    const diffFile = diffSnapshots[i]
    const expectedFile = diffFile.replace('-diff.png', '-expected.png')
    const actualFile = diffFile.replace('-diff.png', '-actual.png')

    // Determine the max height based on the three images
    const expectedMetadata = await sharp(expectedFile).metadata()
    const diffMetadata = await sharp(diffFile).metadata()
    const actualMetadata = await sharp(actualFile).metadata()
    const canvasHeight = Math.max(
      expectedMetadata.height ?? 0,
      diffMetadata.height ?? 0,
      actualMetadata.height ?? 0,
    )

    // Determine the left position for the second and third images
    const diffFileLeft = expectedMetadata.width ?? 0
    const actualFileLeft = diffFileLeft + (diffMetadata.width ?? 0)

    // Make the new file path
    const newFile = path.join(
      './diff_output',
      path.basename(path.dirname(diffFile)),
      path.basename(path.dirname(path.dirname(diffFile))),
      path.basename(diffFile),
    )

    // Make sure the parent directory structure exists
    fs.mkdirSync(path.dirname(newFile), {recursive: true})

    // Stitch images together
    const imageWidth = 1280

    sharp({
      create: {
        width: imageWidth * 3,
        height: canvasHeight,
        channels: 4, // RGBA
        background: {r: 255, g: 255, b: 255, alpha: 1},
      },
    })
      .composite([
        {input: expectedFile, left: 0, top: 0},
        {input: diffFile, left: diffFileLeft, top: 0},
        {input: actualFile, left: actualFileLeft, top: 0},
      ])
      .png()
      .toFile(newFile, (err: Error) => {
        if (err) {
          console.error('Error merging images:', err)
        }
      })
  }
}

export default globalTeardown
