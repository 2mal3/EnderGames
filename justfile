build:
    mvn clean
    mvn package

upload: build
    ./upload.sh
