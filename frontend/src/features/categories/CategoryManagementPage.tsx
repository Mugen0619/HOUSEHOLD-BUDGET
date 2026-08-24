import { useEffect, useState } from 'react'
import type { Category, TransactionType } from '../../types'
import {
  createCategory,
  deleteCategory,
  fetchCategories,
  updateCategory,
} from '../../api/categories'
import { ApiError } from '../../api/client'
import { resolveErrorMessage } from '../../utils/resolveErrorMessage'
import { Modal } from '../../components/Modal'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { CategoryForm } from './CategoryForm'
import styles from './CategoryManagementPage.module.css'

interface FormState {
  type: TransactionType
  category: Category | null
}

export function CategoryManagementPage() {
  const [categories, setCategories] = useState<Category[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [formState, setFormState] = useState<FormState | null>(null)
  const [formSubmitting, setFormSubmitting] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<Category | null>(null)
  const [deleting, setDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const data = await fetchCategories()
      setCategories(data)
    } catch (err) {
      setError(resolveErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const incomeCategories = categories.filter((c) => c.type === 'INCOME')
  const expenseCategories = categories.filter((c) => c.type === 'EXPENSE')

  function openAddForm(type: TransactionType) {
    setFormState({ type, category: null })
  }

  function openEditForm(category: Category) {
    setFormState({ type: category.type, category })
  }

  function closeForm() {
    setFormState(null)
  }

  async function handleFormSubmit(data: { name: string }) {
    if (!formState) return
    setFormSubmitting(true)
    try {
      if (formState.category) {
        await updateCategory(formState.category.id, data)
      } else {
        await createCategory({ name: data.name, type: formState.type })
      }
      closeForm()
      await load()
    } finally {
      setFormSubmitting(false)
    }
  }

  function requestDelete(category: Category) {
    setDeleteTarget(category)
    setDeleteError(null)
  }

  async function confirmDelete() {
    if (!deleteTarget) return
    setDeleting(true)
    setDeleteError(null)
    try {
      await deleteCategory(deleteTarget.id)
      setDeleteTarget(null)
      await load()
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setDeleteError('このカテゴリは使用中のため削除できません')
      } else {
        setDeleteError(resolveErrorMessage(err))
      }
    } finally {
      setDeleting(false)
    }
  }

  function renderList(title: string, type: TransactionType, list: Category[]) {
    return (
      <section className={styles.section}>
        <div className={styles.sectionHeader}>
          <h2>{title}</h2>
          <button type="button" onClick={() => openAddForm(type)} disabled={loading}>
            + 追加
          </button>
        </div>
        <ul className={styles.list}>
          {list.map((c) => (
            <li key={c.id} className={styles.listItem}>
              <span>{c.name}</span>
              <span className={styles.actions}>
                <button type="button" onClick={() => openEditForm(c)}>
                  編集
                </button>
                <button type="button" onClick={() => requestDelete(c)}>
                  削除
                </button>
              </span>
            </li>
          ))}
          {list.length === 0 && !loading && <li className={styles.empty}>カテゴリがありません</li>}
        </ul>
      </section>
    )
  }

  return (
    <div className={styles.page}>
      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}
      {loading && <p className={styles.loading}>読み込み中...</p>}

      {renderList('収入用カテゴリ', 'INCOME', incomeCategories)}
      {renderList('支出用カテゴリ', 'EXPENSE', expenseCategories)}

      {formState && (
        <Modal
          title={formState.category ? 'カテゴリの編集' : 'カテゴリの追加'}
          onClose={closeForm}
          closeDisabled={formSubmitting}
        >
          <CategoryForm
            type={formState.type}
            initial={formState.category}
            onSubmit={handleFormSubmit}
            onCancel={closeForm}
          />
        </Modal>
      )}

      {deleteTarget && (
        <ConfirmDialog
          message={`「${deleteTarget.name}」を削除しますか？`}
          onConfirm={confirmDelete}
          onCancel={() => setDeleteTarget(null)}
          confirmDisabled={deleting}
        />
      )}
      {deleteError && (
        <p className={styles.error} role="alert">
          {deleteError}
        </p>
      )}
    </div>
  )
}
