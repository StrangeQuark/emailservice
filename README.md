# Email service

The Email Service project is a Spring Boot application that handles all email requests

# Tools

The Email Service uses Spring Boot and PostgreSQL

It is highly recommended to use IntelliJ for your IDE, as IntelliJ is the IDE used in development

# Installation

Once the Email Service repository cloned and PostgreSQL is installed, users will need to create an "emailservice" table and connect to the database using the following commands:

**psql**

**CREATE DATABASE emailservice;**

**GRANT ALL PRIVILEGES ON DATABASE "emailservice" TO \<YourUsername\>;**

**\c emailservice**

Now the server can be started in IntelliJ by running the EmailServiceApplication configuration

# MailDev

MailDev is a email server used for testing applications, it can be installed through NPM or docker with the following commands:

Install maildev using NPM: **npm install -g maildev**

Run maildev using the following command: **maildev**

Run maildev in a docker container: **docker run -p 1080:1080 -p 1025:1025 maildev/maildev**