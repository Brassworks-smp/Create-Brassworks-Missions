# Missions

Create: Brassworks Missions provides a configurable mission system for Minecraft 1.21.1 with separate mission templates, per-player active missions, automatic resets, rerolls, rewards, HUD tracking, and operator management commands.

The mod includes 29 mission types for Vanilla Minecraft and Create. Mission templates are stored in one editable JSON configuration file and can be managed in game without restarting the server.

## System overview

A mission template defines what kind of action should be tracked, which target is valid, how much progress is required, how likely the mission is to be selected, and how large its reward will be.

When missions are assigned to a player, the mod:

1. Selects the correct category pool.
2. Removes disabled and unavailable templates.
3. Chooses templates using their configured weights.
4. Tries to avoid assigning the same template more than once.
5. Randomly selects a title, target, required amount, and reward amount.
6. Stores the resulting active missions in the player's persistent data.

Mission progress, claimed state, selected slot, tracked HUD slots, reset timestamps, and unclaimed stored rewards are synchronized to the client and saved with the player.

## Requirements

- Minecraft 1.21.1
- NeoForge
- Create 6.0.6 or newer
- Create: Numismatics is optional and only required when one of its items is configured as the mission reward

## Mission configuration file

Mission templates are stored in:

```text
config/brassworksmissions/missions.json
```

The file is created automatically from the bundled defaults when it does not exist. Existing `managed_missions.json` files are renamed to `missions.json` automatically.

The top-level format is:

```json
{
  "version": 2,
  "missions": [],
  "disabled": []
}
```

- `version` is the configuration format version.
- `missions` contains all mission templates.
- `disabled` contains mission IDs that remain in the file but cannot be assigned.

Changes made through management commands are written atomically to this file.

## Mission definition

A complete mission template can look like this:

```json
{
  "id": "example:weekly_factory_order",
  "type": "brassworksmissions:mechanical_craft",
  "category": 2,
  "icon": "create:mechanical_crafter",
  "weight": 4,
  "titles": ["Weekly Factory Order", "Precision Assembly"],
  "recipes": ["example:weekly_factory_order"],
  "availabilityRequirements": [
    {
      "type": "brassworksmissions:advancement",
      "data": {
        "advancement": "minecraft:story/iron_tools",
        "completed": true
      }
    }
  ],
  "requirement": {
    "requirementType": "item",
    "item": ["minecraft:piston", "minecraft:observer"],
    "minAmount": 8,
    "maxAmount": 24
  },
  "reward": {
    "minAmount": 4,
    "maxAmount": 12,
    "mode": "progressive"
  }
}
```

### Mission fields

| Field | Required | Description |
| --- | --- | --- |
| `id` | Yes | Unique resource location for the mission template. |
| `type` | No | Registered mission type. When omitted, `id` is also used as the type for compatibility with older definitions. |
| `category` | No | Mission category `1` or `2`. Defaults to category 1. |
| `icon` | No | Item shown in the menu. The selected target item is used when omitted. |
| `weight` | Yes | Positive finite selection weight. Larger values make the template more likely to be selected. |
| `titles` | Yes | One or more non-empty titles. One title is selected randomly when the mission is assigned. |
| `recipes` | No | Recipe IDs checked by `/missions manage diagnose`. Progress still follows the mission type and selected target. |
| `availabilityRequirements` | No | Conditions that must pass before the template can be assigned to a player. |
| `requirement` | Yes | Target, amount range, and display requirement type. |
| `reward` | Yes | Reward amount range and calculation mode. |

### Requirement fields

```json
"requirement": {
  "requirementType": "item",
  "item": "minecraft:iron_ingot",
  "minAmount": 16,
  "maxAmount": 64
}
```

`item` accepts either one item ID or an array of item IDs. When an array is used, one entry is selected randomly for each assigned mission.

`minAmount` must be at least 1, and `maxAmount` must be equal to or greater than `minAmount`. The required amount is rolled inclusively between both values.

The correct target and `requirementType` depend on the selected mission type. Management commands select the requirement type automatically.

### Icons and entity targets

Icons must be registered item IDs. Entity missions use spawn-egg item IDs as their configured targets because the spawn egg identifies the required entity type.

Examples:

```json
"item": "minecraft:zombie_spawn_egg"
```

```json
"icon": "minecraft:diamond_sword"
```

## Mission types

The mod registers 29 mission types: 16 Vanilla types and 13 Create types.

The target column describes the argument expected by `/missions manage create`. Types marked as fixed do not accept a target argument and use a built-in display item.

### Vanilla mission types

| Mission type | Target | Progress |
| --- | --- | --- |
| `brassworksmissions:break_block` | Block item | Breaking the selected block. |
| `brassworksmissions:place_block` | Block item | Placing the selected block. |
| `brassworksmissions:smelt_item` | Item | Taking the selected smelted output. Stack size counts toward progress. |
| `brassworksmissions:pickup_item` | Item | Picking up the selected item. The amount actually collected counts. |
| `brassworksmissions:reach_experience_level` | Fixed | Reaching the rolled experience level. Uses `create:experience_nugget` as its display item. |
| `brassworksmissions:use_totem` | Fixed | Activating a Totem of Undying. |
| `brassworksmissions:fish_item` | Item | Fishing the selected item. Matching drop stack sizes count. |
| `brassworksmissions:craft_item` | Item | Crafting the selected item. Crafted stack size counts. |
| `brassworksmissions:kill_entity` | Spawn egg | Killing the entity represented by the selected spawn egg. |
| `brassworksmissions:breed_animals` | Spawn egg | Breeding the entity represented by the selected spawn egg. |
| `brassworksmissions:consume_item` | Item | Finishing consumption of the selected food, drink, or usable item. |
| `brassworksmissions:walk` | Fixed | Distance travelled while walking. Uses leather boots as its display item. |
| `brassworksmissions:drive_boat` | Fixed | Distance travelled while controlling a boat. Uses an oak boat as its display item. |
| `brassworksmissions:fly_elytra` | Fixed | Distance travelled with an elytra. |
| `brassworksmissions:ride_mob` | Fixed | Distance travelled while riding a mob. Uses a saddle as its display item. |
| `brassworksmissions:eat_cake_slice` | Fixed | Eating cake slices. |

### Create mission types

| Mission type | Target | Progress |
| --- | --- | --- |
| `brassworksmissions:mix_item` | Item | Producing the selected output through Create mixing. |
| `brassworksmissions:compact_item` | Item | Producing the selected compacting output. |
| `brassworksmissions:press_item` | Item | Producing the selected mechanical press output. |
| `brassworksmissions:saw` | Block item | Breaking the selected block with a contraption-mounted mechanical saw. |
| `brassworksmissions:crush_item` | Item | Producing the selected crushing-wheel output. |
| `brassworksmissions:mill_item` | Item | Producing the selected millstone output. |
| `brassworksmissions:cut_item` | Item | Producing the selected cutting output with a stationary mechanical saw. |
| `brassworksmissions:fill_item` | Item | Producing the selected filled item with a spout. |
| `brassworksmissions:mechanical_craft` | Item | Producing the selected mechanical-crafting output. |
| `brassworksmissions:drill_block` | Block item | Breaking the selected block with a mechanical drill or Create block-breaking contraption. |
| `brassworksmissions:move_on_belt` | Fixed | Distance travelled while riding a Create belt. Uses the belt connector as its display item. |
| `brassworksmissions:travel_by_chain_conveyor` | Fixed | Distance travelled with a chain conveyor. Uses the Create wrench as its display item. |
| `brassworksmissions:harvest` | Item | Harvesting the selected crop or block with a mechanical harvester. |

Create processing missions count the size of matching output stacks. The integration listens to the relevant Create machines and contraption behavior on the server.

## Mission categories

Category 1 keeps the existing mission behavior and configuration. Missions without a `category` field belong to category 1.

Category 2 is disabled by default. Enable it in the server config and configure its name, slot count, reset days, and UTC reset hour.

```json
{
  "id": "example:weekly_factory_order",
  "type": "brassworksmissions:mechanical_craft",
  "category": 2
}
```

Valid category values are `1` and `2`. When category 2 is disabled, its slots and UI section are not created. Both category names appear as separate sections in the mission selector when category 2 is enabled.

Slot-count changes do not resize existing player data immediately. Category 1 settings are applied on its next reset or with `/missions reassign category 1`. Category 2 slot-count settings are applied on its next reset or with `/missions reassign category 2`. `/missions reassign` applies both categories at once. An optional player argument can be appended to every reassign command.

When category 2 is enabled but missing from a player's saved data, it is initialized automatically on login without reassigning category 1. Disabling category 2 is applied with `/missions reassign category 2` or a full reassign because a disabled category no longer receives scheduled resets.

The currently applied category split is stored per player. This keeps the selector, slot numbering, and reset timer consistent until the corresponding category is reassigned.

### Category configuration

Category 1 uses these server-config values:

| Setting | Default | Description |
| --- | --- | --- |
| `Mission Reset.missionResetDays` | `SUNDAY` | Reset days using uppercase English weekday names. |
| `Mission Reset.missionResetHour` | `0` | UTC hour from 0 to 23. |
| `Mission Assignment.missionSlotCount` | `6` | Active category 1 slots, from 1 to 12. |
| `Mission Assignment.categoryOneName` | `Available Missions` | Name shown in the mission selector. |

Category 2 uses:

| Setting | Default | Description |
| --- | --- | --- |
| `Optional Mission Category.enabled` | `false` | Enables category 2. |
| `Optional Mission Category.name` | `Weekly Missions` | Name shown in the mission selector. |
| `Optional Mission Category.slotCount` | `6` | Active category 2 slots, from 1 to 12. |
| `Optional Mission Category.resetDays` | `SUNDAY` | Independent category 2 reset days. |
| `Optional Mission Category.resetHour` | `0` | Independent UTC reset hour. |

Reset schedules are checked while the server is running and when a player logs in. If multiple configured reset days have passed, the most recent scheduled reset is used.

## Mission selection and weighting

Each category has its own mission pool. A category 1 slot only receives category 1 templates, and a category 2 slot only receives category 2 templates.

Selection uses the `weight` of every enabled and available template. For example, a mission with weight `8` is twice as likely to be selected as a mission with weight `4`, assuming both are in the same category and available to the player.

The mod tries up to 20 times to find a unique template for each slot. Rerolls also prefer templates that are not already active. If too few valid templates exist, a slot can remain empty during assignment, or a reroll can fall back to a duplicate template.

## Availability requirements

Availability requirements are evaluated for the player whenever a mission template is considered for assignment. Every configured requirement must pass.

The built-in advancement requirement is:

```json
"availabilityRequirements": [
  {
    "type": "brassworksmissions:advancement",
    "data": {
      "advancement": "minecraft:story/enter_the_nether",
      "completed": true
    }
  }
]
```

- `advancement` must be a valid advancement resource location.
- `completed` defaults to `true`.
- Set `completed` to `false` to make the mission available only before the advancement is completed.

Unknown requirement types and failed requirement checks make the mission unavailable. Additional requirement implementations can be registered through the mission requirement API.

## Rewards

The reward item is configured through `Mission Rewards.rewardItem` in the server config:

```toml
rewardItem = "minecraft:diamond"
```

Create: Numismatics is only required when a Numismatics item is configured as the reward.

Every mission rolls a reward amount from its `reward` block:

```json
"reward": {
  "minAmount": 4,
  "maxAmount": 12,
  "mode": "random"
}
```

Reward modes:

- `random` rolls an inclusive random amount between `minAmount` and `maxAmount`.
- `progressive` scales the reward between `minAmount` and `maxAmount` according to the rolled mission difficulty.

When a reward is claimed, it is inserted into the player's inventory. Any remainder that does not fit is dropped at the player's position.

Completed but unclaimed rewards are banked before their category is reset or reassigned. Pending rewards survive player-data saves and can later be collected with Claim All.

Claimed missions increase the `brassworksmissions:missions_completed` statistic and can grant the included completion advancements at 10, 50, 100, 250, 500, and 1,000 completed missions.

## Rerolls

Rerolls keep the slot in its current category. Completed missions cannot be rerolled.

The first two rerolls after a reset or reassignment are free. Later rerolls consume increasing amounts of the configured reward item. The cost increases by two items per paid reroll and is capped when the calculated price would exceed 32 items.

Reroll cost is reset by scheduled reassignment, manual reassignment, or the operator command:

```text
/missions reset_reroll_cost <player>
```

## Mission menu and HUD

Players can open the mission menu with the default H key or:

```text
/missions
```

The menu displays active missions, category sections, progress, rewards, the selected mission, reroll controls, tracking controls, and Claim All.

Tracked missions are displayed in the HUD. The client config controls:

- `Mission HUD.hudXOffset`
- `Mission HUD.hudYOffset`
- `Mission HUD.leftAlignHud`
- `Mission HUD.bottomAlignHud`
- `Mission HUD.hudSize`

Available HUD sizes are `NORMAL`, `SMALL`, and `EXTRA_SMALL`.

## Operator commands

Mission administration commands require permission level 2.

### Player mission commands

```text
/missions reassign [player]
/missions reassign category <1|2> [player]
/missions reroll <slot> [player]
/missions claim <slot> [player]
/missions claim_all [player]
/missions complete <slot> [player]
/missions reset_reroll_cost <player>
```

The commands with an optional player argument use the executing player when no target is supplied.

### Mission management commands

```text
/missions manage
/missions manage list [page]
/missions manage types [page]
/missions manage show <id>
/missions manage create <type> [target] <icon> <min> <max> <reward_min> <reward_max> <weight> <random|progressive> <title_or_json>
/missions manage disable <id>
/missions manage enable <id>
/missions manage delete <id>
/missions manage icon <id> <icon>
/missions manage titles <id> <title_or_json>
/missions manage reward_mode <id> <random|progressive>
/missions manage category <id> <1|2>
/missions manage diagnose
/missions manage reload
```

`/missions manage types` lists every registered mission type and shows whether it needs an item, block, entity, or no target. Target and icon arguments support registry-based tab completion.

New missions receive an automatically generated ID and start in category 1. Use `/missions manage category` afterward to move them to category 2.

## Live reload

Automatic `missions.json` live reload is controlled by `Mission Management.liveReload` in the server config and defaults to `true`. The setting is applied when the server starts.

When live reload is enabled, the mod watches the configuration directory and reloads `missions.json` after external file changes. When disabled, use:

```text
/missions manage reload
```

Reloading changes the templates used for future assignments and rerolls. Existing active missions remain stored on players until they are completed, reset, reassigned, or rerolled.

If the file cannot be parsed, the current runtime mission state is kept when possible and the error is logged.

## Diagnostics and validation

Run:

```text
/missions manage diagnose
```

The asynchronous diagnostic checks:

- JSON readability and duplicate mission IDs
- registered mission types
- mission categories, weights, titles, and amount ranges
- requirement items and display icons
- block and entity target compatibility
- declared recipe IDs and loaded recipe outputs
- reward modes
- availability requirement types and advancement IDs

Normal loading also ignores invalid mission definitions and logs the validation error instead of creating an invalid active mission.

## Legacy datapack migration

Mission files in:

```text
data/brassworksmissions/missions/*.json
```

are legacy files and are no longer loaded directly. When legacy mission files are detected in world datapacks, server operators receive a clickable prompt with three options:

- Transfer valid missions to `missions.json`, then delete the legacy mission files.
- Delete the legacy mission files without importing them.
- Ignore the current set of files until it changes.

The same actions are available through:

```text
/missions legacy transfer_and_delete
/missions legacy delete
/missions legacy ignore
```

Only the legacy mission JSON entries are removed. Other datapack content is preserved, including when the datapack is a ZIP file.

## Persistence and synchronization

Player mission data includes:

- active mission slots and their applied category split
- generated titles, targets, icons, required amounts, and rewards
- progress and claimed state
- selected and HUD-tracked slots
- category reset timestamps
- reroll count
- banked unclaimed rewards

The data is copied when the player is cloned and synchronized after login, respawn, dimension changes, mission changes, reward claims, and resets.