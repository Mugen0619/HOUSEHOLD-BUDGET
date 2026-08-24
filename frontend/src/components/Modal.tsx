import type { ReactNode } from 'react'
import styles from './Modal.module.css'

interface ModalProps {
  title: string
  onClose: () => void
  children: ReactNode
  closeDisabled?: boolean
}

export function Modal({ title, onClose, children, closeDisabled = false }: ModalProps) {
  function handleClose() {
    if (!closeDisabled) onClose()
  }

  return (
    <div className={styles.overlay} onClick={handleClose}>
      <div
        className={styles.dialog}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onClick={(e) => e.stopPropagation()}
      >
        <div className={styles.header}>
          <h2>{title}</h2>
          <button
            type="button"
            className={styles.closeButton}
            onClick={handleClose}
            disabled={closeDisabled}
            aria-label="閉じる"
          >
            ×
          </button>
        </div>
        <div className={styles.body}>{children}</div>
      </div>
    </div>
  )
}
