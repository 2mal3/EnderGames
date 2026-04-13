{
  pkgs,
  lib,
  config,
  inputs,
  ...
}: {
  env.EG_DEBUG = "true";

  packages = with pkgs; [
    papermc
    google-java-format
    maven
    jdk21
    nodejs
  ];

  tasks."eg:build".exec = ''
    mvn clean package -Dmaven.test.skip=true
  '';

  tasks."eg:format".exec = ''
    find . -name '*.java' -exec google-java-format --replace {} +
  '';

  tasks."test:format".exec = ''
    find . -name '*.java' -exec google-java-format --dry-run --set-exit-if-changed --skip-sorting-imports {} +
  '';

  tasks."test:logic".exec = ''
    mvn test
  '';
}
