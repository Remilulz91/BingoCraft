# BingoCraft — Developer Setup

## Prerequisites

- **Java 21** (Temurin recommended)
- A Java IDE (IntelliJ IDEA has first-class Fabric Loom support)

## Build

```bash
./gradlew build                          # PUBLIC JAR
./gradlew clean build -PbuildType=debug  # DEBUG JAR
```

Output in `build/libs/bingocraft-<version>[-debug].jar`.

## Run in dev

```bash
./gradlew runClient   # client with the mod loaded (DEBUG build by default)
./gradlew runServer   # dedicated server with the mod loaded (DEBUG build by default)
```

Only resource changes hot-reload; restart Minecraft for Java changes.

## Project conventions

- Debug features are gated on `BingoCraft.isDebugBuild()` — always guard behind this.
- Localization keys live in `assets/bingocraft/lang/{en_us,fr_fr}.json` — keep both in sync.
- Cloth Config classes are only referenced from `client/config/BingoCraftConfigScreen` (Cloth Config is optional).
- Config options are added together with the code that reads them — no dead options.

## Build variants (Public vs Debug)

| | Public | Debug |
|---|---|---|
| `/bingo debug ...` available | ❌ | ✓ (OP) |

The build type is baked into the JAR (`bingocraft.build.properties`); nothing in the config file can
enable debug commands in a PUBLIC build. `runClient` / `runServer` default to DEBUG.

## Project layout

```
src/main/
├── java/fr/bingocraft/
│   ├── BingoCraft.java                 (main entry, build detection)
│   ├── config/BingoCraftConfig.java    (config/bingocraft.json)
│   ├── commands/                       (BingoCommand, DebugCommand)
│   └── client/                         (BingoCraftClient, config/ModMenuIntegration + BingoCraftConfigScreen)
└── resources/
    ├── fabric.mod.json
    ├── bingocraft.build.properties
    └── assets/bingocraft/              (icon.png, lang/)
```

## Releasing a new version

1. Bump `mod_version` in `gradle.properties`.
2. Add a `CHANGELOG.md` entry with the heading `## [<version>] — <date>`.
3. Commit + push, then tag: `git tag v<version> && git push origin v<version>`.
4. The `Release` workflow creates the GitHub Release with the public jar
   (alpha / beta versions are marked as pre-releases).

## DEBUG commands (DEBUG build only)

| Command | Description |
|---|---|
| `/bingo debug info` | Print runtime debug info |
