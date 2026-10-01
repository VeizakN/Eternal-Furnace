# Eternal Furnace — Minecraft 1.20.1 / Forge

This branch preserves the legacy **Eternal Furnace 1.0.2** source for **Minecraft 1.20.1**.

- **Minecraft:** 1.20.1
- **Forge:** 47.3.0
- **Java:** 17
- **Mod version:** 1.0.2

The current NeoForge version lives on [`main`](https://github.com/VeizakN/Eternal-Furnace/tree/main).

## Historical behavior

This version uses a normal shapeless recipe for Hellfire Netherrack, so **Flint and Steel is consumed by crafting**. That was the behavior of the 1.20.1 release and is preserved here as-is. The current version fixes this so crafting costs durability instead.

## Building

On Windows:

```bat
gradlew.bat build
```

On Linux/macOS:

```bash
./gradlew build
```

The built JAR will be placed in `build/libs/`.

## Download

[Download Eternal Furnace on Modrinth](https://modrinth.com/mod/eternal-furnace)

## License

Licensed under the [MIT License](LICENSE).
