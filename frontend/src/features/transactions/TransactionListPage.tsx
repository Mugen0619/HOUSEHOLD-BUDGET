import { useEffect, useMemo, useState } from 'react'
import type { Category, CreateTransactionRequest, Transaction, TransactionType } from '../../types'
import {
  createTransaction,
  deleteTransaction,
  fetchTransactions,
  updateTransaction,
} from '../../api/transactions'
import { fetchCategories } from '../../api/categories'
import { resolveErrorMessage } from '../../utils/resolveErrorMessage'
import { Modal } from '../../components/Modal'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { TransactionForm } from './TransactionForm'
import styles from './TransactionListPage.module.css'

type TypeFilter = '' | TransactionType
type SortField = 'date' | 'amount'
type SortOrder = 'asc' | 'desc'

function pad(n: number): string {
  return String(n).padStart(2, '0')
}

function firstDayOfMonth(): string {
  const now = new Date()
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-01`
}

function lastDayOfMonth(): string {
  const now = new Date()
  const last = new Date(now.getFullYear(), now.getMonth() + 1, 0)
  return `${last.getFullYear()}-${pad(last.getMonth() + 1)}-${pad(last.getDate())}`
}

export function TransactionListPage() {
  const [from, setFrom] = useState(firstDayOfMonth())
  const [to, setTo] = useState(lastDayOfMonth())
  const [typeFilter, setTypeFilter] = useState<TypeFilter>('')
  const [categoryFilter, setCategoryFilter] = useState<string>('')
  const [sort, setSort] = useState<SortField>('date')
  const [order, setOrder] = useState<SortOrder>('desc')

  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [modalOpen, setModalOpen] = useState(false)
  const [editingTransaction, setEditingTransaction] = useState<Transaction | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<Transaction | null>(null)
  const [deleting, setDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  useEffect(() => {
    fetchCategories()
      .then(setCategories)
      .catch(() => {
        // カテゴリ取得失敗時はフィルタの選択肢が空になるだけなので致命的ではない
      })
  }, [])

  const listParams = useMemo(
    () => ({
      from,
      to,
      type: typeFilter || undefined,
      categoryId: categoryFilter ? Number(categoryFilter) : undefined,
      sort,
      order,
    }),
    [from, to, typeFilter, categoryFilter, sort, order],
  )

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    fetchTransactions(listParams)
      .then((items) => {
        if (!cancelled) setTransactions(items)
      })
      .catch((err) => {
        if (!cancelled) setError(resolveErrorMessage(err))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [listParams])

  const categoryOptions = useMemo(
    () => (typeFilter ? categories.filter((c) => c.type === typeFilter) : categories),
    [categories, typeFilter],
  )

  function handleTypeFilterChange(next: TypeFilter) {
    setTypeFilter(next)
    setCategoryFilter('')
  }

  function openCreateModal() {
    setEditingTransaction(null)
    setModalOpen(true)
  }

  function openEditModal(tx: Transaction) {
    setEditingTransaction(tx)
    setModalOpen(true)
  }

  function closeModal() {
    setModalOpen(false)
    setEditingTransaction(null)
  }

  async function reload() {
    setLoading(true)
    setError(null)
    try {
      const items = await fetchTransactions(listParams)
      setTransactions(items)
    } catch (err) {
      setError(resolveErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  async function handleFormSubmit(data: CreateTransactionRequest) {
    if (editingTransaction) {
      await updateTransaction(editingTransaction.id, data)
    } else {
      await createTransaction(data)
    }
    closeModal()
    await reload()
  }

  function requestDelete(tx: Transaction) {
    setDeleteTarget(tx)
    setDeleteError(null)
  }

  async function confirmDelete() {
    if (!deleteTarget) return
    setDeleting(true)
    setDeleteError(null)
    try {
      await deleteTransaction(deleteTarget.id)
      setDeleteTarget(null)
      await reload()
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
          + 新規追加
        </button>
      </div>

      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}

      <div className={styles.filters}>
        <label className={styles.filterItem}>
          期間
          <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
          〜
          <input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </label>
        <label className={styles.filterItem}>
          種別
          <select
            value={typeFilter}
            onChange={(e) => handleTypeFilterChange(e.target.value as TypeFilter)}
          >
            <option value="">すべて</option>
            <option value="INCOME">収入</option>
            <option value="EXPENSE">支出</option>
          </select>
        </label>
        <label className={styles.filterItem}>
          カテゴリ
          <select value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)}>
            <option value="">すべて</option>
            {categoryOptions.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </label>
        <label className={styles.filterItem}>
          並び替え
          <select value={sort} onChange={(e) => setSort(e.target.value as SortField)}>
            <option value="date">日付</option>
            <option value="amount">金額</option>
          </select>
          <select value={order} onChange={(e) => setOrder(e.target.value as SortOrder)}>
            <option value="desc">降順</option>
            <option value="asc">昇順</option>
          </select>
        </label>
      </div>

      {loading && <p className={styles.loading}>読み込み中...</p>}

      <table className={styles.table}>
        <thead>
          <tr>
            <th>日付</th>
            <th>種別</th>
            <th>カテゴリ</th>
            <th>金額</th>
            <th>メモ</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          {transactions.map((tx) => (
            <tr key={tx.id}>
              <td>{tx.date}</td>
              <td>{tx.type === 'INCOME' ? '収入' : '支出'}</td>
              <td>{tx.category.name}</td>
              <td>{tx.amount.toLocaleString()}円</td>
              <td>{tx.memo}</td>
              <td className={styles.actionsCell}>
                <button type="button" onClick={() => openEditModal(tx)}>
                  編集
                </button>
                <button type="button" onClick={() => requestDelete(tx)}>
                  削除
                </button>
              </td>
            </tr>
          ))}
          {!loading && transactions.length === 0 && (
            <tr>
              <td colSpan={6} className={styles.empty}>
                データがありません
              </td>
            </tr>
          )}
        </tbody>
      </table>

      {modalOpen && (
        <Modal title={editingTransaction ? '収支記録の編集' : '収支記録の追加'} onClose={closeModal}>
          <TransactionForm
            initial={editingTransaction}
            onSubmit={handleFormSubmit}
            onCancel={closeModal}
          />
        </Modal>
      )}

      {deleteTarget && (
        <ConfirmDialog
          message={`${deleteTarget.date} / ${deleteTarget.category.name} / ${deleteTarget.amount.toLocaleString()}円 の記録を削除しますか？`}
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
