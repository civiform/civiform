# Docs

Usage docs are [in the wiki](https://github.com/civiform/civiform/wiki/Performance-testing-%E2%80%90-k6).

# File format

k6 supports js and ts, though ts will be [transpiled](https://esbuild.github.io/content-types/#typescript) to js and drop some functionality. We use ts currently to easily hook into bin/fmt. We could add js support but we have existing js config files which we may not want to format and this is more expedient and equally valid.
