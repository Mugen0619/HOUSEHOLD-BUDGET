import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { TransactionListPage } from './TransactionListPage'
import { fetchTransactions } from '../../api/transactions'
import { fetchCategories } from '../../api/categories'
import { ApiError } from '../../api/client'

vi.mock('../../api/transactions', () => ({
  fetchTransactions: vi.fn(),
  createTransaction: vi.fn(),
  updateTransaction: vi.fn(),
  deleteTransaction: vi.fn(),
}))

vi.mock('../../api/categories', () => ({
  fetchCategories: vi.fn(),
}))

const categories = [
  { id: 1, name: '食費', type: 'EXPENSE' as const },
  { id: 2, name: '給与', type: 'INCOME' as const },
]

function lastCallParams() {
  const mock = vi.mocked(fetchTransactions).mock
  return mock.calls[mock.calls.length - 1][0]
}

beforeEach(() => {
  vi.mocked(fetchTransactions).mockReset().mockResolvedValue([])
  vi.mocked(fetchCategories).mockReset().mockResolvedValue(categories)
})

describe('TransactionListPage', () => {
  it('shows a 定期 badge only for transactions generated from a recurring template', async () => {
    vi.mocked(fetchTransactions).mockResolvedValue([
      {
        id: 1,
        date: '2026-08-25',
        amount: 80000,
        type: 'EXPENSE',
        category: { id: 1, name: '住居費' },
        memo: '家賃',
        source: 'RECURRING',
        recurringTransactionId: 10,
        createdAt: '2026-08-25T00:10:00',
        updatedAt: '2026-08-25T00:10:00',
      },
      {
        id: 2,
        date: '2026-08-15',
        amount: 1200,
        type: 'EXPENSE',
        category: { id: 1, name: '食費' },
        memo: null,
        source: 'MANUAL',
        recurringTransactionId: null,
        createdAt: '2026-08-15T20:00:00',
        updatedAt: '2026-08-15T20:00:00',
      },
    ])

    render(<TransactionListPage />)

    await waitFor(() => {
      expect(screen.getByText('定期')).toBeInTheDocument()
    })
    expect(screen.getAllByText('定期')).toHaveLength(1)
  })

  it('shows an error banner when the category fetch fails, instead of failing silently', async () => {
    vi.mocked(fetchCategories).mockRejectedValue(
      new ApiError({ status: 500, message: 'サーバーエラー' }),
    )

    render(<TransactionListPage />)

    expect(await screen.findByText('予期しないエラーが発生しました')).toBeInTheDocument()
  })

  it('refetches with the new type when the type filter changes', async () => {
    render(<TransactionListPage />)

    await waitFor(() => {
      expect(fetchTransactions).toHaveBeenCalledTimes(1)
    })
    await waitFor(() => {
      expect(fetchCategories).toHaveBeenCalled()
    })

    fireEvent.change(screen.getByLabelText('種別'), { target: { value: 'INCOME' } })

    await waitFor(() => {
      expect(lastCallParams()).toMatchObject({ type: 'INCOME' })
    })
  })

  it('refetches with the new date range when the period filters change', async () => {
    render(<TransactionListPage />)

    await waitFor(() => {
      expect(fetchTransactions).toHaveBeenCalledTimes(1)
    })

    const dateInputs = document.querySelectorAll('input[type="date"]')
    expect(dateInputs).toHaveLength(2)

    fireEvent.change(dateInputs[0], { target: { value: '2026-01-01' } })

    await waitFor(() => {
      expect(lastCallParams()).toMatchObject({ from: '2026-01-01' })
    })

    fireEvent.change(dateInputs[1], { target: { value: '2026-01-31' } })

    await waitFor(() => {
      expect(lastCallParams()).toMatchObject({ from: '2026-01-01', to: '2026-01-31' })
    })
  })

  it('refetches with the new sort field and order when the sort controls change', async () => {
    render(<TransactionListPage />)

    await waitFor(() => {
      expect(fetchTransactions).toHaveBeenCalledTimes(1)
    })
    expect(lastCallParams()).toMatchObject({ sort: 'date', order: 'desc' })

    const comboboxes = screen.getAllByRole('combobox')
    // DOM順: 種別, カテゴリ, 並び替え(フィールド), 並び替え(順序)
    const sortFieldSelect = comboboxes[2]
    const sortOrderSelect = comboboxes[3]

    fireEvent.change(sortFieldSelect, { target: { value: 'amount' } })

    await waitFor(() => {
      expect(lastCallParams()).toMatchObject({ sort: 'amount' })
    })

    fireEvent.change(sortOrderSelect, { target: { value: 'asc' } })

    await waitFor(() => {
      expect(lastCallParams()).toMatchObject({ sort: 'amount', order: 'asc' })
    })
  })
})
