import { useEffect, useState, type FormEvent } from 'react'
import type { Category, CreateRecurringTransactionRequest, RecurringTransaction, TransactionType } from '../../types'
import { fetchCategories } from '../../api/categories'
import { ApiError } from '../../api/client'
import { resolveErrorMessage } from '../../utils/resolveErrorMessage'
import styles from './RecurringTransactionForm.module.css'

interface RecurringTransactionFormProps {
  initial?: RecurringTransaction | null
  onSubmit: (data: CreateRecurringTransactionRequest) => Promise<void>
  onCancel: () => void
}

interface FieldErrors {
  name?: string
  type?: string
  categoryId?: string
  amount?: string
  executionDay?: string
  memo?: string
}

const MAX_NAME_LENGTH = 20
const MAX_MEMO_LENGTH = 500

export function RecurringTransactionForm({ initial, onSubmit, onCancel }: RecurringTransactionFormProps) {
  const [name, setName] = useState(initial?.name ?? '')
  const [type, setType] = useState<TransactionType>(initial?.type ?? 'EXPENSE')
  const [categoryId, setCategoryId] = useState<string>(
    initial ? String(initial.category.id) : '',
  )
  const [amount, setAmount] = useState<string>(initial ? String(initial.amount) : '')
  const [executionDay, setExecutionDay] = useState<string>(
    initial ? String(initial.executionDay) : '',
  )
  const [memo, setMemo] = useState(initial?.memo ?? '')

  const [categories, setCategories] = useState<Category[]>([])
  const [categoriesLoading, setCategoriesLoading] = useState(true)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    let cancelled = false
    setCategoriesLoading(true)
    fetchCategories()
      .then((data) => {
        if (!cancelled) setCategories(data)
      })
      .catch((err) => {
        if (!cancelled) setFormError(resolveErrorMessage(err))
      })
      .finally(() => {
        if (!cancelled) setCategoriesLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  const categoryOptions = categories.filter((c) => c.type === type)

  function handleTypeChange(next: TransactionType) {
    setType(next)
    setCategoryId('')
  }

  function validate(): FieldErrors {
    const errors: FieldErrors = {}

    if (!name.trim()) {
      errors.name = '名称は必須です'
    } else if (name.length > MAX_NAME_LENGTH) {
      errors.name = `名称は${MAX_NAME_LENGTH}文字以内で入力してください`
    }

    if (!categoryId) {
      errors.categoryId = 'カテゴリは必須です'
    }

    if (amount === '') {
      errors.amount = '金額は必須です'
    } else {
      const amountNum = Number(amount)
      if (!Number.isInteger(amountNum) || amountNum < 1) {
        errors.amount = '金額は1以上の整数で入力してください'
      }
    }

    if (executionDay === '') {
      errors.executionDay = '実行日は必須です'
    } else {
      const dayNum = Number(executionDay)
      if (!Number.isInteger(dayNum) || dayNum < 1 || dayNum > 31) {
        errors.executionDay = '実行日は1〜31の範囲で入力してください'
      }
    }

    if (memo.length > MAX_MEMO_LENGTH) {
      errors.memo = `メモは${MAX_MEMO_LENGTH}文字以内で入力してください`
    }

    return errors
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)

    const errors = validate()
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) {
      return
    }

    setSubmitting(true)
    try {
      await onSubmit({
        name,
        amount: Number(amount),
        type,
        categoryId: Number(categoryId),
        executionDay: Number(executionDay),
        memo: memo === '' ? undefined : memo,
      })
    } catch (err) {
      if (err instanceof ApiError && err.errors && err.errors.length > 0) {
        const next: FieldErrors = {}
        for (const fieldError of err.errors) {
          next[fieldError.field as keyof FieldErrors] = fieldError.message
        }
        setFieldErrors(next)
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
        <label htmlFor="rt-name">名称</label>
        <input
          id="rt-name"
          type="text"
          value={name}
          maxLength={MAX_NAME_LENGTH}
          onChange={(e) => setName(e.target.value)}
        />
        {fieldErrors.name && <p className={styles.fieldError}>{fieldErrors.name}</p>}
      </div>

      <div className={styles.field}>
        <span className={styles.legend}>種別</span>
        <label className={styles.radioLabel}>
          <input
            type="radio"
            name="rt-type"
            value="INCOME"
            checked={type === 'INCOME'}
            onChange={() => handleTypeChange('INCOME')}
          />
          収入
        </label>
        <label className={styles.radioLabel}>
          <input
            type="radio"
            name="rt-type"
            value="EXPENSE"
            checked={type === 'EXPENSE'}
            onChange={() => handleTypeChange('EXPENSE')}
          />
          支出
        </label>
        {fieldErrors.type && <p className={styles.fieldError}>{fieldErrors.type}</p>}
      </div>

      <div className={styles.field}>
        <label htmlFor="rt-category">カテゴリ</label>
        <select
          id="rt-category"
          value={categoryId}
          onChange={(e) => setCategoryId(e.target.value)}
          disabled={categoriesLoading}
        >
          <option value="">選択してください</option>
          {categoryOptions.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        {fieldErrors.categoryId && <p className={styles.fieldError}>{fieldErrors.categoryId}</p>}
      </div>

      <div className={styles.field}>
        <label htmlFor="rt-amount">金額</label>
        <div className={styles.amountRow}>
          <input
            id="rt-amount"
            type="number"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
          />
          <span>円</span>
        </div>
        {fieldErrors.amount && <p className={styles.fieldError}>{fieldErrors.amount}</p>}
      </div>

      <div className={styles.field}>
        <label htmlFor="rt-execution-day">実行日</label>
        <div className={styles.amountRow}>
          <span>毎月</span>
          <input
            id="rt-execution-day"
            type="number"
            min={1}
            max={31}
            value={executionDay}
            onChange={(e) => setExecutionDay(e.target.value)}
          />
          <span>日</span>
        </div>
        <p className={styles.hint}>31日等、存在しない月は月末に生成されます</p>
        {fieldErrors.executionDay && <p className={styles.fieldError}>{fieldErrors.executionDay}</p>}
      </div>

      <div className={styles.field}>
        <label htmlFor="rt-memo">メモ（任意・500文字以内）</label>
        <textarea id="rt-memo" value={memo} onChange={(e) => setMemo(e.target.value)} />
        {fieldErrors.memo && <p className={styles.fieldError}>{fieldErrors.memo}</p>}
      </div>

      <div className={styles.actions}>
        <button type="button" onClick={onCancel} disabled={submitting}>
          キャンセル
        </button>
        <button type="submit" disabled={submitting || categoriesLoading}>
          {submitting ? '保存中...' : '保存'}
        </button>
      </div>
    </form>
  )
}
