import {defineConfig} from 'vite'
import {viteStaticCopy} from 'vite-plugin-static-copy'
import path from 'path'

// Asset paths to reference in the below config
const assetPaths = {
  applicant: 'app/assets/javascripts/applicant_entry_point.ts',
  admin: 'app/assets/javascripts/admin_entry_point.ts',
  uswds_css: 'app/assets/stylesheets/uswds/styles.scss',
  uswds_js: 'node_modules/@uswds/uswds/dist/js/uswds.min.js',
  uswdsinit_js: 'node_modules/@uswds/uswds/dist/js/uswds-init.min.js',
  northstar_css: 'app/assets/stylesheets/northstar/styles.scss',
  tailwind: 'app/assets/stylesheets/styles.css',
  maplibregl_css: 'node_modules/maplibre-gl/dist/maplibre-gl.css',
  swaggerui_css: 'node_modules/swagger-ui-dist/swagger-ui.css',
  swaggerui_js: 'node_modules/swagger-ui-dist/swagger-ui-bundle.js',
  swaggeruipreset_js:
    'node_modules/swagger-ui-dist/swagger-ui-standalone-preset.js',
}

export default defineConfig({
  // Server options are for the dev server and are for local development
  server: {
    port: 5173,
    host: '0.0.0.0', // Listen on all network interfaces for Docker
    cors: true,
    strictPort: true, // Make Vite error if the default port is in use, otherwise it will try the next port up.
    fs: {
      // Allow serving files from node_modules and project root
      allow: [
        path.resolve(__dirname, 'app/assets'),
        path.resolve(__dirname, 'node_modules'),
      ],
    },
    // Pre-compile on startup for faster loading of the first page hit
    warmup: {
      clientFiles: [
        assetPaths.applicant,
        assetPaths.admin,
        assetPaths.uswdsinit_js,
        assetPaths.uswds_js,
        assetPaths.uswds_css,
        assetPaths.northstar_css,
        assetPaths.maplibregl_css,
        assetPaths.tailwind,
      ],
    },
    watch: {
      // Limit files Vite watches. Prevents extra rebuilds.
      ignored: [
        '**/target/**', // java build output
        '**/.*', // hidden files
        '**/*.java', // java source files
        '**/*.scala', // scala source files
        '**/*.class', // compiled java
        '**/*.html', // html source files
        '**/*.jar', // java jar files
        '**/conf/**', // play config files
        '**/project/**', // sbt project files
        '**/logs/**', // log files
        '**/app/assets/dist/**', // pre-compiled assets
      ],
    },
  },

  // Pre-bundle dependencies for faster cold starts
  optimizeDeps: {
    include: ['htmx.org', 'markdown-it', 'dompurify', 'maplibre-gl'],
  },

  // Prevent the terminal from being cleared
  clearScreen: false,

  // Build options are used for pre-compiling asset output for all other environments
  build: {
    // Output to the location Play will look for assets
    outDir: 'app/assets/dist',
    minify: 'esbuild',
    emptyOutDir: true,
    // Up the warning size limit past our largest chunk
    chunkSizeWarningLimit: 1100,
    // Generate source maps
    sourcemap: true,
    // Set up the main entrypoint chunks
    rollupOptions: {
      input: {
        applicant: path.resolve(__dirname, assetPaths.applicant),
        admin: path.resolve(__dirname, assetPaths.admin),
        uswdsinit_js: path.resolve(__dirname, assetPaths.uswdsinit_js),
        uswds_js: path.resolve(__dirname, assetPaths.uswds_js),
        uswds_css: path.resolve(__dirname, assetPaths.uswds_css),
        northstar_css: path.resolve(__dirname, assetPaths.northstar_css),
        maplibregl: path.resolve(__dirname, assetPaths.maplibregl_css),
        tailwind: path.resolve(__dirname, assetPaths.tailwind),
      },
      // Set up the output file naming conventions
      output: {
        // entryFileNames align with the rollupOptions for javascript files. No hash is
        // on these files so they can have a deterministic name to reference in the
        // application, Play's AssetsFinder will add it.
        entryFileNames: '[name].bundle.js',
        // chunkFileNames align with manualChunks and include a hash in the file name for cache busting purposes
        chunkFileNames: '[hash]-[name].chunk.js',
        // assetFileNames are for non-javascript files
        assetFileNames: (assetInfo) => {
          if (assetInfo.names?.[0]?.endsWith('.css')) {
            return '[name].min.css'
          }
          // Keep fonts in their respective folders for USWDS to load correctly
          if (assetInfo.names?.[0]?.match(/\.(woff2?|ttf|eot)$/)) {
            return 'fonts/[name][extname]'
          }
          // Keep images in their respective folders for USWDS to load correctly
          if (assetInfo.names?.[0]?.match(/\.(png|jpe?g|svg|gif|webp)$/)) {
            return 'img/[name][extname]'
          }

          return '[name][extname]'
        },
        // This splits vendor dependencies into separate chunks
        manualChunks: {
          'vendor-htmx': ['htmx.org'],
          'vendor-markdown': ['markdown-it', 'dompurify'],
          'vendor-maps': ['maplibre-gl'],
        },
      },
      onwarn(warning, warn) {
        // Suppress eval warnings from htmx
        if (warning.code === 'EVAL' && warning.id?.includes('htmx')) {
          return
        }
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

  plugins: [
    // Files that need to be copied to asset folder that don't run through the bundler
    viteStaticCopy({
      targets: [
        {
          src: assetPaths.swaggerui_css,
          dest: 'swagger-ui',
        },
        {
          src: assetPaths.swaggerui_js,
          dest: 'swagger-ui',
        },
        {
          src: assetPaths.swaggeruipreset_js,
          dest: 'swagger-ui',
        },
      ],
    }),
  ],
})
