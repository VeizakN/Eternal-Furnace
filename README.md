# Eternal Furnace

[![CI](https://github.com/VeizakN/Eternal-Furnace/actions/workflows/ci.yml/badge.svg)](https://github.com/VeizakN/Eternal-Furnace/actions/workflows/ci.yml)

Give Netherrack a purpose.

**Eternal Furnace** adds **Hellfire Netherrack**: place it directly beneath a regular Furnace and it becomes an infinite fuel source. The trade-off is speed — Hellfire-powered smelting takes **1.8× longer** than the recipe's normal cooking time.

[Download on Modrinth](https://modrinth.com/mod/eternal-furnace)

## Features

- Infinite fuel for the regular vanilla Furnace
- Simple recipe: **Flint and Steel + Netherrack**
- Flint and Steel loses only one durability point when crafting
- Hellfire smelting is balanced at **1.8× normal cooking time**
- Existing burning fuel is allowed to finish before Hellfire takes over
- Fuel waiting in the fuel slot is not consumed while Hellfire can power the Furnace
- Removing Hellfire restores vanilla Furnace behavior
- Cooking progress is proportionally preserved when switching between normal and Hellfire timing
- Recipes with custom cooking times are supported
- Smokers and Blast Furnaces are intentionally unaffected
- Works in singleplayer and multiplayer

## Current source

The current `main` branch targets:

- **Minecraft 26.2**
- **NeoForge 26.2.0.88+**
- **Java 25**

The older Minecraft 1.20.1 / Forge release is available on [Modrinth](https://modrinth.com/mod/eternal-furnace).

## Building

Clone the repository and run:

```bash
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

The project uses the Gradle Wrapper and a Java 25 toolchain. The Foojay toolchain resolver configured by the project can provision the required JDK when supported by your environment.

## Tests

The 26.2 port includes GameTests covering the important furnace and crafting behavior, including:

- Hellfire fuel handoff
- Invalid and blocked inputs
- Ordinary fuel finishing before Hellfire takeover
- Proportional cooking-time changes
- Furnace-only behavior
- Durable Flint and Steel crafting
- Persistence and concurrent furnaces
- Recipe changes with different cooking times

See [`PORT_NOTES.md`](PORT_NOTES.md) for the full port and test notes.

## License

Licensed under the [MIT License](LICENSE).
