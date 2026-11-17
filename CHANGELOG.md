# Changelog

All notable changes to BingoCraft will be documented in this file.

## [0.3.0-alpha.1] — 2026-11-17

### Added
- **Endless mode** (on by default, `endlessMode` in the config): every player always has a card.
  Players without one are dealt a card automatically (first join included).
- Completing a card gives a **random reward** from the loot table
  `bingocraft:rewards/card_complete` (2-3 rolls: ores, diamonds, emeralds, XP bottles, enchanted
  books and gear, golden apples… and rarely a totem or an enchanted golden apple), then deals a
  **new card** right away, forever. Overridable by datapack.
- A new card never contains an item the player is carrying when it is dealt (reward items
  included), so it never starts with squares already ticked.
- Completed cards are counted per player, announced to the server and shown in the card view.
- Chat message with a clickable **[Open]** link when a card is dealt.

### Fixed
- A goal could be ticked on the wrong card if a new card replaced the old one during a check.

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
