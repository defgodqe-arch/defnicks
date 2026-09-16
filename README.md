# KrystalDisplayName

Standalone Paper plugin for Krystal SMP.

## Requirements
- Minecraft/Paper 1.21.11
- Java 21
- Maven 3.9+

## Build
Run `mvn clean package` from the project root. The JAR is created at `target/krystal-display-name-1.0.0.jar`.

Copy the JAR into the server `plugins` folder and restart.

## Commands
- `/nick <name>`
- `/nick reset`

## MiniMessage examples
- `/nick Krystal`
- `/nick <aqua>Krystal</aqua>`
- `/nick <gradient:#00ffff:#9b5cff>Krystal</gradient>`

The plugin changes chat, TAB, the custom overhead nametag, and death messages. Nicknames are stored in the generated plugin config.yml.