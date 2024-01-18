# DVD Store Service

The Conference Management System Rest API is a Spring Boot-based application designed to manage academic conferences. It provides functionalities for user registration, paper submission, and conference management. 
The system allows the user to have multiple roles and the available ones are `ROLE_AUTHOR`,`ROLE_REVIEWER`, `ROLE_PC_CHAIR`. The supported files are pdf and Latex(tex).

**Important note:** When you create a paper or a conference with a user that does not have the
`ROLE_AUTHOR` and `ROLE_PC_CHAIR` prior to the request and the request is successful, the user will be assigned a new role
and the current session will be invalidated. The reason for this is, because the cookie/token that was assigned upon the user logging in/registering in our application now has the wrong  roles,
and despite the user has a new role, they don't have access to the new endpoints. In this case after the paper/conference is created
,remove the cookie from the browser/postman, make a `GET` request to the `/api/v1/auth/csrf` to get the new CSRF token and login to continue your requests.

# Features
* User Authentication and Registration
* Paper Submission and Review
* Conference Creation and Management

# Technologies
* Java 17
* Spring Boot 3.2.0
* Spring Security
* Spring Data JPA
* PostgreSQL
* Spring Sessions with Redis
* Flyway
* Apache Tika
* TestContainers
* Junit5
* Mockito
* Swagger

# Getting Started
To get started with this project you will need to have the following installed on your machine:

* Java 17+
* Maven 3.9.1+
* PostgreSQL 15.2+

Make sure you create a database with name `conference_ms` and set the password of the default user `postgres` to `postgres`. You also need to add the directory that you want the files to be stored by updating the `papers.directory` property in the application.yaml.

To build and run the project, follow these steps:

* Clone the repository to your local machine: https://github.com/ThLentzas/conference-management-system.git
* Navigate to the project directory
* Build the project: `mvn clean install -DskipTests`
* Run the project: `mvn spring-boot:run`
* The application will be available on: http://localhost:8080/swagger-ui/index.html

Your first request should be a `GET` to `/api/v1/auth/csrf` in order to obtain a CSRF token that you will have to
set to the `X-CSRF-TOKEN` header in th SwaggerUI for subsequent requests. This request will also set the `COOKIE` that will
be used for subsequent requests and will be automatically included by the browser.

![Swagger UI](https://i.imgur.com/kZwPATm.png)