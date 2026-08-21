package com.example.householdbudget.transaction;

import java.time.LocalDate;

import com.example.householdbudget.category.Category;
import com.example.householdbudget.category.CategoryRepository;
import com.example.householdbudget.category.CategoryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class TransactionControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CategoryRepository categoryRepository;

    private Long categoryId;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.save(new Category("食費", CategoryType.EXPENSE));
        categoryId = category.getId();
    }

    @Test
    void createGetDelete_roundTrip() {
        TransactionRequest createRequest = new TransactionRequest(
                LocalDate.of(2026, 8, 15), 1200, CategoryType.EXPENSE, categoryId, "スーパーで食料品購入");

        ResponseEntity<TransactionResponse> createResponse =
                restTemplate.postForEntity("/api/transactions", createRequest, TransactionResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long id = createResponse.getBody().id();

        ResponseEntity<TransactionListResponse> listResponse = restTemplate.getForEntity(
                "/api/transactions?from=2026-08-01&to=2026-08-31", TransactionListResponse.class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody().items())
                .extracting(TransactionResponse::id)
                .contains(id);

        restTemplate.delete("/api/transactions/" + id);

        ResponseEntity<TransactionListResponse> afterDeleteResponse = restTemplate.getForEntity(
                "/api/transactions?from=2026-08-01&to=2026-08-31", TransactionListResponse.class);
        assertThat(afterDeleteResponse.getBody().items())
                .extracting(TransactionResponse::id)
                .doesNotContain(id);
    }
}
