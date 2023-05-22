# Shared Target location code

This package is meant to build a common shared codebase that might be reused by other implementation (especially Tycho).

Therefore there are some restrictions:

1. no references to any m2e / pde specific classes beside from this package
2. If anything changes here, and unless Tycho can use the artifact directly (see https://github.com/eclipse-m2e/m2e-core/discussions/1388) any code changes here must possibly synchronized with Tycho
3. Keep dependencies low, currently only BND, maven/aether and of course java itself should be used here