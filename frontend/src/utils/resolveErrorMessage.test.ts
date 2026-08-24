import { describe, expect, it } from 'vitest'
import { ApiError } from '../api/client'
import { resolveErrorMessage } from './resolveErrorMessage'

describe('resolveErrorMessage', () => {
  it('returns the generic message for a 5xx ApiError', () => {
    expect(resolveErrorMessage(new ApiError({ status: 500, message: '内部エラー' }))).toBe(
      '予期しないエラーが発生しました',
    )
  })

  it('returns the ApiError message for a 4xx error', () => {
    expect(resolveErrorMessage(new ApiError({ status: 400, message: '入力値が不正です' }))).toBe(
      '入力値が不正です',
    )
  })

  it('falls back to the generic message when a 4xx ApiError has no message', () => {
    expect(resolveErrorMessage(new ApiError({ status: 400, message: '' }))).toBe(
      '予期しないエラーが発生しました',
    )
  })

  it('returns the generic message for a non-ApiError value', () => {
    expect(resolveErrorMessage(new Error('unexpected'))).toBe('予期しないエラーが発生しました')
  })
})
