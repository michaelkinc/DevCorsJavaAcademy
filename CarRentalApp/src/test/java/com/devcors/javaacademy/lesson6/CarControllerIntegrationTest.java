package com.devcors.javaacademy.lesson6;

import com.devcors.javaacademy.carrental.CarRentalApplication;
import com.devcors.javaacademy.carrental.data.entity.enums.CarColor;
import com.devcors.javaacademy.carrental.data.entity.enums.CarType;
import com.devcors.javaacademy.carrental.data.entity.Car;
import com.devcors.javaacademy.carrental.data.repository.CarRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = CarRentalApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class CarControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CarRepository carRepository;

    @AfterEach
    void afterEach() {
        carRepository.deleteAll();
    }

    private static final String LICENCE_PLATE_1 = "4H44444";
    private static final String LICENCE_PLATE_2 = "5H55555";

    private static final String BRAND_1 = "BMW";
    private static final String BRAND_2 = "Audi";

    private static final short YEAR_1 = (short) 1999;
    private static final short YEAR_2 = (short) 2002;

    private static final Car CAR_1 = Car.builder()
            .brand(BRAND_1)
            .year(YEAR_1)
            .licencePlate(LICENCE_PLATE_1)
            .color(CarColor.BLACK)
            .type(CarType.HATCHBACK)
            .build();

    private static final Car CAR_2 = Car.builder()
            .brand(BRAND_2)
            .year(YEAR_2)
            .licencePlate(LICENCE_PLATE_2)
            .color(CarColor.PINK)
            .type(CarType.COMBI)
            .build();

    @Test
    @WithMockUser(username = "user1@email.cz", authorities = { "ADMIN" }, password = "password")
    void createCarShouldReturnOk() {
        String encodedAuthString = java.util.Base64.getEncoder().encodeToString(("user1@email.cz" + ":" + "password").getBytes(StandardCharsets.UTF_8));
        webTestClient.post()
                .uri(ub -> ub.path("/cars").build())
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuthString)
                .bodyValue(CAR_1)
                .exchange()
                .expectStatus()
                .isOk();

        List<Car> cars = carRepository.findAll();
        assertFalse(CollectionUtils.isEmpty(cars));
        assertEquals(1, cars.size());
        Car car = cars.get(0);

        assertEquals(BRAND_1, car.getBrand());
        assertEquals(YEAR_1, car.getYear());
        assertEquals(LICENCE_PLATE_1, car.getLicencePlate());
        assertEquals(CarColor.BLACK, car.getColor());
        assertEquals(CarType.HATCHBACK, car.getType());
    }

    @Test
    @WithMockUser(username = "user1@email.cz", authorities = { "ADMIN" }, password = "password")
    void updateCarShouldReturnOk() {
        String encodedAuthString = java.util.Base64.getEncoder().encodeToString(("user1@email.cz" + ":" + "password").getBytes(StandardCharsets.UTF_8));
        Car savedCar = carRepository.save(CAR_1);
        Integer carId = savedCar.getId();

        savedCar.setId(null);
        savedCar.setBrand(BRAND_2);
        webTestClient.patch()
                .uri(ub -> ub.path("/cars/" + carId).build())
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuthString)
                .bodyValue(savedCar)
                .exchange()
                .expectStatus()
                .isOk();

        Optional<Car> car = carRepository.findById(carId);
        assertTrue(car.isPresent());
        assertEquals(BRAND_2, car.get().getBrand());
    }

    @Test
    @WithMockUser(username = "user1@email.cz", authorities = { "ADMIN" }, password = "password")
    void updateCarByIdShouldReturnBadRequest() {
        String encodedAuthString = java.util.Base64.getEncoder().encodeToString(("user1@email.cz" + ":" + "password").getBytes(StandardCharsets.UTF_8));
        Car savedCar = carRepository.save(CAR_1);

        webTestClient.put()
                .uri(ub -> ub.path("/cars/" + (savedCar.getId() + 1)).build())
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuthString)
                .bodyValue(savedCar)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void getAllCarsShouldReturnOk() {
        carRepository.save(CAR_1);
        carRepository.save(CAR_2);

        List<Car> cars = webTestClient.get()
                .uri(ub -> ub.path("/cars").build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Car.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(cars);
        assertEquals(2, cars.size());
    }

    @Test
    @WithMockUser(username = "user1@email.cz", authorities = { "ADMIN" }, password = "password")
    void getCarByIdShouldReturnOk() {
        String encodedAuthString = java.util.Base64.getEncoder().encodeToString(("user1@email.cz" + ":" + "password").getBytes(StandardCharsets.UTF_8));
        Car savedCar = carRepository.save(CAR_1);

        Car car = webTestClient.get()
                .uri(ub -> ub.path("/cars/" + savedCar.getId()).build())
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuthString)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Car.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(car);
        assertEquals(BRAND_1, car.getBrand());
    }

    @Test
    void getCarsByBrandShouldReturnOk() {
        carRepository.save(CAR_1);
        carRepository.save(CAR_2);

        List<Car> cars = webTestClient.get()
                .uri(ub -> ub.path("/cars/filter").queryParam("brandName", BRAND_1).build())

                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Car.class)
                .returnResult()
                .getResponseBody();

        assertFalse(CollectionUtils.isEmpty(cars));
        assertEquals(1, cars.size());
        assertEquals(LICENCE_PLATE_1, cars.get(0).getLicencePlate());
    }

    @Test
    @WithMockUser(username = "user1@email.cz", authorities = { "ADMIN" }, password = "password")
    void getCarByIdShouldReturnNotFound() {
        String encodedAuthString = java.util.Base64.getEncoder().encodeToString(("user1@email.cz" + ":" + "password").getBytes(StandardCharsets.UTF_8));
        Car savedCar = carRepository.save(CAR_1);

        webTestClient.get()
                .uri(ub -> ub.path("/cars/" + (savedCar.getId() + 1)).build())
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuthString)
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    @WithMockUser(username = "user1@email.cz", authorities = { "ADMIN" }, password = "password")
    void deleteCarShouldReturnOk() {
        String encodedAuthString = java.util.Base64.getEncoder().encodeToString(("user1@email.cz" + ":" + "password").getBytes(StandardCharsets.UTF_8));
        Car savedCar = carRepository.save(CAR_1);
        carRepository.save(CAR_2);

        webTestClient.delete()
                .uri(ub -> ub.path("/cars/" + savedCar.getId()).build())
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuthString)
                .exchange()
                .expectStatus()
                .isOk();

        List<Car> cars = carRepository.findAll();
        assertFalse(CollectionUtils.isEmpty(cars));
        assertEquals(1, cars.size());
        assertEquals(LICENCE_PLATE_2, cars.get(0).getLicencePlate());
    }
}
