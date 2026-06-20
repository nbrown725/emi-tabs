# EMI Tabs

An [EMI](https://emi.dev/) add-on for Minecraft **1.21.1** that adds a row of creative-mode tabs above EMI's item list.

Works on both **Fabric** and **NeoForge**

## Requirements

- Minecraft 1.21.1
- [EMI](https://modrinth.com/mod/emi)
- Fabric Loader + Fabric API, **or** NeoForge (21.1.x)

## Building

The build uses JDK 21 and Gradle 8.12 (via the wrapper).

```bash
./gradlew build
```

Outputs:

- `fabric/build/libs/EmiTabs-fabric-<version>.jar`
- `neoforge/build/libs/EmiTabs-neoforge-<version>.jar`


### Running in dev mode

```bash
./gradlew :fabric:runClient      # Fabric
./gradlew :neoforge:runClient    # NeoForge
```
## Known Issues

- Non-default EMI settings unsupported
  - The tabs persist if you remove the right side panel
  - The tabs do not work when the panel is in other posistions