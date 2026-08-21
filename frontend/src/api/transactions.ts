import { apiClient } from './client'
import type { CreateTransactionRequest, Transaction, TransactionType } from '../types'

export interface TransactionListParams {
  from?: string
  to?: string
  type?: TransactionType
  categoryId?: number
  sort?: 'date' | 'amount'
  order?: 'asc' | 'desc'
}

interface TransactionListResponse {
  items: Transaction[]
}

function buildQuery(params: TransactionListParams): string {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') {
      query.set(key, String(value))
    }
  }
  const qs = query.toString()
  return qs ? `?${qs}` : ''
}

export async function fetchTransactions(params: TransactionListParams): Promise<Transaction[]> {
  const res = await apiClient.get<TransactionListResponse>(`/transactions${buildQuery(params)}`)
  return res.items
}

export function createTransaction(data: CreateTransactionRequest): Promise<Transaction> {
  return apiClient.post<Transaction>('/transactions', data)
}

export function updateTransaction(
  id: number,
  data: CreateTransactionRequest,
): Promise<Transaction> {
  return apiClient.put<Transaction>(`/transactions/${id}`, data)
}

export function deleteTransaction(id: number): Promise<void> {
  return apiClient.delete<void>(`/transactions/${id}`)
}
