# Contributing

## Developing

- Set the environment variable `EG_DEBUG=true` to enable debug mode: This speeda up internal timers and is useful during development.
- Validate the codebase by running: `mvn test`
- You can check the currently installed plugin version in‑game using: `/version endergames` in-game

### Conventions

- Code is formatted using [Google Java Style](https://google.github.io/styleguide/javaguide.html). You can apply formatting automatically using e.g. [google-java-format](https://github.com/google/google-java-format).
- Experimental Paper APIs can be used if necessary.
- Deprecated APIs are not allowed.
- Use [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) for commit messages.
- Use [Conventional Branch](https://conventional-branch.github.io/) naming for branches.

### Kit API

- The constructor is called once at plugin initialization. Do not put per‑game logic here.
- `onEnable()` is called at the start of every game. Use this for timers, schedulers, or per-game initialization.
- `onDisable()` is called when a game ends. Use this to stop tasks and clean up temporary state.
- You only need to listen for player death to deregister a player. A disconnect automatically counts as a death in the game system.

### Adding a new Kit

To add a new kit to the game:

1. Create a new class in [`io.github.mal32.endergames.kitsystem.kits`](/src/main/java/io/github/mal32/endergames/kitsystem/kits) extending [`AbstractKit`](/src/main/java/io/github/mal32/endergames/kitsystem/api/AbstractKit.java).
2. Implement the required methods
   - `initPlayer(Player)` &rarr; give starting items, effects, etc.
   - Optional Override:
     - `onEnable()` &rarr; per-game initialization
     - `onDisable()` &rarr; cleanup, cancel tasks
3. Register the kit in [`KitRegistry`](/src/main/java/io/github/mal32/endergames/kitsystem/registry/KitRegistry.java):
   ```java
   kitManager.register(new YourKitName(kitService, plugin));
   ```
4. (Optional) If the kit should be locked behind an advancement:
   - Implement the `KitUnlockAdvancement` interface.
   - Implement the `getKitAdvancementKey()` method that returns your advancement key (e.g. `enga:mycustomadvancement`).
   - Add the advancement JSON to: [`/src/main/resources/EnderGamesDatapack/data/enga/advancement`](/src/main/resources/EnderGamesDatapack/data/enga/advancement)
5. Test the kit in-game. Ensure:
   - it appears in the selection menu
   - abilities work as expected
   - unlock conditions behave correctly

## Design Notes

- The game is designed for up to 24 players.

### Color Style

- Use vanilla Minecraft colors.
- `green` and `red` for positive/negative messages.
- `yellow` for default system messages, with optional `gold` highlights.
