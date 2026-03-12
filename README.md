# ODYAdminCommands

`ODYAdminCommands` is a Paper `1.21.11` admin plugin focused on lightweight staff stealth tools.

Current version: `1.0.3`  
Author: `starboyash`

## Features

- `/vanish`
  - Hides the player from the tab list for normal players
  - Makes vanished players appear offline to configured private-message commands like `/msg`
  - Suppresses join and quit messages while vanished
  - Blocks normal public chat while vanished and asks for confirmation before sending
- `/invis`
  - Hides the player entity from non-bypass players
- `/cleanlogs`
  - Deletes old server log files
  - Truncates `latest.log`
  - Deletes crash reports
- Bypass visibility
  - Players with bypass permission can still see vanished and invisible staff
  - Vanished players show in tab as red with strikethrough for bypass viewers
- Persistent state
  - Vanish and invis states survive reconnects and restarts
- Auto update
  - Checks the latest GitHub release on startup
  - Downloads a newer jar into Paper's update folder for the next restart

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/vanish` | Toggle offline-style vanish | `odyadmincommands.vanish` |
| `/invis` | Toggle physical invisibility | `odyadmincommands.invis` |
| `/cleanlogs` | Clean server logs and crash reports | `odyadmincommands.cleanlogs` |

Internal command:

- `/vanishchatconfirm <token>`
  - Used only by the clickable chat confirmation flow for vanished players

## Permissions

| Permission | Description | Default |
| --- | --- | --- |
| `odyadmincommands.vanish` | Allows `/vanish` | `op` |
| `odyadmincommands.invis` | Allows `/invis` | `op` |
| `odyadmincommands.cleanlogs` | Allows `/cleanlogs` | `op` |
| `odyadmincommands.vanish.bypass` | Lets staff see vanished/invisible players and bypass offline-style checks | `op` |

## Vanish Chat Confirmation

If a vanished player tries to send normal chat, the plugin blocks the message and sends a private confirmation prompt:

`You are in vanish. Are you sure you want to send this? Send anyway`

Clicking `Send anyway` resends that exact message once.

## Config

Default `config.yml`:

```yaml
offline-command-aliases:
  - msg
```

`offline-command-aliases` controls which private-message style commands should treat vanished players as offline for non-bypass players.

Auto update settings:

```yaml
auto-update:
  enabled: true
```

- `enabled`: turns startup update checks on or off
- The updater is hardcoded to `itsasheruwu/ODYAdminCommands`
- The updater is hardcoded to download the release asset named `ODYAdminCommands.jar`

## Build

Requirements:

- Java `21+`

Build with the Gradle wrapper:

```powershell
.\gradlew.bat build
```

Output jar:

```text
build/libs/ODYAdminCommands-1.0.3.jar
```

## Notes

- Target platform: Paper `1.21.11`
- Designed to remain usable for Bedrock players joining through Geyser
- No Floodgate integration is required for the current feature set
- Auto-update stages the new jar for the next restart; it does not hot-swap the running plugin
- GitHub repo selection and release-asset selection are intentionally not configurable
