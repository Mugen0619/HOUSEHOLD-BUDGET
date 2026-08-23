export type TransactionType = 'INCOME' | 'EXPENSE'
export type TransactionSource = 'MANUAL' | 'RECURRING'

export interface CategoryRef {
  id: number
  name: string
}

export interface Category {
  id: number
  name: string
  type: TransactionType
}

export interface Transaction {
  id: number
  date: string
  amount: number
  type: TransactionType
  category: CategoryRef
  memo: string | null
  source: TransactionSource
  recurringTransactionId: number | null
  createdAt: string
  updatedAt: string
}

export interface CreateTransactionRequest {
  date: string
  amount: number
  type: TransactionType
  categoryId: number
  memo?: string
}

export interface RecurringTransaction {
  id: number
  name: string
  amount: number
  type: TransactionType
  category: CategoryRef
  executionDay: number
  memo: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateRecurringTransactionRequest {
  name: string
  amount: number
  type: TransactionType
  categoryId: number
  executionDay: number
  memo?: string
}

export interface CategoryAmount {
  categoryId: number
  name: string
  amount: number
}

export interface Summary {
  month: string
  incomeTotal: number
  expenseTotal: number
  balance: number
  incomeByCategory: CategoryAmount[]
  expenseByCategory: CategoryAmount[]
}

export interface ApiFieldError {
  field: string
  message: string
}

export interface ApiErrorBody {
  status: number
  message: string
  errors?: ApiFieldError[]
}
