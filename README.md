<p align="center"><img src="icon.png" width="96" alt="BingoCraft"></p>

# BingoCraft

Item bingo for Minecraft (Fabric 1.21.1).

> 🚧 Early development (alpha).

## Game modes

A new world starts with bingo **off**. An OP picks the mode with `/bingo mode` (a menu) or
`/bingo mode endless|race|off`.

### Race
Pick teams and race on the same 5x5 card. The first team to fill a **row, a column or a diagonal** wins.

1. `/bingo mode race` (OP).
2. Everyone runs `/bingo team` and clicks a wool (8 teams) or the ender eye to spectate.
3. An OP runs `/bingo start`: 10-second countdown, then every racer is cleared, put in survival
   and dropped from the top of the world at their team's random spot (never over water), with
   1 minute of fall protection, regeneration and saturation. Spectators stay where they are and
   can teleport to players with the vanilla spectator menu.
4. 30 seconds after the win (last 10 counted down), everybody is sent back to spawn in creative
   mode, teams are dissolved and bingo is switched off.

Teams are locked during a race. `/bingo stop` (OP) cancels it.

### Endless
Install it on any server, modded or not, and play your survival as usual. Every player has
their own card in the background (`/bingo card` to look at it). Complete it to earn a random
reward, and a fresh card is dealt. Forever.

Rewards come from the loot table `data/bingocraft/loot_table/rewards/card_complete.json`:
override it with a datapack to change them. Turn it off with `/bingo mode off`.

## Requirements

- Minecraft **1.21.1**
- Fabric Loader **0.16.5+**
- [Fabric API](https://modrinth.com/mod/fabric-api)
- Optional: [Mod Menu](https://modrinth.com/mod/modmenu) + [Cloth Config](https://modrinth.com/mod/cloth-config) for the in-game config screen

Install on **both** the client and the server.

## Download

Grab the jar from the [Releases](https://github.com/Remilulz91/BingoCraft/releases) page.

## Commands

| Command | Who | Description |
|---|---|---|
| `/bingo card` | everyone | Open your card (your team's card during a race) |
| `/bingo team` | everyone | Pick a race team or spectate (race mode) |
| `/bingo version` | everyone | Mod version and build type |
| `/bingo mode [endless\|race\|off]` | OP | Pick the game mode (menu without argument) |
| `/bingo start` | OP | Start a race |
| `/bingo stop` | OP | Cancel the race |
| `/bingo reload` | OP | Reload `config/bingocraft.json` |

## Excluding items (datapacks & modpacks)

Cards are pure random: any item of the game can show up, modded items included. Items a survival
player cannot get are listed in the item tag `#bingocraft:excluded`. To exclude more (e.g. a mod's
creative-only items), add a datapack file `data/bingocraft/tags/item/excluded.json`:

```json
{ "replace": false, "values": ["somemod:creative_wand", "#somemod:debug_items"] }
```

`/reload` applies it to the next cards dealt; cards already dealt keep their goals.

## License

MIT — see [LICENSE](LICENSE).
