import {defineConfig} from 'vite'
import {viteStaticCopy} from 'vite-plugin-static-copy'
import path from 'path'

export default defineConfig({
  server: {
    port: 5173,
    host: '0.0.0.0', // Listen on all network interfaces for Docker
    cors: true,
    strictPort: true,
    fs: {
      // Allow serving files from node_modules and project root
      allow: [
        path.resolve(__dirname, 'app/assets'),
        path.resolve(__dirname, 'node_modules'),
      ],
    },
    watch: {
      ignored: [
        '**/target/**',
        '**/.*', // hidden files
        '**/*.java', // java source files
        '**/*.class', // compiled java
        '**/*.jar',
        '**/conf/**', // play config files
        '**/project/**', // sbt project files
        '**/logs/**',
        '**/app/assets/dist/**',
      ],
    },
  },

  clearScreen: false,

  build: {
    // Output to the directory SBT expects
    outDir: 'app/assets/dist',
    minify: 'esbuild',
    // Generate source maps
    sourcemap: true,
    // Don't clear the output directory (SBT manages this)
    emptyOutDir: false,
    chunkSizeWarningLimit: 1100,
    // Configure rollup options for multiple entry points
    rollupOptions: {
      input: {
        applicant: path.resolve(
          __dirname,
          'app/assets/javascripts/applicant_entry_point.ts',
        ),
        admin: path.resolve(
          __dirname,
          'app/assets/javascripts/admin_entry_point.ts',
        ),
        uswds_js: path.resolve(
          __dirname,
          'node_modules/@uswds/uswds/dist/js/uswds.min.js',
        ),
        uswds_css: path.resolve(
          __dirname,
          'app/assets/stylesheets/uswds/styles.scss',
        ),
        northstar_css: path.resolve(
          __dirname,
          'app/assets/stylesheets/northstar/styles.scss',
        ),
        maplibregl_css: path.resolve(
          __dirname,
          'node_modules/maplibre-gl/dist/maplibre-gl.css',
        ),
        tailwind: path.resolve(__dirname, 'app/assets/stylesheets/styles.css'),
      },
      output: {
        // Use [name].bundle.js format to match webpack output
        entryFileNames: '[name].bundle.js',
        chunkFileNames: '[name].chunk.js',
        assetFileNames: (assetInfo) => {
          if (assetInfo.names?.[0]?.endsWith('.css')) {
            return '[name].min.css'
          }
          // Keep fonts and images in their respective folders
          if (assetInfo.names?.[0]?.match(/\.(woff2?|ttf|eot)$/)) {
            return 'fonts/[name][extname]'
          }
          if (assetInfo.names?.[0]?.match(/\.(png|jpe?g|svg|gif|webp)$/)) {
            return 'img/[name][extname]'
          }
          return '[name][extname]'
        },
        manualChunks: {
          // Split vendor dependencies
          'vendor-htmx': ['htmx.org'],
          'vendor-markdown': ['markdown-it', 'dompurify'],
          'vendor-maps': ['maplibre-gl'],
        },
      },
      onwarn(warning, warn) {
        // Suppress eval warnings from htmx
        if (warning.code === 'EVAL' && warning.id?.includes('htmx')) return
        warn(warning)
      },
    },
    // Target modern browsers (no IE11)
    target: 'es2022',
  },

  css: {
    preprocessorOptions: {
      scss: {
        quietDeps: true,
        // Add USWDS load paths for SCSS imports
        loadPaths: [
          path.resolve(__dirname, 'app/assets/stylesheets/uswds'),
          path.resolve(__dirname, 'node_modules/@uswds/uswds/packages'),
        ],
      },
    },
  },

  resolve: {
    extensions: ['.ts', '.js', '.scss', '.css'],
  },

  // plugins: [
  //   // Copy swagger-ui-dist to public/swagger-ui
  //   viteStaticCopy({
  //     targets: [
  //       {
  //         src: 'node_modules/swagger-ui-dist/*',
  //         dest: '../public/swagger-ui',
  //       },
  //     ],
  //   }),
  // ],
})
