package com.example.householdbudget.transaction;

import java.util.List;

public record TransactionListResponse(List<TransactionResponse> items) {
}
