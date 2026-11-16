# Changelog

All notable changes to BingoCraft will be documented in this file.

## [0.2.0-alpha.1] — 2026-11-16

### Added
- **Bingo cards**: 3x3, 4x4 or 5x5 grids of items to obtain, one card per player, saved with the world.
- **Pure random cards**: goals are drawn from every item of the game, vanilla and modded, with no
  difficulty tiers — a card can be trivial or brutal.
- Items a survival player cannot obtain are excluded through the item tag `#bingocraft:excluded`
  (bedrock, command blocks, spawn eggs, infested blocks…). Datapacks and modpacks can add their
  own creative-only items to it. Disabled experimental items are excluded too.
- **Automatic detection**: a goal is ticked as soon as its item is in the player's inventory
  (picked up, crafted, smelted, traded…). The item is not consumed. A completed card is
  announced to the whole server.
- `/bingo card` opens your card in a double chest view (read-only, updates live).
- Debug commands: `/bingo debug pool`, `new [size]`, `complete <cell>`, `clear`.
- Config: card size, inventory check interval.

## [0.1.0-alpha.1] — 2026-11-16

### Added
- Project skeleton: Fabric 1.21.1 mod with public / debug build variants.
- `/bingo version`, `/bingo reload` (OP) and `/bingo debug info` (debug build only).
- Configuration file `config/bingocraft.json`, Mod Menu + Cloth Config screen.
- English and French translations.
- GitHub Actions: build on every push, GitHub Release with the jar on every `v*` tag.
