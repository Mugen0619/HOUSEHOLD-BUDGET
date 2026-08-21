import { useState } from 'react'
import { TransactionListPage } from './features/transactions/TransactionListPage'
import { CategoryManagementPage } from './features/categories/CategoryManagementPage'
import { SummaryPage } from './features/summary/SummaryPage'
import styles from './App.module.css'

type Tab = 'transactions' | 'categories' | 'summary'

const TABS: { key: Tab; label: string }[] = [
  { key: 'transactions', label: '一覧' },
  { key: 'categories', label: 'カテゴリ管理' },
  { key: 'summary', label: '月次集計' },
]

function App() {
  const [tab, setTab] = useState<Tab>('transactions')

  return (
    <div className={styles.app}>
      <header className={styles.header}>
        <h1 className={styles.title}>家計簿アプリ</h1>
        <nav className={styles.tabs}>
          {TABS.map((t) => (
            <button
              key={t.key}
              type="button"
              className={tab === t.key ? styles.activeTab : styles.tab}
              onClick={() => setTab(t.key)}
            >
              {t.label}
            </button>
          ))}
        </nav>
      </header>
      <main className={styles.main}>
        {tab === 'transactions' && <TransactionListPage />}
        {tab === 'categories' && <CategoryManagementPage />}
        {tab === 'summary' && <SummaryPage />}
      </main>
    </div>
  )
}

export default App
