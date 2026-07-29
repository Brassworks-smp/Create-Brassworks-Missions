### Added



* Added a complete in-game mission management system for server operators.

* Missions can now be created, listed, viewed, enabled, disabled and deleted through commands.

* Added automatic mission ID generation.

* Added support for separate mission targets and display icons.

* Added support for multiple randomly selected mission titles.

* Added commands for changing mission icons, titles and reward modes.

* Added random and progressive reward modes for individual missions.

* Added persistent storage for completed but unclaimed rewards.

* Added a configurable active mission count from 1 to 32.

* Expanded the mod to 29 mission types, including block placing, item smelting and item collection missions.

* Added `/missions manage types` to display available mission types and their creation syntax.

* Added support for multiple mission templates using the same mission type.



### Changed



* Completed rewards stored during a mission reset can be collected later with Claim All.

* Existing datapacks remain compatible with the expanded mission format.

* Mission validation and loading now provide clearer errors for invalid definitions.



### Fixed



* Fixed a critical server crash caused by missing player or mission data during progress checks.

* Fixed rewards being lost when the configured mission slot count was reduced.

* Fixed the Fill Item mission type not being registered correctly.

* Fixed several mission progress handlers failing with blocks or items from other mods.

* Fixed invalid or incomplete mission definitions being able to crash mission loading.

* Fixed reroll validation and reward delivery behavior.

* Improved null safety across the mission system.