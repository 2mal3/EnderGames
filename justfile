export EG_DEBUG := "true"

build:
    mvn clean
    mvn package

dev: build
    mkdir -p mc-server/plugins
    ln -s --force ../../target/endergames-0.9.0.jar mc-server/plugins/endergames.jar
    cd mc-server && minecraft-server

upload: build
    ./upload.sh

format:
    find . -name '*.java' -exec google-java-format --replace {} +
