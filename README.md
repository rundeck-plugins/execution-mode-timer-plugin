# Enable/Disable Execution/Scheduled Later plugin


## Install

Add to local maven 
```
mvn install:install-file -Dfile=build/libs/enable-later-executions-plugin-0.1.jar -DgroupId=com.rundeck.plugins -DartifactId=enable-later -Dversion=0.0.1 -Dpackaging=jar
```

Add dependency rundeck (`rundeckapp/build.gradle`)

```
compile ("com.rundeck.plugins:enable-later:0.0.1")
```
