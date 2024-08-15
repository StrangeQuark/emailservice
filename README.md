# Email service

The Email Service project is a Spring Boot application that handles all email requests

# Tools

The Email Service uses Spring Boot and PostgreSQL

# Docker

It is highly recommended that the service is ran using docker, you can simply use the `docker-compose up --build` command to run the application, there are no other steps needed

If you decide not to use docker, you will need to uncomment out `src/main/resources/application.properties`, you can then proceed with the next steps

# Installation

Once the Email Service repository cloned and PostgreSQL is installed, create an "emailservice" table and connect to the database using the following commands:

`psql`

`CREATE DATABASE emailservice;`

`GRANT ALL PRIVILEGES ON DATABASE "emailservice" TO \<YourUsername\>;`

`\c emailservice`

Now the server can be started in IntelliJ by running the EmailServiceApplication configuration

# MailDev

MailDev is a email server used for testing applications, it can be installed through NPM or docker with the following commands:

Install maildev using NPM: `npm install -g maildev`

Run maildev using the following command: `maildev`

Run maildev in a docker container: `docker run -p 1080:1080 -p 1025:1025 maildev/maildev`