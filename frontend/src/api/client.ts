import type { ApiErrorBody, ApiFieldError } from '../types'

const BASE_URL = 'http://localhost:8080/api'

/**
 * バックエンドAPIのエラーレスポンス（data-design.md 4章）をラップする例外クラス。
 * 呼び出し側は `err instanceof ApiError` でハンドリングし、
 * `err.errors` からフィールド単位のバリデーションエラーを取り出せる。
 */
export class ApiError extends Error {
  readonly status: number
  readonly errors?: ApiFieldError[]

  constructor(body: ApiErrorBody) {
    super(body.message)
    this.name = 'ApiError'
    this.status = body.status
    this.errors = body.errors
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  let res: Response
  try {
    res = await fetch(`${BASE_URL}${path}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
    })
  } catch {
    // ネットワークエラー（バックエンド未起動等）
    throw new ApiError({ status: 0, message: '予期しないエラーが発生しました' })
  }

  if (!res.ok) {
    let body: ApiErrorBody
    try {
      body = (await res.json()) as ApiErrorBody
    } catch {
      body = { status: res.status, message: '予期しないエラーが発生しました' }
    }
    throw new ApiError(body)
  }

  if (res.status === 204) {
    return undefined as T
  }

  const text = await res.text()
  return (text ? JSON.parse(text) : undefined) as T
}

export const apiClient = {
  get: <T>(path: string): Promise<T> => request<T>(path, { method: 'GET' }),
  post: <T>(path: string, body: unknown): Promise<T> =>
    request<T>(path, { method: 'POST', body: JSON.stringify(body) }),
  put: <T>(path: string, body: unknown): Promise<T> =>
    request<T>(path, { method: 'PUT', body: JSON.stringify(body) }),
  delete: <T>(path: string): Promise<T> => request<T>(path, { method: 'DELETE' }),
}
