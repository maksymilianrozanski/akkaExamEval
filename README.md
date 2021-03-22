A simple app created while exploring Akka Typed, and Akka Persistence.

The project is not polished up yet. Some functionalities, like error handling are not fully implemented yet.

App is allows generating exams/quizzes from question pool. The results of exams are saved, and exam score is displayed
to users.

Steps to run in local environment:

- make sure that docker engine is installed and port 8080 is available

- run `docker:publishLocal` in sbt shell

- run `docker-compose up` in terminal

A very simple frontend is build for part of functionalities, and should be available at `localhost:8080`. For remaining
part example http requests, and their content is located at in `./curl` directory 
