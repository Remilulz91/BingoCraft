<p align="center"><img src="icon.png" width="96" alt="BingoCraft"></p>

# BingoCraft

Item bingo for Minecraft (Fabric 1.21.1).

> 🚧 Early development — endless mode is playable, race mode is coming.

## Game modes

### Race (planned)
Create a world, start a bingo and race your friends — solo or in teams — on a shared card.
The first player or team to complete the card (a line or the whole card, your choice) wins.

### Endless (available)
Install it on any server, modded or not, and play your survival as usual. Every player has
their own card in the background (`/bingo card` to look at it). Complete it to earn a random
reward, and a fresh card is dealt. Forever.

Rewards come from the loot table `data/bingocraft/loot_table/rewards/card_complete.json`:
override it with a datapack to change them. Turn the mode off with `endlessMode: false` in
`config/bingocraft.json`.

## Requirements

- Minecraft **1.21.1**
- Fabric Loader **0.16.5+**
- [Fabric API](https://modrinth.com/mod/fabric-api)
- Optional: [Mod Menu](https://modrinth.com/mod/modmenu) + [Cloth Config](https://modrinth.com/mod/cloth-config) for the in-game config screen

Install on **both** the client and the server.

## Download

Grab the jar from the [Releases](https://github.com/Remilulz91/BingoCraft/releases) page.

## Commands

| Command | Description |
|---|---|
| `/bingo card` | Open your card |
| `/bingo version` | Mod version and build type |
| `/bingo reload` | Reload `config/bingocraft.json` (OP) |

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
