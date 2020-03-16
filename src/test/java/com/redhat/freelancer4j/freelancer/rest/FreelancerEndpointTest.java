package com.redhat.freelancer4j.freelancer.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FreelancerEndpointTest {

    @LocalServerPort
    private int port;

    @Before
    public void beforeTest() throws Exception {
        RestAssured.baseURI = String.format("http://localhost:%d/freelancers", port);
    }

    @Test
    public void retrieveFreelancerById() throws Exception {
        given().get("/{freelancerId}", "1")
            .then()
            .assertThat()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("freelancerId", equalTo("1"))
            .body("firstName", equalTo("Ken"))
            .body("lastName", equalTo("Yasuda"))
            .body("skills", hasItem("ruby"));
    }
    
    @Test
    public void retrieveNotExistFreelancerById() throws Exception {
        given().get("/{freelancerId}", "100")
            .then()
            .assertThat()
            .statusCode(204);
    }
    
    @Test
    public void retrieveFreelancers() throws Exception {
        given().get("/")
            .then(	)
            .assertThat()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("size()", equalTo(3));
    }
}