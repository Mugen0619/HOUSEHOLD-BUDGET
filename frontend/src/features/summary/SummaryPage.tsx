import { useEffect, useState } from 'react'
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js'
import { Pie } from 'react-chartjs-2'
import type { CategoryAmount, Summary } from '../../types'
import { fetchSummary } from '../../api/summary'
import { resolveErrorMessage } from '../../utils/resolveErrorMessage'
import styles from './SummaryPage.module.css'

ChartJS.register(ArcElement, Tooltip, Legend)

const CHART_COLORS = [
  '#4C6EF5',
  '#FA5252',
  '#12B886',
  '#FAB005',
  '#7950F2',
  '#15AABF',
  '#E64980',
  '#82C91E',
  '#FD7E14',
  '#495057',
]

function pad(n: number): string {
  return String(n).padStart(2, '0')
}

function currentMonthString(): string {
  const now = new Date()
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}`
}

function buildChartData(items: CategoryAmount[]) {
  return {
    labels: items.map((i) => i.name),
    datasets: [
      {
        data: items.map((i) => i.amount),
        backgroundColor: items.map((_, idx) => CHART_COLORS[idx % CHART_COLORS.length]),
      },
    ],
  }
}

function CategoryBreakdown({
  title,
  items,
}: {
  title: string
  items: CategoryAmount[]
}) {
  return (
    <div className={styles.breakdown}>
      <h3>{title}</h3>
      {items.length === 0 ? (
        <p className={styles.empty}>データがありません</p>
      ) : (
        <table className={styles.table}>
          <tbody>
            {items.map((c) => (
              <tr key={c.categoryId}>
                <td>{c.name}</td>
                <td>{c.amount.toLocaleString()}円</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}

export function SummaryPage() {
  const [month, setMonth] = useState(currentMonthString())
  const [summary, setSummary] = useState<Summary | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    fetchSummary(month)
      .then((data) => {
        if (!cancelled) setSummary(data)
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
  }, [month])

  return (
    <div className={styles.page}>
      <label className={styles.monthLabel}>
        対象月
        <input type="month" value={month} onChange={(e) => setMonth(e.target.value)} />
      </label>

      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}
      {loading && <p className={styles.loading}>読み込み中...</p>}

      {summary && (
        <>
          <div className={styles.totals}>
            <span>収入合計: {summary.incomeTotal.toLocaleString()}円</span>
            <span>支出合計: {summary.expenseTotal.toLocaleString()}円</span>
            <span>差引（収支）: {summary.balance.toLocaleString()}円</span>
          </div>

          <div className={styles.charts}>
            <div className={styles.chartBlock}>
              <h3>支出内訳</h3>
              {summary.expenseByCategory.length === 0 ? (
                <p className={styles.empty}>データがありません</p>
              ) : (
                <Pie data={buildChartData(summary.expenseByCategory)} />
              )}
            </div>
            <div className={styles.chartBlock}>
              <h3>収入内訳</h3>
              {summary.incomeByCategory.length === 0 ? (
                <p className={styles.empty}>データがありません</p>
              ) : (
                <Pie data={buildChartData(summary.incomeByCategory)} />
              )}
            </div>
          </div>

          <CategoryBreakdown title="支出カテゴリ別内訳" items={summary.expenseByCategory} />
          <CategoryBreakdown title="収入カテゴリ別内訳" items={summary.incomeByCategory} />
        </>
      )}
    </div>
  )
}
