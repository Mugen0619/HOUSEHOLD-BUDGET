import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { Modal } from './Modal'

describe('Modal', () => {
  it('calls onClose when the overlay is clicked and closeDisabled is false', () => {
    const onClose = vi.fn()
    render(
      <Modal title="タイトル" onClose={onClose}>
        <p>内容</p>
      </Modal>,
    )

    fireEvent.click(screen.getByRole('dialog').parentElement!)

    expect(onClose).toHaveBeenCalledTimes(1)
  })

  it('does not call onClose when the overlay is clicked while closeDisabled (saving in progress)', () => {
    const onClose = vi.fn()
    render(
      <Modal title="タイトル" onClose={onClose} closeDisabled>
        <p>内容</p>
      </Modal>,
    )

    fireEvent.click(screen.getByRole('dialog').parentElement!)

    expect(onClose).not.toHaveBeenCalled()
  })

  it('disables the close button while closeDisabled (saving in progress)', () => {
    const onClose = vi.fn()
    render(
      <Modal title="タイトル" onClose={onClose} closeDisabled>
        <p>内容</p>
      </Modal>,
    )

    const closeButton = screen.getByRole('button', { name: '閉じる' })
    expect(closeButton).toBeDisabled()
    fireEvent.click(closeButton)
    expect(onClose).not.toHaveBeenCalled()
  })
})
