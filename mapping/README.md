# mapping module

This module generates `mapping/*.json` resources at build time using Java code.

## Main tasks

- `generateMappings`: downloads the Mojang version manifest and the required `server.jar` files, runs the data generator, and produces mapping resources.
- `mappingResourcesJar`: packages the generated `mapping/**` resources as a dedicated artifact consumed by the `plugin` module.

## Resource source

Seed and fallback files are stored in `src/main/seeds/mapping/`.

## Consumption

The `plugin` module consumes this module's resource artifact through the `mappingResourcesElements` configuration and merges it into the final plugin package during `processResources`.

