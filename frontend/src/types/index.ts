export type TransactionType = 'INCOME' | 'EXPENSE'

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
