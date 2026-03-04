package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
public class WarehouseEndpointIT {

  private static final String PATH = "warehouse";

  @Test
  public void testSimpleListWarehouses() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  public void testGetWarehouseByBusinessUnitCode() {
    given()
        .when()
        .get(PATH + "/MWH.001")
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("ZWOLLE-001"));
  }

  @Test
  public void testArchiveWarehouseShouldHideFromActiveList() {
    given().when().delete(PATH + "/MWH.023").then().statusCode(204);

    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(not(containsString("MWH.023")), containsString("MWH.001"), containsString("MWH.012"));
  }

  @Test
  public void testCreateWarehouseShouldReturn201() {
    String payload =
        """
        {
          "businessUnitCode": "MWH.099",
          "location": "AMSTERDAM-001",
          "capacity": 20,
          "stock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post(PATH)
        .then()
        .statusCode(200)
        .body(containsString("MWH.099"), containsString("AMSTERDAM-001"));
  }

  @Test
  public void testCreateWarehouseShouldValidateStockAgainstCapacity() {
    String payload =
        """
        {
          "businessUnitCode": "MWH.100",
          "location": "AMSTERDAM-001",
          "capacity": 10,
          "stock": 99
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  public void testReplaceWarehouseShouldArchiveOldAndCreateNewActive() {
    String payload =
        """
        {
          "location": "ZWOLLE-001",
          "capacity": 15,
          "stock": 10
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post(PATH + "/MWH.001/replacement")
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("ZWOLLE-001"));

    given()
        .when()
        .get(PATH + "/MWH.001")
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("ZWOLLE-001"));
  }

  @Test
  public void testReplaceWarehouseShouldValidateStockMatching() {
    String payload =
        """
        {
          "location": "ZWOLLE-001",
          "capacity": 15,
          "stock": 9
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post(PATH + "/MWH.001/replacement")
        .then()
        .statusCode(400);
  }
}
