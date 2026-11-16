<p align="center"><img src="icon.png" width="96" alt="BingoCraft"></p>

# BingoCraft

Item bingo for Minecraft (Fabric 1.21.1).

> 🚧 Early development — the game modes below are planned, not playable yet.

## Game modes

### Race
Create a world, start a bingo and race your friends — solo or in teams — on a shared card.
The first player or team to complete the card (a line or the whole card, your choice) wins.

### Endless
Install it on any server, modded or not, and play your survival as usual. Every player has
their own card in the background. Complete it to earn a reward, and a fresh card is dealt.
Forever.

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
| `/bingo card` | Show your card in chat |
| `/bingo version` | Mod version and build type |
| `/bingo reload` | Reload `config/bingocraft.json` (OP) |

## Adding items (datapacks & modpacks)

Goals come from JSON files under `data/<namespace>/bingo_pools/`. Every file found is merged:

```json
{ "difficulty": "hard", "items": ["minecraft:trident", "create:brass_ingot"] }
```

- `difficulty`: `easy`, `medium` or `hard`.
- Items of mods that are not installed are ignored, so one file can cover optional mods.
- To remove the vanilla goals, override `data/bingocraft/bingo_pools/vanilla_<difficulty>.json`
  with an empty `items` list.
- `/reload` reloads the pools; cards already dealt keep their goals.

## License

MIT — see [LICENSE](LICENSE).
