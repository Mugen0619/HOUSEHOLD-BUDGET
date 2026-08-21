import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { TransactionForm } from './TransactionForm'

vi.mock('../../api/categories', () => ({
  fetchCategories: vi.fn().mockResolvedValue([{ id: 1, name: '食費', type: 'EXPENSE' }]),
}))

describe('TransactionForm', () => {
  it('shows a validation error and does not call onSubmit when amount is 0', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    render(<TransactionForm onSubmit={onSubmit} onCancel={vi.fn()} />)

    // カテゴリの読み込み完了を待つ（読み込み中は保存ボタンが無効化される）
    await waitFor(() => {
      expect(screen.getByRole('option', { name: '食費' })).toBeInTheDocument()
    })

    fireEvent.change(screen.getByLabelText('日付'), { target: { value: '2026-08-15' } })
    fireEvent.change(screen.getByLabelText('カテゴリ'), { target: { value: '1' } })
    fireEvent.change(screen.getByLabelText('金額'), { target: { value: '0' } })

    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    expect(
      await screen.findByText('金額は1以上の整数で入力してください'),
    ).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })
})
