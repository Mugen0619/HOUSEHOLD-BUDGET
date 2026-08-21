import { ApiError } from '../api/client'

/**
 * APIエラーを画面表示用のメッセージに変換する。
 * 500系エラー・想定外の例外は画面設計書5章の方針に従い汎用メッセージにする。
 */
export function resolveErrorMessage(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.status >= 500 || err.status === 0) {
      return '予期しないエラーが発生しました'
    }
    return err.message
  }
  return '予期しないエラーが発生しました'
}
