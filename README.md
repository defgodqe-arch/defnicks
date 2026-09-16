# defnicks

A small Paper plugin that automatically configures **TAB** to show the nicknames set by **EssentialsX**.

## What it does

When TAB and Essentials are enabled, defnicks automatically runs:

```text
/tab group _DEFAULT_ customtabname %essentials_nickname%
```

That is the official TAB-supported way to put an Essentials nickname into the player's tablist name. The nickname is resolved dynamically, so changing `/nick` updates what TAB displays without modifying the TAB JAR.

## Requirements

- Paper 1.21.x / Java 21
- EssentialsX
- TAB

## Install

1. Download `defnicks-1.0.0.jar` from the GitHub Actions build artifact.
2. Put it in your server's `plugins` folder.
3. Restart the server.
4. Use EssentialsX `/nick <name>`.
5. Press TAB and the Essentials nickname should be shown.

You can also run `/defnicks` as an operator to re-apply the TAB setting.

## Important

TAB's documentation states that `_DEFAULT_` applies the setting to groups that do not have their own `customtabname` override. If a specific TAB group already defines `customtabname`, remove that override or set it to `%essentials_nickname%` for that group.

This plugin does **not** modify TAB's JAR. That keeps the setup compatible with TAB updates.
