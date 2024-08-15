# Just Enough Debug Tools

[work in progress]

You can add the mod to your 1.21.1 workspace. It requires the Fabric API and the Fabric Loader (v0.15.11 or higher).

```groovy
repositories {
    maven { url "https://maven.shadew.net/" }
}

dependencies {
    // ...other dependencies...

    modImplementation("dev.runefox.mc:jedt:0.7.1") {
        exclude group: "net.fabricmc"
        exclude group: "net.fabricmc.fabric-api"
    }
}
```

## Dedicated server

When using this mod on a dedicated Minecraft Server instance, make sure to enable the debug tools on the server. For
security and performance reasons, the mod is disabled by default on dedicated servers.

To enable the debug tools on the dedicated server, add a JSON file in your server directory, name it
`debug_config.json`, and add the following:

```json
{
  "available": true
}
```

When `available` is set to `true`, you can still disable certain functions of the debug mod by defining them as
properties in this JSON file. Currently, there are four configurable properties:

- `game_rule_sync` (default: `true`) indicates whether the full list of game rules and their values should be synced
  with the client every time they change. This is used to show semantic game rule options in the debug tools menu.
- `send_pathfinding_info` (default: `true`) indicates whether the server should send pathfinding information to the
  client. This is used on the client to render pathfinding information. When `false`, the pathfinding debug tool is not
  available in the menu.
- `send_neighbor_updates` (default: `true`) indicates whether the server should send neighbor update information to the
  client. This is used on the client to render updates of neighbors. When `false`, the neighbor update debug tool is not
  available in the menu.
- `enable_gametest` (default: `true`) indicates whether the server allows the usage of the GameTest framework.
  Integration with the GameTest framework is experimental.
