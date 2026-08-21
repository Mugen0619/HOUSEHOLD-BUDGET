import { describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { SummaryPage } from './SummaryPage'

vi.mock('../../api/summary', () => ({
  fetchSummary: vi.fn().mockResolvedValue({
    month: '2026-08',
    incomeTotal: 0,
    expenseTotal: 0,
    balance: 0,
    incomeByCategory: [],
    expenseByCategory: [],
  }),
}))

describe('SummaryPage', () => {
  it('shows "データがありません" and hides the pie charts when category breakdowns are empty', async () => {
    const { container } = render(<SummaryPage />)

    await waitFor(() => {
      // 支出内訳・収入内訳のグラフ欄 + 支出/収入カテゴリ別内訳の表欄で計4箇所
      expect(screen.getAllByText('データがありません')).toHaveLength(4)
    })

    expect(container.querySelectorAll('canvas')).toHaveLength(0)
  })
})
