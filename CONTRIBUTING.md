# Contributing

## Developing

- setting the `EG_DEBUG` environment variable to `true` enables debug mode, which speeds up various timers for faster development
- you cant validate the code with the shell command `mvn test`
- you can get the exact current version of the plugin by running the command `/version endergames` in-game

### Conventions

- code is formatted using [Google Java Style](https://google.github.io/styleguide/javaguide.html), for example by using [google-java-format](https://github.com/google/google-java-format)
- don't use getter and setter methods when no other logic is involved. Just use public attributes in this case
- experimental paper apis can be used if neccecary
- deprecated apis are not allowed
- use [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) for the commit messages
- use [Conventional Branch](https://conventional-branch.github.io/) for branches

### Kit API

- the constructor is called at plugin init; don't put per-game logic there
- `enable()` is called at the start of every game; here you can put your timer inits etc.
- `disable()` is called when a game ends; use this to stop all running tasks
- to deregister a player, you only have to listen for the `playerDeath` and not for the `playerDisconnect` event as a disconnect automatically results in a death

### Adding a Kit

1. create a new class in the [`io.github.mal32.endergames.kits`](/src/main/java/io/github/mal32/endergames/kits) package
   that extends the `AbstractKit` class
2. add a reference to the list in the method `getKits` of the `AbstractKit` class
3. if you want players to unlock the kit first, add the corresponding advancement to [`/src/main/resources/EnderGamesDatapack/data/enga/advancement`](/src/main/resources/EnderGamesDatapack/data/enga/advancement)
4. test if you can select the kit in-game and if it works as expected

## Design

- game is intended for at most 24 players

### Color Style

- use Vanilla colors
- `green` and `red` for good and bad messages
- `yellow` for default system messages with optional `gold` highlights
