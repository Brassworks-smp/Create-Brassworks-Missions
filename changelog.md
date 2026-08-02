### Added

* Added an optional second mission category with its own name, slot count and independent reset schedule.
* Added per-mission category assignment through the new `category` property.
* Added `/missions manage category <id> <1|2>` for moving mission templates between categories.
* Added `/missions reassign category <1|2> [player]` for reassigning only one mission category.
* Added configurable names for both mission categories.
* Added mission availability requirements with a public registry for custom requirement types.
* Added a built-in advancement requirement for locking missions behind completed or incomplete advancements.
* Added `/missions manage diagnose` for asynchronously checking mission definitions, registry entries, recipes and advancement requirements.
* Added automatic live reloading of `missions.json` when the file changes.
* Added a configurable option for disabling automatic mission reloading.
* Added an in-game migration workflow for legacy datapack missions.
* Added commands for transferring, deleting or ignoring detected legacy mission files.
* Added administrator notifications when legacy mission datapacks are detected.
* Added configurable HUD size, horizontal and vertical offsets, left alignment and bottom alignment.

### Changed

* Mission definitions are now loaded from `config/brassworksmissions/missions.json`.
* The bundled default missions are copied to the configuration directory when no mission file exists.
* Existing `managed_missions.json` files are automatically renamed to `missions.json`.
* Datapack mission definitions are now treated as legacy files and are no longer loaded directly.
* The mission reward item is now configured through the NeoForge server config instead of `data/brassworksmissions/missions/mission_reward.json`.
* Mission storage now keeps separate slot counts for both mission categories.
* The previous single slot range of 1 to 32 was replaced with up to 12 configurable slots per category.
* Mission resets can now affect one category without replacing missions from the other category.
* Mission selection now filters templates according to each player's availability requirements.
* Mission list and detail commands now display the assigned mission category.
* Mission networking now uses direct NeoForge 1.21.1 payload registration with explicit clientbound, serverbound and bidirectional packet directions.
* Removed the obsolete packet registration map and its unchecked raw-type casts.
* Removed the unused reward reload manager and its duplicate mission resources.
* Removed unused menu binding state, the unused delayed server work queue and other dead helper code.

### Fixed

* Fixed payloads being registered through `FMLCommonSetupEvent` instead of the modern NeoForge payload registration event.
* Fixed serverbound and clientbound packets being registered as bidirectional when only one direction was required.
* Fixed invalid mission configuration reloads replacing a previously working runtime mission pool.
* Fixed category-specific resets affecting slots belonging to the other mission category.
* Fixed the second mission category not being initialized for existing players after enabling it.
* Fixed category reset timestamps not being preserved when player data is cloned.
* Fixed unavailable or unknown mission requirements being able to enter the player's mission pool.
* Fixed invalid advancement references, recipe IDs, item IDs, block targets and entity targets being difficult to identify.
* Fixed obsolete reward configuration resources conflicting with the server-config reward system.