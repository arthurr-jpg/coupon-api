package com.arthur.coupon.infrastructure.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CouponControllerIT {

    @Autowired
    private MockMvc mockMvc;

    private static Instant inThirtyDays() {
        return Instant.now().plus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
    }

    private static String body(String code, String discount, Instant expiration, boolean published) {
        return """
                {"code":"%s","description":"Coupon description","discountValue":%s,"expirationDate":"%s","published":%s}
                """.formatted(code, discount, expiration, published);
    }

    private ResultActions postCoupon(String json) throws Exception {
        return mockMvc.perform(post("/coupon").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String createCoupon(String code) throws Exception {
        String response = postCoupon(body(code, "0.8", inThirtyDays(), false))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    // ---------- CREATE ----------

    @Test
    void createReturns201WithSanitizedCode() throws Exception {
        postCoupon(body("ABC-123", "0.8", inThirtyDays(), false))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.code").value("ABC123"))
                .andExpect(jsonPath("$.description").value("Coupon description"))
                .andExpect(jsonPath("$.discountValue").value(0.8))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.published").value(false))
                .andExpect(jsonPath("$.redeemed").value(false));
    }

    @Test
    void createPersistsTheSanitizedCodeNotTheOriginalOne() throws Exception {
        String response = postCoupon(body("A-B-C-1-2-3", "0.8", inThirtyDays(), false))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(response, "$.id");

        mockMvc.perform(get("/coupon/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ABC123"));
    }

    @Test
    void createCanReturnAlreadyPublishedCoupon() throws Exception {
        postCoupon(body("PUB123", "10", inThirtyDays(), true))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.published").value(true))
                .andExpect(jsonPath("$.discountValue").value(10));
    }

    @Test
    void createAcceptsDiscountExactlyAtTheMinimumAndVeryLargeDiscounts() throws Exception {
        postCoupon(body("MIN123", "0.5", inThirtyDays(), false)).andExpect(status().isCreated());
        postCoupon(body("BIG123", "999999999", inThirtyDays(), false))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountValue").value(999999999));
    }

    @Test
    void createRejectsCodeWithLessThanSixCharactersAfterSanitization() throws Exception {
        postCoupon(body("AB-12", "0.8", inThirtyDays(), false)).andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsCodeWithMoreThanSixCharacters() throws Exception {
        postCoupon(body("ABCDEFG", "0.8", inThirtyDays(), false)).andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsCodeMadeOnlyOfSpecialCharacters() throws Exception {
        postCoupon(body("!!!!!!", "0.8", inThirtyDays(), false)).andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsDiscountBelowMinimum() throws Exception {
        postCoupon(body("ABC123", "0.49", inThirtyDays(), false)).andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsZeroAndNegativeDiscount() throws Exception {
        postCoupon(body("ABC123", "0", inThirtyDays(), false)).andExpect(status().isBadRequest());
        postCoupon(body("ABC123", "-10", inThirtyDays(), false)).andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsExpirationDateInThePast() throws Exception {
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);

        postCoupon(body("ABC123", "0.8", past, false)).andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsMissingRequiredFields() throws Exception {
        postCoupon("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.code").exists())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.discountValue").exists())
                .andExpect(jsonPath("$.errors.expirationDate").exists());
    }

    @Test
    void createRejectsBlankDescription() throws Exception {
        String json = """
                {"code":"ABC123","description":"  ","discountValue":0.8,"expirationDate":"%s"}
                """.formatted(inThirtyDays());

        postCoupon(json).andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsMalformedJson() throws Exception {
        postCoupon("{not-json").andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsInvalidDateFormat() throws Exception {
        String json = """
                {"code":"ABC123","description":"d","discountValue":0.8,"expirationDate":"tomorrow"}
                """;

        postCoupon(json).andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsDescriptionLongerThanTheStorageLimitInsteadOfFailing() throws Exception {
        String json = """
                {"code":"ABC123","description":"%s","discountValue":0.8,"expirationDate":"%s"}
                """.formatted("x".repeat(2001), inThirtyDays());

        postCoupon(json).andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsAbsurdlyLargeDiscountInsteadOfFailing() throws Exception {
        postCoupon(body("ABC123", "1" + "0".repeat(40), inThirtyDays(), false))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAndGetReturnTheSameDiscountForValuesWithManyDecimals() throws Exception {
        String response = postCoupon(body("DEC123", "0.50004", inThirtyDays(), false))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountValue").value(0.5))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(response, "$.id");

        mockMvc.perform(get("/coupon/{id}", id))
                .andExpect(jsonPath("$.discountValue").value(0.5));
    }

    // ---------- GET ----------

    @Test
    void getReturnsExistingCoupon() throws Exception {
        String id = createCoupon("GET123");

        mockMvc.perform(get("/coupon/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.code").value("GET123"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getReturns404WhenCouponDoesNotExist() throws Exception {
        mockMvc.perform(get("/coupon/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void getReturns400WhenIdIsNotAnUuid() throws Exception {
        mockMvc.perform(get("/coupon/{id}", "not-a-uuid")).andExpect(status().isBadRequest());
    }

    // ---------- DELETE ----------

    @Test
    void deleteSoftDeletesCouponAndKeepsItsData() throws Exception {
        String id = createCoupon("DEL123");

        mockMvc.perform(delete("/coupon/{id}", id)).andExpect(status().isNoContent());

        mockMvc.perform(get("/coupon/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELETED"))
                .andExpect(jsonPath("$.code").value("DEL123"))
                .andExpect(jsonPath("$.description").value("Coupon description"))
                .andExpect(jsonPath("$.discountValue").value(0.8));
    }

    @Test
    void deleteTwiceReturns409AndTheCouponStaysDeleted() throws Exception {
        String id = createCoupon("TWI123");

        mockMvc.perform(delete("/coupon/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/coupon/{id}", id)).andExpect(status().isConflict());
        mockMvc.perform(delete("/coupon/{id}", id)).andExpect(status().isConflict());

        mockMvc.perform(get("/coupon/{id}", id))
                .andExpect(jsonPath("$.status").value("DELETED"));
    }

    @Test
    void deleteReturns404WhenCouponDoesNotExist() throws Exception {
        mockMvc.perform(delete("/coupon/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns400WhenIdIsNotAnUuid() throws Exception {
        mockMvc.perform(delete("/coupon/{id}", "123")).andExpect(status().isBadRequest());
    }
}
