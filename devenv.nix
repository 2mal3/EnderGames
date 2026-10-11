{pkgs, ...}: {
  env = {
    EG_DEBUG = "true";
  };

  languages.java = {
    enable = true;
    maven.enable = true;
  };

  packages = with pkgs; [
    papermcServers.papermc-1_21_11
    google-java-format
  ];

  processes.server = {
    exec = ''
      devenv tasks run eg:build --no-tui
      mkdir -p mc-server/plugins
      ln -s --force ../../target/endergames-0.9.0.jar mc-server/plugins/endergames.jar
      cd mc-server && minecraft-server
    '';
    watch = {
      paths = [./src/main];
      extensions = ["java"];
    };
    ready.exec = ''
      ss -tlnp | grep 25565
    '';
  };

  tasks = {
    "eg:build".exec = ''
      mvn clean
      mvn package -Dmaven.test.skip=true
    '';

    "eg:format".exec = ''
      find . -name '*.java' -exec google-java-format --replace {} +
    '';

    "eg:test" = {
      exec = ''
        mvn test
      '';
    };
  };
}
