# Contributing

## Code

- This project uses the [Google Java Style](https://google.github.io/styleguide/javaguide.html). I recommend
  using [google-java-format](https://github.com/google/google-java-format) to format your code for you.

### Adding a Kit

1. Create a new class in the [`io.github.mal32.endergames.kits`](/src/main/java/io/github/mal32/endergames/kits) package
   that extends the `AbstractKit` class.
2. Add a reference to the list in the method `getKits` of the `AbstractKit` class.
3. Create a corresponding kit unlock advancement in
   the [Datapack](/src/main/resources/EnderGamesDatapack/data/enga/advancement) advancements.
4. Test if you can unlock and select the kit in-game and if it works as expected.
