# Mission Availability Requirements API

Availability requirements decide whether a mission template may be assigned to a player. They do not change progress tracking after the mission has been assigned.

## Advancement requirement

```json
{
  "id": "example:diamond_duty",
  "type": "brassworksmissions:break_block",
  "availabilityRequirements": [
    {
      "type": "brassworksmissions:advancement",
      "data": {
        "advancement": "minecraft:story/mine_diamond",
        "completed": true
      }
    }
  ]
}
```

`completed` is optional and defaults to `true`. Set it to `false` when the mission should only be available before the advancement is completed. Multiple requirements use AND logic.

## Custom requirement

Implement `MissionAvailabilityRequirement` and register the implementation during common setup.

```java
public final class PermissionRequirement implements MissionAvailabilityRequirement {
    @Override
    public String id() {
        return "example:permission";
    }

    @Override
    public Optional<String> validate(MissionRequirementContext context) {
        JsonElement level = context.data().get("level");
        return level != null && level.isJsonPrimitive()
                ? Optional.empty()
                : Optional.of("permission requirement needs a numeric level");
    }

    @Override
    public boolean test(ServerPlayer player, MissionRequirementContext context) {
        return player.hasPermissions(context.data().get("level").getAsInt());
    }
}
```

```java
MissionRequirementRegistry.register(new PermissionRequirement());
```

```json
{
  "availabilityRequirements": [
    {
      "type": "example:permission",
      "data": {
        "level": 2
      }
    }
  ]
}
```

Requirement IDs must be unique. `validate` is used by mission loading and `/missions manage diagnose`. `test` runs during weighted mission selection and should avoid blocking I/O.
