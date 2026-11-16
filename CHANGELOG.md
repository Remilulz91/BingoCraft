# Changelog

All notable changes to BingoCraft will be documented in this file.

## [0.2.0-alpha.1] — 2026-11-16

### Added
- **Bingo cards**: 3x3, 4x4 or 5x5 grids of items to obtain, one card per player, saved with the world.
- **Goal pools** by difficulty (easy / medium / hard) loaded from datapacks:
  `data/<namespace>/bingo_pools/*.json`. Vanilla pools ship with 72 easy, 82 medium and 45 hard items.
  Items of mods that are not installed are skipped, so modpacks can add their own items safely.
- New cards follow a configurable difficulty mix (default 40 % easy / 40 % medium / 20 % hard).
- **Automatic detection**: a goal is ticked as soon as its item is in the player's inventory
  (picked up, crafted, smelted, traded…). The item is not consumed.
- `/bingo card` shows your card in chat (hover a square to see its item).
- Debug commands: `/bingo debug pools`, `new [size]`, `complete <cell>`, `clear`.
- Config: card size, difficulty mix, inventory check interval.

## [0.1.0-alpha.1] — 2026-11-16

### Added
- Project skeleton: Fabric 1.21.1 mod with public / debug build variants.
- `/bingo version`, `/bingo reload` (OP) and `/bingo debug info` (debug build only).
- Configuration file `config/bingocraft.json`, Mod Menu + Cloth Config screen.
- English and French translations.
- GitHub Actions: build on every push, GitHub Release with the jar on every `v*` tag.
