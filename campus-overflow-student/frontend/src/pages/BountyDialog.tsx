import { useEffect, useState, type FormEvent } from 'react';

/** 发起悬赏对话框：积分 10–500、时长 1–14 天，提交后由父组件调用后端接口。 */
export function BountyDialog({
  onClose,
  onSubmit,
}: {
  onClose: () => void;
  onSubmit: (points: number, days: number) => Promise<void>;
}) {
  const [points, setPoints] = useState(50);
  const [days, setDays] = useState(7);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await onSubmit(points, days);
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : '发起悬赏失败');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-backdrop" role="presentation" onClick={onClose}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-label="发起悬赏"
        onClick={(e) => e.stopPropagation()}
      >
        <h3>发起悬赏</h3>
        <p className="muted">悬赏积分将从你的账户冻结，采纳结算后转给获奖者；到期未采纳将原路退回。</p>
        <form onSubmit={submit}>
          <label>
            悬赏积分（10–500）
            <input
              type="number"
              min={10}
              max={500}
              value={points}
              onChange={(e) => setPoints(Number(e.target.value))}
            />
          </label>
          <label>
            悬赏时长（1–14 天）
            <input
              type="number"
              min={1}
              max={14}
              value={days}
              onChange={(e) => setDays(Number(e.target.value))}
            />
          </label>
          {error && <p className="error">{error}</p>}
          <div className="modal-actions">
            <button type="button" onClick={onClose}>
              取消
            </button>
            <button type="submit" className="primary" disabled={submitting}>
              {submitting ? '提交中…' : '确认发起'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

/** 悬赏倒计时：OPEN 显示剩余时间与积分；AWARDED/EXPIRED 显示终态。 */
export function BountyBadge({
  bounty,
}: {
  bounty: {
    points: number;
    status: 'OPEN' | 'AWARDED' | 'EXPIRED';
    expiresAt: string;
    winnerId?: number;
  };
}) {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 60_000);
    return () => clearInterval(timer);
  }, []);

  if (bounty.status === 'AWARDED') {
    return (
      <span className="bounty awarded" title="悬赏已发放">
        悬赏 {bounty.points} 分已发放
      </span>
    );
  }
  if (bounty.status === 'EXPIRED') {
    return (
      <span className="bounty expired" title="悬赏已到期">
        悬赏 {bounty.points} 分已到期
      </span>
    );
  }
  const left = new Date(bounty.expiresAt).getTime() - now.getTime();
  const hours = Math.max(0, Math.floor(left / 3_600_000));
  const minutes = Math.max(0, Math.floor((left % 3_600_000) / 60_000));
  return (
    <span
      className="bounty open"
      title={`悬赏 ${bounty.points} 积分，${new Date(bounty.expiresAt).toLocaleString('zh-CN')} 到期`}
    >
      悬赏 {bounty.points} 分 · 剩余 {hours} 小时 {minutes} 分
    </span>
  );
}
