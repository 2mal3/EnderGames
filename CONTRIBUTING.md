# Contributing

## Developing

- Setting the `EG_DEBUG` environment variable to `true` enables debug mode when the server is started. This decreases

## Code

- This project uses the [Google Java Style](https://google.github.io/styleguide/javaguide.html). I recommend
  using [google-java-format](https://github.com/google/google-java-format) to format your code for you.
  cosmetic timers, allowing for faster testing and iteration.

### Adding a Kit

1. Create a new class in the [`io.github.mal32.endergames.kits`](/src/main/java/io/github/mal32/endergames/kits) package
   that extends the `AbstractKit` class.
2. Add a reference to the list in the method `getKits` of the `AbstractKit` class.
3. Test if you can select the kit in-game and if it works as expected.

## Text Color Style

- use Vanilla colors
- `green` and `red` for good and bad messages
- `yellow` for default system messages with optional `gold` highlights