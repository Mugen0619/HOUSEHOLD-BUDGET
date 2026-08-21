package com.example.householdbudget.category;

import java.time.LocalDate;

import com.example.householdbudget.common.ErrorResponse;
import com.example.householdbudget.transaction.Transaction;
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
class CategoryControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    // The test Spring context (and its in-memory H2 database) is cached and
    // shared across all IT test classes for the whole test run, so each test
    // must clear shared tables before seeding its own data to avoid colliding
    // with the categories.(name, type) unique constraint.
    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void list_returnsAllCategories_andFiltersByType() {
        categoryRepository.save(new Category("食費", CategoryType.EXPENSE));
        categoryRepository.save(new Category("給与", CategoryType.INCOME));

        ResponseEntity<CategoryResponse[]> allResponse =
                restTemplate.getForEntity("/api/categories", CategoryResponse[].class);
        assertThat(allResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(allResponse.getBody())
                .extracting(CategoryResponse::name)
                .contains("食費", "給与");

        ResponseEntity<CategoryResponse[]> incomeResponse =
                restTemplate.getForEntity("/api/categories?type=INCOME", CategoryResponse[].class);
        assertThat(incomeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(incomeResponse.getBody())
                .extracting(CategoryResponse::type)
                .containsOnly(CategoryType.INCOME);
        assertThat(incomeResponse.getBody())
                .extracting(CategoryResponse::name)
                .contains("給与")
                .doesNotContain("食費");
    }

    @Test
    void create_returnsCreated() {
        CategoryRequest request = new CategoryRequest("交通費", CategoryType.EXPENSE);

        ResponseEntity<CategoryResponse> response =
                restTemplate.postForEntity("/api/categories", request, CategoryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().name()).isEqualTo("交通費");
        assertThat(response.getBody().type()).isEqualTo(CategoryType.EXPENSE);
        assertThat(response.getBody().id()).isNotNull();
    }

    @Test
    void create_returnsBadRequest_whenNameDuplicatedWithinSameType() {
        categoryRepository.save(new Category("食費", CategoryType.EXPENSE));
        CategoryRequest request = new CategoryRequest("食費", CategoryType.EXPENSE);

        ResponseEntity<ErrorResponse> response =
                restTemplate.postForEntity("/api/categories", request, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().errors())
                .extracting(ErrorResponse.FieldErrorDetail::field)
                .contains("name");
    }

    @Test
    void update_changesNameOnly() {
        Category category = categoryRepository.save(new Category("食費", CategoryType.EXPENSE));
        CategoryUpdateRequest request = new CategoryUpdateRequest("食費（更新）");

        ResponseEntity<CategoryResponse> response = restTemplate.exchange(
                "/api/categories/" + category.getId(), HttpMethod.PUT,
                new HttpEntity<>(request), CategoryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().name()).isEqualTo("食費（更新）");
        assertThat(response.getBody().type()).isEqualTo(CategoryType.EXPENSE);
    }

    @Test
    void delete_removesUnusedCategory() {
        Category category = categoryRepository.save(new Category("使わないカテゴリ", CategoryType.EXPENSE));

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/categories/" + category.getId(), HttpMethod.DELETE, null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(categoryRepository.findById(category.getId())).isEmpty();
    }

    @Test
    void delete_returnsConflict_whenCategoryIsReferencedByTransaction() {
        Category category = categoryRepository.save(new Category("食費", CategoryType.EXPENSE));
        transactionRepository.save(new Transaction(
                LocalDate.of(2026, 8, 1), 1000, CategoryType.EXPENSE, category, null));

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/categories/" + category.getId(), HttpMethod.DELETE, null, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).isNotBlank();
        assertThat(categoryRepository.findById(category.getId())).isPresent();
    }
}
