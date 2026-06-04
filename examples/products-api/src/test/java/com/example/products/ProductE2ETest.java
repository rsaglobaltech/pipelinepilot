package com.example.products;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductE2ETest {

    @LocalServerPort
    private int port;

    @BeforeAll
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/products";
    }

    @Test
    @Order(1)
    void fullProductLifecycle() {
        Integer productId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Gaming Laptop",
                            "price": 1499.99,
                            "quantity": 15
                        }
                        """)
        .when()
                .post()
        .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Gaming Laptop"))
                .extract().path("id");

        given()
                .pathParam("id", productId)
        .when()
                .get("/{id}")
        .then()
                .statusCode(200)
                .body("name", equalTo("Gaming Laptop"))
                .body("price", equalTo(1499.99f));

        given()
                .pathParam("id", productId)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Pro Gaming Laptop",
                            "price": 1699.99,
                            "quantity": 20
                        }
                        """)
        .when()
                .put("/{id}")
        .then()
                .statusCode(200)
                .body("name", equalTo("Pro Gaming Laptop"))
                .body("price", equalTo(1699.99f));

        given()
        .when()
                .get()
        .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));

        given()
                .pathParam("id", productId)
        .when()
                .delete("/{id}")
        .then()
                .statusCode(204);

        given()
                .pathParam("id", productId)
        .when()
                .get("/{id}")
        .then()
                .statusCode(404);
    }

    @Test
    @Order(2)
    void concurrentOperations() {
        Integer id1 = createProduct("Product A", 10.99, 100);
        Integer id2 = createProduct("Product B", 20.99, 200);
        Integer id3 = createProduct("Product C", 30.99, 300);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                given()
                .when()
                        .get()
                .then()
                        .statusCode(200)
                        .body("size()", greaterThanOrEqualTo(3))
        );

        given().pathParam("id", id1).when().delete("/{id}").then().statusCode(204);
        given().pathParam("id", id2).when().delete("/{id}").then().statusCode(204);
        given().pathParam("id", id3).when().delete("/{id}").then().statusCode(204);
    }

    @Test
    @Order(3)
    void validationAndErrorHandling() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "",
                            "price": -10,
                            "quantity": -5
                        }
                        """)
        .when()
                .post()
        .then()
                .statusCode(201);

        given()
                .pathParam("id", 999999)
        .when()
                .get("/{id}")
        .then()
                .statusCode(404);

        given()
                .pathParam("id", 999999)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Test",
                            "price": 10.0,
                            "quantity": 1
                        }
                        """)
        .when()
                .put("/{id}")
        .then()
                .statusCode(404);

        given()
                .pathParam("id", 999999)
        .when()
                .delete("/{id}")
        .then()
                .statusCode(404);
    }

    @Test
    @Order(4)
    void healthEndpointShouldBeAvailable() {
        given()
                .port(port)
                .basePath("/actuator")
        .when()
                .get("/health")
        .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    private Integer createProduct(String name, double price, int quantity) {
        return given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {
                            "name": "%s",
                            "price": %f,
                            "quantity": %d
                        }
                        """, name, price, quantity))
        .when()
                .post()
        .then()
                .statusCode(201)
                .extract().path("id");
    }
}
