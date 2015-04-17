# Examples of messaging using RabbitMQ
In Java, using Gradle for managing dependencies and build.

## Requirements
You need a RabbitMQ server on your localhost for the examples to work. You can download and install RabbitMQ from their webpage <http://www.rabbitmq.com/>. The examples have been tested with version 3.5.1 of RabbitMQ (but they will work with other versions too).

## Use (command line)
Clone the project in your computer. Then go to the directory where a file named `build.gradle` is. Run `$ ./gradlew tasks` (`gradlew.bat` if you are in Windows). If everything is fine, this will download a specific version of Gradle to your computer (only the first time), and will show the available Gradle tasks in the build.

As there are a number of examples in this repository, I have configured gradle.build so you can run them from the shell following this pattern: `$ ./gradlew --quiet -PmainClass=TheClassYouWant run`. The available classes are:

- Example 0: `es.unizar.tmdad.msg.ex0.Emisor` (Sender) and `es.unizar.tmdad.msg.ex0.Receptor` (Receiver)
- Example 1: `es.unizar.tmdad.msg.ex1.Emisor` (Sender) and `es.unizar.tmdad.msg.ex1.Receptor` (Receiver)
- Example Pub-Sub: `es.unizar.tmdad.msg.expubsub.EmisorPubSub` (Sender) and `es.unizar.tmdad.msg.expubsub.ReceptorPubSub` (Receiver)

If you want to try the example 0, you would launch the sender on a console with `$ ./gradlew --quiet -PmainClass=es.unizar.tmdad.msg.ex0.Emisor run` and the receiver on a different console with `$ ./gradlew --quiet -PmainClass=es.unizar.tmdad.msg.ex0.Receptor run`. 

## Use (Eclipse IDE)
Install the plugin named "Gradle Integration for Eclipse" for your Eclipse version from its marketplace. Then go to File > Import..., choose Gradle Project, put as root folder the directory you want (one with a build.gradle file) from the project, click on Build model, choose the one that is shown and click on Finish. This imports the project to Eclipse so you can run it from there.  

If dependencies are not automatically met, right click on the name of the projecto in the Package Explorer and choose Gradle > Refresh All.