

import PhotoSwipeLightbox from 'photoswipe/lightbox';
import 'photoswipe/style.css';

// Define an interface for your S3 image data
interface S3ImageData {
  src: string;
  width: number;
  height: number;
  alt?: string;
}
const s3Images: S3ImageData[] = [
  {
    src: 'http://civiform-local-s3-public.s3.localhost.localstack.cloud:4566/program-summary-image/program-48/temp.jpg',
    width: 1200,
    height: 800,
    alt: 'S3 Image 1'
  },
  {
    src: 'http://civiform-local-s3-public.s3.localhost.localstack.cloud:4566/program-summary-image/program-48/temp.jpg',
    // msrc: 'https://your-bucket-name.s3.amazonaws.com', // Optional thumbnail

    width: 1200,
    height: 800,
    alt: 'Description of image 2',
  }

  // Add more S3 objects here
];


export function init() {
  const lightbox = new PhotoSwipeLightbox({
    gallery: '#swiping-image-gallery',
    children: 'a',
    dataSource: s3Images,
    pswpModule: () => import('photoswipe')
  });

  lightbox.init();
}
