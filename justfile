export EG_DEBUG := "true"

build:
    mvn clean
    mvn package -Dmaven.test.skip=true

test:
    mvn test

dev: build
    mkdir -p mc-server/plugins
    ln -s --force ../../target/endergames-0.9.0.jar mc-server/plugins/endergames.jar
    cd mc-server && minecraft-server

upload: build test
    ./upload.sh

format:
    find . -name '*.java' -exec google-java-format --replace {} +
