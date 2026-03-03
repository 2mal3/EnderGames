export EG_DEBUG := "true"

set dotenv-load := true

build:
    mvn clean
    mvn package

dev: build
    cd $EG_SEVER_PATH && minecraft-server

upload: build
    ./upload.sh
