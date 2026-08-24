import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { ConfirmDialog } from './ConfirmDialog'

describe('ConfirmDialog', () => {
  it('calls onCancel when the overlay is clicked and confirmDisabled is false', () => {
    const onCancel = vi.fn()
    render(<ConfirmDialog message="削除しますか？" onConfirm={vi.fn()} onCancel={onCancel} />)

    fireEvent.click(screen.getByRole('alertdialog').parentElement!)

    expect(onCancel).toHaveBeenCalledTimes(1)
  })

  it('does not call onCancel when the overlay is clicked while confirmDisabled (deletion in progress)', () => {
    const onCancel = vi.fn()
    render(
      <ConfirmDialog
        message="削除しますか？"
        onConfirm={vi.fn()}
        onCancel={onCancel}
        confirmDisabled
      />,
    )

    fireEvent.click(screen.getByRole('alertdialog').parentElement!)

    expect(onCancel).not.toHaveBeenCalled()
  })

  it('disables the cancel button while confirmDisabled (deletion in progress)', () => {
    const onCancel = vi.fn()
    render(
      <ConfirmDialog
        message="削除しますか？"
        onConfirm={vi.fn()}
        onCancel={onCancel}
        confirmDisabled
      />,
    )

    const cancelButton = screen.getByRole('button', { name: 'キャンセル' })
    expect(cancelButton).toBeDisabled()
    fireEvent.click(cancelButton)
    expect(onCancel).not.toHaveBeenCalled()
  })
})
