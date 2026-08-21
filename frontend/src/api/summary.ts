import { apiClient } from './client'
import type { Summary } from '../types'

export function fetchSummary(month: string): Promise<Summary> {
  return apiClient.get<Summary>(`/summary?month=${month}`)
}
