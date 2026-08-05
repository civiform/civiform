# File format

k6 supports js and ts, though ts will be [transpiled](https://esbuild.github.io/content-types/#typescript) to js and drop some functionality. We use ts currently to easily hook into bin/fmt. We could add js support but we have existing js files and this is more expedient and valid.
