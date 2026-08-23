import { useEffect, useState } from 'react'
import type { CreateRecurringTransactionRequest, RecurringTransaction } from '../../types'
import {
  createRecurringTransaction,
  deleteRecurringTransaction,
  fetchRecurringTransactions,
  updateRecurringTransaction,
} from '../../api/recurringTransactions'
import { resolveErrorMessage } from '../../utils/resolveErrorMessage'
import { Modal } from '../../components/Modal'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { RecurringTransactionForm } from './RecurringTransactionForm'
import styles from './RecurringTransactionManagementPage.module.css'

export function RecurringTransactionManagementPage() {
  const [templates, setTemplates] = useState<RecurringTransaction[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [modalOpen, setModalOpen] = useState(false)
  const [editingTemplate, setEditingTemplate] = useState<RecurringTransaction | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<RecurringTransaction | null>(null)
  const [deleting, setDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const items = await fetchRecurringTransactions()
      setTemplates(items)
    } catch (err) {
      setError(resolveErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  function openCreateModal() {
    setEditingTemplate(null)
    setModalOpen(true)
  }

  function openEditModal(template: RecurringTransaction) {
    setEditingTemplate(template)
    setModalOpen(true)
  }

  function closeModal() {
    setModalOpen(false)
    setEditingTemplate(null)
  }

  async function handleFormSubmit(data: CreateRecurringTransactionRequest) {
    if (editingTemplate) {
      await updateRecurringTransaction(editingTemplate.id, data)
    } else {
      await createRecurringTransaction(data)
    }
    closeModal()
    await load()
  }

  function requestDelete(template: RecurringTransaction) {
    setDeleteTarget(template)
    setDeleteError(null)
  }

  async function confirmDelete() {
    if (!deleteTarget) return
    setDeleting(true)
    setDeleteError(null)
    try {
      await deleteRecurringTransaction(deleteTarget.id)
      setDeleteTarget(null)
      await load()
    } catch (err) {
      setDeleteError(resolveErrorMessage(err))
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.toolbar}>
        <button type="button" onClick={openCreateModal} disabled={loading}>
          + 追加
        </button>
      </div>

      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}

      {loading && <p className={styles.loading}>読み込み中...</p>}

      <table className={styles.table}>
        <thead>
          <tr>
            <th>名称</th>
            <th>種別</th>
            <th>カテゴリ</th>
            <th>金額</th>
            <th>実行日</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          {templates.map((t) => (
            <tr key={t.id}>
              <td>{t.name}</td>
              <td>{t.type === 'INCOME' ? '収入' : '支出'}</td>
              <td>{t.category.name}</td>
              <td>{t.amount.toLocaleString()}円</td>
              <td>毎月{t.executionDay}日</td>
              <td className={styles.actionsCell}>
                <button type="button" onClick={() => openEditModal(t)}>
                  編集
                </button>
                <button type="button" onClick={() => requestDelete(t)}>
                  削除
                </button>
              </td>
            </tr>
          ))}
          {!loading && templates.length === 0 && (
            <tr>
              <td colSpan={6} className={styles.empty}>
                定期支出テンプレートがありません
              </td>
            </tr>
          )}
        </tbody>
      </table>

      {modalOpen && (
        <Modal title={editingTemplate ? '定期支出テンプレートの編集' : '定期支出テンプレートの追加'} onClose={closeModal}>
          <RecurringTransactionForm
            initial={editingTemplate}
            onSubmit={handleFormSubmit}
            onCancel={closeModal}
          />
        </Modal>
      )}

      {deleteTarget && (
        <ConfirmDialog
          message={`「${deleteTarget.name}」を削除しますか？削除しても過去に自動生成された収支記録は残ります。`}
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
