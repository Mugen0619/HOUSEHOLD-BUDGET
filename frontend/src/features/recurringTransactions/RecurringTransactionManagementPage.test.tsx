import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { RecurringTransactionManagementPage } from './RecurringTransactionManagementPage'
import { deleteRecurringTransaction, fetchRecurringTransactions } from '../../api/recurringTransactions'
import { ApiError } from '../../api/client'

vi.mock('../../api/recurringTransactions', () => ({
  fetchRecurringTransactions: vi.fn(),
  createRecurringTransaction: vi.fn(),
  updateRecurringTransaction: vi.fn(),
  deleteRecurringTransaction: vi.fn(),
}))

const templates = [
  {
    id: 1,
    name: '家賃',
    amount: 80000,
    type: 'EXPENSE' as const,
    category: { id: 5, name: '住居費' },
    executionDay: 25,
    memo: null,
    createdAt: '2026-08-01T10:00:00',
    updatedAt: '2026-08-01T10:00:00',
  },
]

beforeEach(() => {
  vi.mocked(fetchRecurringTransactions).mockReset()
  vi.mocked(deleteRecurringTransaction).mockReset()
})

describe('RecurringTransactionManagementPage', () => {
  it('renders the template list with execution day', async () => {
    vi.mocked(fetchRecurringTransactions).mockResolvedValue(templates)

    render(<RecurringTransactionManagementPage />)

    expect(await screen.findByText('家賃')).toBeInTheDocument()
    expect(screen.getByText('毎月25日')).toBeInTheDocument()
    expect(screen.getByText('住居費')).toBeInTheDocument()
  })

  it('mentions that past generated records remain in the delete confirmation, and reloads after deletion', async () => {
    vi.mocked(fetchRecurringTransactions).mockResolvedValue(templates)
    vi.mocked(deleteRecurringTransaction).mockResolvedValue(undefined)

    render(<RecurringTransactionManagementPage />)
    await screen.findByText('家賃')

    fireEvent.click(screen.getByRole('button', { name: '削除' }))

    const dialog = await screen.findByRole('alertdialog')
    expect(within(dialog).getByText(/削除しても過去に自動生成された収支記録は残ります/)).toBeInTheDocument()

    vi.mocked(fetchRecurringTransactions).mockResolvedValue([])
    fireEvent.click(within(dialog).getByRole('button', { name: 'OK' }))

    await waitFor(() => {
      expect(deleteRecurringTransaction).toHaveBeenCalledWith(1)
    })
    await waitFor(() => {
      expect(screen.queryByText('家賃')).not.toBeInTheDocument()
    })
  })

  it('shows an error message and keeps the template in the list when delete fails', async () => {
    vi.mocked(fetchRecurringTransactions).mockResolvedValue(templates)
    vi.mocked(deleteRecurringTransaction).mockRejectedValue(
      new ApiError({ status: 500, message: 'サーバーエラー' }),
    )

    render(<RecurringTransactionManagementPage />)
    await screen.findByText('家賃')

    fireEvent.click(screen.getByRole('button', { name: '削除' }))
    const dialog = await screen.findByRole('alertdialog')
    fireEvent.click(within(dialog).getByRole('button', { name: 'OK' }))

    expect(await screen.findByText('予期しないエラーが発生しました')).toBeInTheDocument()
    expect(screen.getByText('家賃')).toBeInTheDocument()
  })
})
