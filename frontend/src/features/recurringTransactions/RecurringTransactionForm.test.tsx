import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { RecurringTransactionForm } from './RecurringTransactionForm'

vi.mock('../../api/categories', () => ({
  fetchCategories: vi.fn().mockResolvedValue([{ id: 1, name: '住居費', type: 'EXPENSE' }]),
}))

describe('RecurringTransactionForm', () => {
  it('shows a validation error and does not call onSubmit when executionDay is out of range', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    render(<RecurringTransactionForm onSubmit={onSubmit} onCancel={vi.fn()} />)

    // カテゴリの読み込み完了を待つ（読み込み中は保存ボタンが無効化される）
    await waitFor(() => {
      expect(screen.getByRole('option', { name: '住居費' })).toBeInTheDocument()
    })

    fireEvent.change(screen.getByLabelText('名称'), { target: { value: '家賃' } })
    fireEvent.change(screen.getByLabelText('カテゴリ'), { target: { value: '1' } })
    fireEvent.change(screen.getByLabelText('金額'), { target: { value: '80000' } })
    fireEvent.change(screen.getByLabelText('実行日'), { target: { value: '32' } })

    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    expect(
      await screen.findByText('実行日は1〜31の範囲で入力してください'),
    ).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('submits the entered values converted to numbers', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    render(<RecurringTransactionForm onSubmit={onSubmit} onCancel={vi.fn()} />)

    await waitFor(() => {
      expect(screen.getByRole('option', { name: '住居費' })).toBeInTheDocument()
    })

    fireEvent.change(screen.getByLabelText('名称'), { target: { value: '家賃' } })
    fireEvent.change(screen.getByLabelText('カテゴリ'), { target: { value: '1' } })
    fireEvent.change(screen.getByLabelText('金額'), { target: { value: '80000' } })
    fireEvent.change(screen.getByLabelText('実行日'), { target: { value: '25' } })

    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith({
        name: '家賃',
        amount: 80000,
        type: 'EXPENSE',
        categoryId: 1,
        executionDay: 25,
        memo: undefined,
      })
    })
  })
})
