# Create: Brassworks Missions

[![Modrinth](https://wsrv.nl/?url=https%3A%2F%2Fcdn.bypixel.dev%2Fraw%2F4dlsHJ.png&n=-1)](https://modrinth.com/mod/create-brassworks-missions)
[![CurseForge](https://i.ibb.co/F4MtFnv7/cozy-64h.png)](https://www.curseforge.com/minecraft/mc-mods/create-brassworks-missions)
[![GitHub](https://i.ibb.co/DPnZdBK5/41dfe80a399c0c8466f8cecc9f35048de5066e11-1.png)](https://github.com/Brassworks-smp/Create-Brassworks-Missions)
[![Discord](https://wsrv.nl/?url=https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/discord-plural_vector.svg&h=64)](https://discord.com/invite/nDhkgzAPR2)

[![CodeFactor](https://www.codefactor.io/repository/github/brassworks-smp/create-brassworks-missions/badge)](https://www.codefactor.io/repository/github/brassworks-smp/create-brassworks-missions)
[![GitHub issues](https://img.shields.io/github/issues/Brassworks-smp/Create-Brassworks-Missions.svg)](https://github.com/Brassworks-smp/Create-Brassworks-Missions/issues)
[![GitHub commits](https://img.shields.io/github/commit-activity/t/Brassworks-smp/Create-Brassworks-Missions/master.svg)](https://github.com/Brassworks-smp/Create-Brassworks-Missions/commits)
![Lines of Code](https://img.shields.io/endpoint?url=https://ghloc.vercel.app/api/Brassworks-smp/Create-Brassworks-Missions/badge&color=2ecc71)

Create: Brassworks Missions adds a data driven mission system to Minecraft 1.21.1.
It was made for the [Brassworks SMP](https://modrinth.com/modpack/brassworks-smp-modpack), but can be used in any modpack.

## Features

- Create style mission menu and HUD
- Open the menu with H or `/missions`
- 29 mission types for Vanilla and Create
- Custom missions through datapacks
- Admin commands for creating and managing missions
- Random or progressive rewards for each mission
- Unclaimed rewards survive mission resets
- Configurable number of active missions
- Singleplayer and multiplayer support

<img width="1408" height="683" alt="Brassworks Missions interface" src="https://github.com/user-attachments/assets/4ae53084-d48a-4847-81cf-da010cfdcfa6" />

## Requirements

- Minecraft 1.21.1
- NeoForge
- Create 6.0.6 or newer
- Create: Numismatics is optional

Numismatics is only needed when one of its items is used as the mission reward.

## Datapack missions

Mission files are loaded from:

```text
data/brassworksmissions/missions/*.json
```

Each file contains a JSON array:

```json
[
  {
    "id": "mypack:iron_shift",
    "type": "brassworksmissions:smelt_item",
    "icon": "minecraft:blast_furnace",
    "weight": 4,
    "titles": ["Iron Shift", "Hot Work"],
    "requirement": {
      "requirementType": "item",
      "item": "minecraft:iron_ingot",
      "minAmount": 16,
      "maxAmount": 64
    },
    "reward": {
      "minAmount": 3,
      "maxAmount": 12,
      "mode": "progressive"
    }
  }
]
```

`id` identifies the mission template. `type` selects the mission logic. If `type` is missing, the mod uses `id` as the type so older datapacks still work.

`icon` is optional and only controls the icon shown in the menu. Without it, the selected target item is used. Multiple entries in `titles` are picked randomly when the mission is assigned.

Reward modes:

- `random` rolls a value between `minAmount` and `maxAmount`
- `progressive` increases the reward with the rolled mission difficulty

The reward item is configured in:

```text
data/brassworksmissions/missions/mission_reward.json
```

```json
{
  "item": "numismatics:spur"
}
```

## Mission management

Operators can open the clickable management menu with:

```text
/missions manage
```

Main commands:

```text
/missions manage list [page]
/missions manage types [page]
/missions manage show <id>
/missions manage disable <id>
/missions manage enable <id>
/missions manage delete <id>
/missions manage icon <id> <icon>
/missions manage titles <id> <title_or_json>
/missions manage reward_mode <id> <random|progressive>
/missions manage reload
```

Create a mission:

```text
/missions manage create <type> [target] <icon> <min> <max> <reward_min> <reward_max> <weight> <random|progressive> <title_or_json>
```

Example:

```text
/missions manage create brassworksmissions:break_block minecraft:stone minecraft:diamond_pickaxe 32 64 4 8 4 progressive ["Stone Breaker","Mining Duty"]
```

The target is only used by item, block and entity missions. Level, distance and count missions omit it. The icon is always an item and is only used for display. `requirementType` is selected automatically.

Target and icon arguments support tab completion. Block missions suggest block items, item missions suggest items, entity missions suggest entity IDs with spawn eggs and icons suggest all registered items.

The mission ID is created automatically. If it already exists, the mod adds a number.

Managed missions are saved in:

```text
config/brassworksmissions/managed_missions.json
```

Datapack files are never changed by the commands. Disabling a datapack mission only hides it through the management file.

Completed rewards that were not collected are stored before a mission reset. They remain available through Claim All after the reset and after restarting the world.

## Active mission count

The number of active missions is set in the server config:

```toml
missionSlotCount = 6
```

Valid values are 1 to 32. After changing the value, players receive the new number of missions the next time they join.

## License

Licensed under the [MIT License](./LICENSE). Credit is not required, but acknowledgment is appreciated.

Contributions, suggestions and pull requests are welcome.
