This is an example of application that loads plugin from a jar.


## How to compile and run the demo application

You should have Maven and JDK 1.8+ installed.

1. Open this folder with in the console.

2. Compile the example: ``mvn clean package``

3. Run the demo app: ``java -jar plugin-loader-example-app/target/plugin-loader-example-app-0.0.1.jar plugin-loader-example-plugin/target``

If you want to see want happens if plugin directory contains jar that does not contains valid plugins, please run:  
``java -jar plugin-loader-example-app/target/plugin-loader-example-app-0.0.1.jar . 3``