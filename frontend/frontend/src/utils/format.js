/**
 * 时间展示格式化。
 * - 入参为空返回 ''
 * - 兼容后端 "2024-01-01T10:00:00" 与 "2024-01-01 10:00:00" 两种分隔符
 * - 长度不足 16 时不截断
 */
export function formatTime(t) {
	if (!t) return ''
	const s = String(t).replace('T', ' ')
	return s.length > 16 ? s.substring(0, 16) : s
}
