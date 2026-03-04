# Java Code Assignment

This is a short code assignment that explores various aspects of software development, including API implementation, documentation, persistence layer handling, and testing.

## About the assignment

You will find the tasks of this assignment on [CODE_ASSIGNMENT](CODE_ASSIGNMENT.md) file

## About the code base

This is based on https://github.com/quarkusio/quarkus-quickstarts

### Requirements

To compile and run this demo you will need:

- JDK 17+

In addition, you will need either a PostgreSQL database, or Docker to run one.

### Configuring JDK 17+

Make sure that `JAVA_HOME` environment variables has been set, and that a JDK 17+ `java` command is on the path.

## Building the demo

Execute the Maven build on the root of the project:

```sh
./mvnw package
```

## Running the demo

### Live coding with Quarkus

The Maven Quarkus plugin provides a development mode that supports
live coding. To try this out:

```sh
./mvnw quarkus:dev
```

In this mode you can make changes to the code and have the changes immediately applied, by just refreshing your browser.

    Hot reload works even when modifying your JPA entities.
    Try it! Even the database schema will be updated on the fly.

## (Optional) Run Quarkus in JVM mode

When you're done iterating in developer mode, you can run the application as a conventional jar file.

First compile it:

```sh
./mvnw package
```

Next we need to make sure you have a PostgreSQL instance running (Quarkus automatically starts one for dev and test mode). To set up a PostgreSQL database with Docker:

```sh
docker run -it --rm=true --name quarkus_test -e POSTGRES_USER=quarkus_test -e POSTGRES_PASSWORD=quarkus_test -e POSTGRES_DB=quarkus_test -p 15432:5432 postgres:13.3
```

Connection properties for the Agroal datasource are defined in the standard Quarkus configuration file,
`src/main/resources/application.properties`.

Then run it:

```sh
java -jar ./target/quarkus-app/quarkus-run.jar
```
    Have a look at how fast it boots.
    Or measure total native memory consumption...


## See the demo in your browser

Navigate to:

<http://localhost:8080/index.html>

Have fun, and join the team of contributors!

## API Request / Response Reference

Base URL: `http://localhost:8080`

### Product APIs

#### 1) List Products
- `GET /product`

Response `200`
```json
[
    {
        "id": 1,
        "name": "TONSTAD",
        "description": null,
        "price": null,
        "stock": 10
    }
]
```

#### 2) Get Product by ID
- `GET /product/{id}`

Response `200`
```json
{
    "id": 1,
    "name": "TONSTAD",
    "description": null,
    "price": null,
    "stock": 10
}
```

Response `404`
```json
{
    "exceptionType": "jakarta.ws.rs.WebApplicationException",
    "code": 404,
    "error": "Product with id of 999 does not exist."
}
```

#### 3) Create Product
- `POST /product`

Request
```json
{
    "name": "MALM",
    "description": "Chest of drawers",
    "price": 149.99,
    "stock": 12
}
```

Response `201`
```json
{
    "id": 4,
    "name": "MALM",
    "description": "Chest of drawers",
    "price": 149.99,
    "stock": 12
}
```

#### 4) Update Product
- `PUT /product/{id}`

Request
```json
{
    "name": "MALM",
    "description": "Updated description",
    "price": 139.99,
    "stock": 10
}
```

Response `200`
```json
{
    "id": 4,
    "name": "MALM",
    "description": "Updated description",
    "price": 139.99,
    "stock": 10
}
```

#### 5) Delete Product
- `DELETE /product/{id}`

Response `204` (no body)

---

### Store APIs

#### 1) List Stores
- `GET /store`

Response `200`
```json
[
    {
        "id": 1,
        "name": "TONSTAD",
        "quantityProductsInStock": 10
    }
]
```

#### 2) Get Store by ID
- `GET /store/{id}`

Response `200`
```json
{
    "id": 1,
    "name": "TONSTAD",
    "quantityProductsInStock": 10
}
```

Response `404`
```json
{
    "exceptionType": "jakarta.ws.rs.WebApplicationException",
    "code": 404,
    "error": "Store with id of 999 does not exist."
}
```

#### 3) Create Store
- `POST /store`

Request
```json
{
    "name": "OSLO",
    "quantityProductsInStock": 25
}
```

Response `201`
```json
{
    "id": 4,
    "name": "OSLO",
    "quantityProductsInStock": 25
}
```

#### 4) Update Store
- `PUT /store/{id}`

Request
```json
{
    "name": "OSLO-CENTER",
    "quantityProductsInStock": 30
}
```

Response `200`
```json
{
    "id": 4,
    "name": "OSLO-CENTER",
    "quantityProductsInStock": 30
}
```

#### 5) Patch Store
- `PATCH /store/{id}`

Request
```json
{
    "name": "OSLO-PATCHED",
    "quantityProductsInStock": 28
}
```

Response `200`
```json
{
    "id": 4,
    "name": "OSLO-PATCHED",
    "quantityProductsInStock": 28
}
```

#### 6) Delete Store
- `DELETE /store/{id}`

Response `204` (no body)

---

### Warehouse APIs

#### 1) List Warehouses
- `GET /warehouse`

Response `200`
```json
[
    {
        "businessUnitCode": "MWH.001",
        "location": "ZWOLLE-001",
        "capacity": 100,
        "stock": 10
    }
]
```

#### 2) Get Warehouse by Business Unit Code
- `GET /warehouse/{id}`

Response `200`
```json
{
    "businessUnitCode": "MWH.001",
    "location": "ZWOLLE-001",
    "capacity": 100,
    "stock": 10
}
```

Response `404`
```json
{
    "details": "Warehouse with businessUnitCode MWH.999 does not exist."
}
```

#### 3) Create Warehouse
- `POST /warehouse`

Request
```json
{
    "businessUnitCode": "MWH.099",
    "location": "AMSTERDAM-001",
    "capacity": 20,
    "stock": 5
}
```

Response `200`
```json
{
    "businessUnitCode": "MWH.099",
    "location": "AMSTERDAM-001",
    "capacity": 20,
    "stock": 5
}
```

Response `400` (example)
```json
{
    "details": "Warehouse stock cannot exceed capacity"
}
```

#### 4) Archive Warehouse
- `DELETE /warehouse/{id}`

Response `204` (no body)

#### 5) Replace Warehouse
- `POST /warehouse/{businessUnitCode}/replacement`

Request
```json
{
    "location": "ZWOLLE-001",
    "capacity": 15,
    "stock": 10
}
```

Response `200`
```json
{
    "businessUnitCode": "MWH.001",
    "location": "ZWOLLE-001",
    "capacity": 15,
    "stock": 10
}
```

Response `400` (example)
```json
{
    "details": "Replacement warehouse stock must match current stock"
}
```

### Quick cURL examples

```sh
curl -s http://localhost:8080/product
curl -s http://localhost:8080/store
curl -s http://localhost:8080/warehouse
```

## Troubleshooting

Using **IntelliJ**, in case the generated code is not recognized and you have compilation failures, you may need to add `target/.../jaxrs` folder as "generated sources".