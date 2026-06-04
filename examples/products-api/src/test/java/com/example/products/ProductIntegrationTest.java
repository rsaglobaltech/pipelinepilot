package com.example.products;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeAll
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/products";
    }

    @Test
    @Order(1)
    void shouldCreateProduct() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Laptop",
                            "price": 999.99,
                            "quantity": 10
                        }
                        """)
        .when()
                .post()
        .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Laptop"))
                .body("price", equalTo(999.99f))
                .body("quantity", equalTo(10));
    }

    @Test
    @Order(2)
    void shouldGetProductById() {
        Integer id = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Mouse",
                            "price": 29.99,
                            "quantity": 50
                        }
                        """)
        .when()
                .post()
        .then()
                .extract().path("id");

        given()
                .pathParam("id", id)
        .when()
                .get("/{id}")
        .then()
                .statusCode(200)
                .body("name", equalTo("Mouse"))
                .body("price", equalTo(29.99f));
    }

    @Test
    @Order(3)
    void shouldListAllProducts() {
        given()
        .when()
                .get()
        .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(4)
    void shouldUpdateProduct() {
        Integer id = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Keyboard",
                            "price": 79.99,
                            "quantity": 25
                        }
                        """)
        .when()
                .post()
        .then()
                .extract().path("id");

        given()
                .pathParam("id", id)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Mechanical Keyboard",
                            "price": 149.99,
                            "quantity": 30
                        }
                        """)
        .when()
                .put("/{id}")
        .then()
                .statusCode(200)
                .body("name", equalTo("Mechanical Keyboard"))
                .body("price", equalTo(149.99f));
    }

    @Test
    @Order(5)
    void shouldDeleteProduct() {
        Integer id = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Monitor",
                            "price": 399.99,
                            "quantity": 5
                        }
                        """)
        .when()
                .post()
        .then()
                .extract().path("id");

        given()
                .pathParam("id", id)
        .when()
                .delete("/{id}")
        .then()
                .statusCode(204);

        given()
                .pathParam("id", id)
        .when()
                .get("/{id}")
        .then()
                .statusCode(404);
    }

    @Test
    @Order(6)
    void shouldReturn404ForNonExistentProduct() {
        given()
                .pathParam("id", 99999)
        .when()
                .get("/{id}")
        .then()
                .statusCode(404);
    }
}
