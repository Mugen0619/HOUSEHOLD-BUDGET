import { apiClient } from './client'
import type { CreateRecurringTransactionRequest, RecurringTransaction, TransactionType } from '../types'

interface RecurringTransactionListResponse {
  items: RecurringTransaction[]
}

export async function fetchRecurringTransactions(type?: TransactionType): Promise<RecurringTransaction[]> {
  const qs = type ? `?type=${type}` : ''
  const res = await apiClient.get<RecurringTransactionListResponse>(`/recurring-transactions${qs}`)
  return res.items
}

export function createRecurringTransaction(
  data: CreateRecurringTransactionRequest,
): Promise<RecurringTransaction> {
  return apiClient.post<RecurringTransaction>('/recurring-transactions', data)
}

export function updateRecurringTransaction(
  id: number,
  data: CreateRecurringTransactionRequest,
): Promise<RecurringTransaction> {
  return apiClient.put<RecurringTransaction>(`/recurring-transactions/${id}`, data)
}

export function deleteRecurringTransaction(id: number): Promise<void> {
  return apiClient.delete<void>(`/recurring-transactions/${id}`)
}
