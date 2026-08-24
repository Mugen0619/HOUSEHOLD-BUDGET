package com.example.householdbudget.common;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // Issue #21 バグ5：カテゴリの同時作成/リネームで一意制約違反が起きた場合、
    // 削除不可の409ではなく、名称重複を示す400を返す。
    @Test
    void handleDataIntegrityViolation_returnsBadRequestWithNameField_whenUniqueConstraintViolated() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "could not execute statement; SQL [n/a]; constraint [uq_categories_name_type]; "
                        + "nested exception is org.hibernate.exception.ConstraintViolationException: "
                        + "duplicate key value violates unique constraint \"uq_categories_name_type\"");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().errors())
                .extracting(ErrorResponse.FieldErrorDetail::field)
                .contains("name");
    }

    @Test
    void handleDataIntegrityViolation_returnsConflict_whenForeignKeyRestrictViolated() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "update or delete on table \"categories\" violates foreign key constraint "
                        + "\"transactions_category_id_fkey\" on table \"transactions\"");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).isEqualTo("このカテゴリは使用中のため削除できません");
    }
}
