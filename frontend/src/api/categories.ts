import { apiClient } from './client'
import type { Category, TransactionType } from '../types'

export function fetchCategories(type?: TransactionType): Promise<Category[]> {
  const qs = type ? `?type=${type}` : ''
  return apiClient.get<Category[]>(`/categories${qs}`)
}

export interface CreateCategoryRequest {
  name: string
  type: TransactionType
}

export interface UpdateCategoryRequest {
  name: string
}

export function createCategory(data: CreateCategoryRequest): Promise<Category> {
  return apiClient.post<Category>('/categories', data)
}

export function updateCategory(id: number, data: UpdateCategoryRequest): Promise<Category> {
  return apiClient.put<Category>(`/categories/${id}`, data)
}

export function deleteCategory(id: number): Promise<void> {
  return apiClient.delete<void>(`/categories/${id}`)
}
