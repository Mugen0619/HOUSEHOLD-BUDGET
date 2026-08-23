package com.example.householdbudget.recurringtransaction;

import com.example.householdbudget.category.Category;
import com.example.householdbudget.category.CategoryRepository;
import com.example.householdbudget.category.CategoryType;
import com.example.householdbudget.common.ErrorResponse;
import com.example.householdbudget.transaction.TransactionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class RecurringTransactionControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    private Long categoryId;

    // The test Spring context (and its in-memory H2 database) is cached and
    // shared across all IT test classes for the whole test run, so each test
    // must clear shared tables before seeding its own data to avoid colliding
    // with the categories.(name, type) unique constraint.
    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        recurringTransactionRepository.deleteAll();
        categoryRepository.deleteAll();
        Category category = categoryRepository.save(new Category("住居費", CategoryType.EXPENSE));
        categoryId = category.getId();
    }

    @Test
    void createGetUpdateDelete_roundTrip() {
        RecurringTransactionRequest createRequest = new RecurringTransactionRequest(
                "家賃", 80000, CategoryType.EXPENSE, categoryId, 25, "毎月の家賃");

        ResponseEntity<RecurringTransactionResponse> createResponse = restTemplate.postForEntity(
                "/api/recurring-transactions", createRequest, RecurringTransactionResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long id = createResponse.getBody().id();
        assertThat(createResponse.getBody().executionDay()).isEqualTo(25);

        ResponseEntity<RecurringTransactionListResponse> listResponse = restTemplate.getForEntity(
                "/api/recurring-transactions", RecurringTransactionListResponse.class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody().items())
                .extracting(RecurringTransactionResponse::id)
                .contains(id);

        RecurringTransactionRequest updateRequest = new RecurringTransactionRequest(
                "家賃（更新）", 82000, CategoryType.EXPENSE, categoryId, 27, "更新後");
        ResponseEntity<RecurringTransactionResponse> updateResponse = restTemplate.exchange(
                "/api/recurring-transactions/" + id, HttpMethod.PUT,
                new HttpEntity<>(updateRequest), RecurringTransactionResponse.class);
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().name()).isEqualTo("家賃（更新）");
        assertThat(updateResponse.getBody().amount()).isEqualTo(82000);
        assertThat(updateResponse.getBody().executionDay()).isEqualTo(27);

        restTemplate.delete("/api/recurring-transactions/" + id);

        ResponseEntity<RecurringTransactionListResponse> afterDeleteResponse = restTemplate.getForEntity(
                "/api/recurring-transactions", RecurringTransactionListResponse.class);
        assertThat(afterDeleteResponse.getBody().items())
                .extracting(RecurringTransactionResponse::id)
                .doesNotContain(id);
    }

    @Test
    void create_returnsBadRequest_whenExecutionDayOutOfRange() {
        RecurringTransactionRequest request = new RecurringTransactionRequest(
                "家賃", 80000, CategoryType.EXPENSE, categoryId, 32, null);

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/recurring-transactions", request, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errors())
                .extracting(ErrorResponse.FieldErrorDetail::field)
                .contains("executionDay");
    }

    @Test
    void create_returnsBadRequest_whenCategoryTypeDoesNotMatch() {
        Category incomeCategory = categoryRepository.save(new Category("給与", CategoryType.INCOME));
        RecurringTransactionRequest request = new RecurringTransactionRequest(
                "家賃", 80000, CategoryType.EXPENSE, incomeCategory.getId(), 25, null);

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/recurring-transactions", request, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errors())
                .extracting(ErrorResponse.FieldErrorDetail::field)
                .contains("categoryId");
    }

    @Test
    void list_filtersByType() {
        recurringTransactionRepository.save(
                new RecurringTransaction("家賃", 80000, CategoryType.EXPENSE,
                        categoryRepository.findById(categoryId).orElseThrow(), 25, null));
        Category incomeCategory = categoryRepository.save(new Category("給与", CategoryType.INCOME));
        recurringTransactionRepository.save(
                new RecurringTransaction("お小遣い", 30000, CategoryType.INCOME, incomeCategory, 1, null));

        ResponseEntity<RecurringTransactionListResponse> response = restTemplate.getForEntity(
                "/api/recurring-transactions?type=INCOME", RecurringTransactionListResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().items())
                .extracting(RecurringTransactionResponse::name)
                .containsExactly("お小遣い");
    }
}
