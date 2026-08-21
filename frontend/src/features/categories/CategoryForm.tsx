import { useState, type FormEvent } from 'react'
import type { Category, TransactionType } from '../../types'
import { ApiError } from '../../api/client'
import { resolveErrorMessage } from '../../utils/resolveErrorMessage'
import styles from './CategoryForm.module.css'

interface CategoryFormProps {
  type: TransactionType
  initial?: Category | null
  onSubmit: (data: { name: string }) => Promise<void>
  onCancel: () => void
}

const MAX_NAME_LENGTH = 20

export function CategoryForm({ type, initial, onSubmit, onCancel }: CategoryFormProps) {
  const [name, setName] = useState(initial?.name ?? '')
  const [nameError, setNameError] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  function validate(): string | null {
    if (!name.trim()) {
      return 'カテゴリ名は必須です'
    }
    if (name.length > MAX_NAME_LENGTH) {
      return `カテゴリ名は${MAX_NAME_LENGTH}文字以内で入力してください`
    }
    return null
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)

    const error = validate()
    setNameError(error)
    if (error) {
      return
    }

    setSubmitting(true)
    try {
      await onSubmit({ name })
    } catch (err) {
      if (err instanceof ApiError && err.errors) {
        const nameFieldError = err.errors.find((e) => e.field === 'name')
        if (nameFieldError) {
          setNameError(nameFieldError.message)
        }
      }
      setFormError(resolveErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className={styles.form} noValidate>
      {formError && (
        <p className={styles.formError} role="alert">
          {formError}
        </p>
      )}

      <div className={styles.field}>
        <span className={styles.typeLabel}>種別: {type === 'INCOME' ? '収入用' : '支出用'}</span>
      </div>

      <div className={styles.field}>
        <label htmlFor="category-name">カテゴリ名</label>
        <input
          id="category-name"
          type="text"
          value={name}
          maxLength={MAX_NAME_LENGTH}
          onChange={(e) => setName(e.target.value)}
        />
        {nameError && <p className={styles.fieldError}>{nameError}</p>}
      </div>

      <div className={styles.actions}>
        <button type="button" onClick={onCancel} disabled={submitting}>
          キャンセル
        </button>
        <button type="submit" disabled={submitting}>
          {submitting ? '保存中...' : '保存'}
        </button>
      </div>
    </form>
  )
}
