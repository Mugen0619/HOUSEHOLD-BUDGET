import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { CategoryManagementPage } from './CategoryManagementPage'
import { deleteCategory, fetchCategories } from '../../api/categories'
import { ApiError } from '../../api/client'

vi.mock('../../api/categories', () => ({
  fetchCategories: vi.fn(),
  deleteCategory: vi.fn(),
  createCategory: vi.fn(),
  updateCategory: vi.fn(),
}))

const categories = [{ id: 1, name: '食費', type: 'EXPENSE' as const }]

describe('CategoryManagementPage', () => {
  it('shows the in-use error and keeps the category in the list when delete returns 409', async () => {
    vi.mocked(fetchCategories).mockResolvedValue(categories)
    vi.mocked(deleteCategory).mockRejectedValue(
      new ApiError({ status: 409, message: 'このカテゴリは使用中のため削除できません' }),
    )

    render(<CategoryManagementPage />)

    await waitFor(() => {
      expect(screen.getByText('食費')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: '削除' }))

    const dialog = await screen.findByRole('alertdialog')
    fireEvent.click(within(dialog).getByRole('button', { name: 'OK' }))

    expect(
      await screen.findByText('このカテゴリは使用中のため削除できません'),
    ).toBeInTheDocument()

    // カテゴリは一覧から消えていないこと
    expect(screen.getByText('食費')).toBeInTheDocument()
    expect(deleteCategory).toHaveBeenCalledWith(1)
    // 削除失敗時は再読み込みされないので、初回ロードの1回のみ呼ばれる
    expect(fetchCategories).toHaveBeenCalledTimes(1)
  })
})
