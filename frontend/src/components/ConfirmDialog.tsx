import styles from './ConfirmDialog.module.css'

interface ConfirmDialogProps {
  message: string
  onConfirm: () => void
  onCancel: () => void
  confirmLabel?: string
  cancelLabel?: string
  confirmDisabled?: boolean
}

export function ConfirmDialog({
  message,
  onConfirm,
  onCancel,
  confirmLabel = 'OK',
  cancelLabel = 'キャンセル',
  confirmDisabled = false,
}: ConfirmDialogProps) {
  function handleCancel() {
    if (!confirmDisabled) onCancel()
  }

  return (
    <div className={styles.overlay} onClick={handleCancel}>
      <div
        className={styles.dialog}
        role="alertdialog"
        aria-modal="true"
        onClick={(e) => e.stopPropagation()}
      >
        <p className={styles.message}>{message}</p>
        <div className={styles.actions}>
          <button
            type="button"
            onClick={handleCancel}
            disabled={confirmDisabled}
            className={styles.cancelButton}
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={confirmDisabled}
            className={styles.confirmButton}
          >
            {confirmDisabled ? '削除中...' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
