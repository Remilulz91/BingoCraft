# Changelog

All notable changes to BingoCraft will be documented in this file.

## [0.5.0-alpha.1] — 2026-11-17

### Added
- The last 10 seconds before the return to spawn are counted down in the action bar, with a tick sound.
- Team picker: offline members are greyed out and marked "(offline)" — they keep their team but
  only online players race.

### Changed
- Race: **diagonals win again** — first full row, column or diagonal.
- After a race everybody is sent back to spawn in **creative** mode and bingo is switched
  **off** (`/bingo mode` to start again): everything restarts from scratch.
- Dev runs (`./gradlew runClient`) use a fixed player name (`Remilulz_91`, or
  `-PdevUsername=<name>`) instead of Loom's random `PlayerNNN`, so the same offline UUID — card,
  team, inventory — comes back every launch.

## [0.4.0-alpha.1] — 2026-11-17

### Added
- **Game mode per world**, picked by an OP with `/bingo mode` (menu) or
  `/bingo mode endless|race|off`. A new world starts **off**: no card is dealt and nothing is
  tracked until a mode is chosen. Saved with the world.
- **Race mode**:
  - `/bingo team`: a chest with one wool per team (8 colours) and an ender eye to spectate.
    The choice glows and member lists update live. Spectators join no team.
  - `/bingo start` (OP) refuses while an online player has neither picked a team nor chosen to
    spectate, and names them. Teams are locked from then until the end of the race.
  - Same 5x5 card for every team; the **first full row or column wins** (diagonals don't count).
  - 10-second countdown with titles, then GO: every racer is cleared, put in survival and
    dropped from the top of the world at their team's random spot (team mates a few blocks
    apart; within `raceSpawnRadius` of the world spawn; never over an ocean, river, beach or
    lake), with `raceProtectionSeconds` (60) of fall protection, regeneration and saturation.
  - Spectators switch to spectator mode where they are and can teleport to any player with the
    vanilla spectator menu.
  - A team learns which item it ticked; the other teams only that it scored.
  - Win: title, race time and harmless fireworks on the winning team. 30 seconds later everybody
    is sent back to the world spawn (spectators back in survival) and the teams are dissolved.
  - `/bingo stop` (OP) cancels the race and sends everybody back to spawn right away.
  - The card view shows the goal, your team and the race clock, updated every second.
  - Racers offline at GO are dropped in when they come back; the race survives a restart.
- Endless mode: **BINGO!** title and harmless fireworks when a card is completed.

### Changed
- Every card is 5x5 in both modes (`cardSize` removed). `/bingo debug new [size]` keeps smaller
  cards for quick tests.
- `endlessMode` removed from the config: the mode is now chosen in game with `/bingo mode`.
- Player commands: `/bingo card`, `/bingo team`, `/bingo version`. OP only: `/bingo mode`,
  `start`, `stop`, `reload`, `debug`.

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
