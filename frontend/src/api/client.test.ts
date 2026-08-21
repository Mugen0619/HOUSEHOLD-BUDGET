import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiClient } from './client'
import type { ApiErrorBody } from '../types'

function jsonResponse(status: number, body: unknown): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
    text: () => Promise.resolve(JSON.stringify(body)),
  } as Response
}

describe('apiClient', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('returns parsed JSON for a 2xx response', async () => {
    const data = { id: 1, name: '食費', type: 'EXPENSE' }
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(200, data))
    vi.stubGlobal('fetch', fetchMock)

    const result = await apiClient.get<typeof data>('/categories/1')

    expect(result).toEqual(data)
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/categories/1',
      expect.objectContaining({ method: 'GET' }),
    )
  })

  it('throws an ApiError with the parsed {status, message, errors} body for a non-2xx response', async () => {
    const errorBody: ApiErrorBody = {
      status: 409,
      message: 'このカテゴリは使用中のため削除できません',
      errors: [{ field: 'categoryId', message: '使用中のカテゴリです' }],
    }
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(409, errorBody))
    vi.stubGlobal('fetch', fetchMock)

    await expect(apiClient.delete('/categories/1')).rejects.toMatchObject({
      status: 409,
      message: 'このカテゴリは使用中のため削除できません',
      errors: errorBody.errors,
    })
  })

  it('throws an ApiError instance so callers can read .status and .errors', async () => {
    const errorBody: ApiErrorBody = {
      status: 400,
      message: '入力内容に誤りがあります',
      errors: [{ field: 'amount', message: '金額は1以上の整数で入力してください' }],
    }
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(400, errorBody))
    vi.stubGlobal('fetch', fetchMock)

    try {
      await apiClient.post('/transactions', {})
      expect.unreachable('expected apiClient.post to throw')
    } catch (err) {
      expect(err).toBeInstanceOf(ApiError)
      expect(err).toBeInstanceOf(Error)
      const apiErr = err as ApiError
      expect(apiErr.status).toBe(400)
      expect(apiErr.errors).toEqual(errorBody.errors)
    }
  })

  it('throws an ApiError with status 0 when the network request itself fails', async () => {
    const fetchMock = vi.fn().mockRejectedValue(new TypeError('Failed to fetch'))
    vi.stubGlobal('fetch', fetchMock)

    await expect(apiClient.get('/transactions')).rejects.toMatchObject({
      status: 0,
      message: '予期しないエラーが発生しました',
    })
  })
})
