import { ApiError } from '../api/client'

/**
 * APIエラーを画面表示用のメッセージに変換する。
 * 500系エラー・想定外の例外は画面設計書5章の方針に従い汎用メッセージにする。
 */
const FALLBACK_MESSAGE = '予期しないエラーが発生しました'

export function resolveErrorMessage(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.status >= 500 || err.status === 0) {
      return FALLBACK_MESSAGE
    }
    return err.message || FALLBACK_MESSAGE
  }
  return FALLBACK_MESSAGE
}
