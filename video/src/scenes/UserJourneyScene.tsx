import React from "react";
import { useCurrentFrame, useVideoConfig, interpolate, spring } from "remotion";

const STEPS = [
  { emoji: "🗳️", zh: "建立投票", en: "Create Poll", color: "#7C3AED", bg: "#EDE9FE", desc: "設定標題、選項\n與截止時間" },
  { emoji: "👥", zh: "選擇對象", en: "Pick Recipients", color: "#0891B2", bg: "#E0F2FE", desc: "從聯絡人選取\n或匯入名單" },
  { emoji: "📱", zh: "發送 SMS", en: "Send SMS", color: "#059669", bg: "#D1FAE5", desc: "批次傳送\n即時追蹤狀態" },
  { emoji: "💬", zh: "收到回覆", en: "Get Replies", color: "#D97706", bg: "#FEF3C7", desc: "受訪者直接回覆\n數字或文字皆可" },
  { emoji: "🤖", zh: "AI 分析", en: "AI Analysis", color: "#DC2626", bg: "#FEE2E2", desc: "自動分類\n產生關鍵洞察" },
  { emoji: "📊", zh: "查看報表", en: "View Report", color: "#7C3AED", bg: "#EDE9FE", desc: "後台儀表板\n一鍵匯出 PDF" },
];

export const UserJourneyScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const headerOpacity = interpolate(frame, [0, 20], [0, 1], { extrapolateRight: "clamp" });
  const headerY = interpolate(frame, [0, 20], [-28, 0], { extrapolateRight: "clamp" });

  return (
    <div style={{
      width: "100%", height: "100%",
      background: "#FFFFFF",
      display: "flex", flexDirection: "column",
      fontFamily: "'Helvetica Neue', Arial, 'PingFang TC', 'Microsoft JhengHei', sans-serif",
    }}>
      {/* Header */}
      <div style={{ padding: "40px 120px 28px", opacity: headerOpacity, transform: `translateY(${headerY}px)`, textAlign: "center" }}>
        <div style={{ fontSize: 26, color: "#7C3AED", fontWeight: 600, letterSpacing: 3, textTransform: "uppercase", marginBottom: 12 }}>
          How It Works · 完整操作流程
        </div>
        <div style={{ fontSize: 72, fontWeight: 900, color: "#1E1B4B", lineHeight: 1.1 }}>
          從投票到報表，
          <span style={{ background: "linear-gradient(135deg, #7C3AED, #06B6D4)", WebkitBackgroundClip: "text", WebkitTextFillColor: "transparent" }}>一條龍完成</span>
        </div>
        <div style={{ fontSize: 28, color: "#6B7280", marginTop: 14 }}>
          6 個步驟完成整個投票流程，從建立到報表一站搞定
        </div>
      </div>

      {/* Timeline */}
      <div style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center", padding: "0 60px", gap: 0 }}>
        {STEPS.map((step, i) => {
          const delay = 22 + i * 16;
          const cardOpacity = interpolate(frame, [delay, delay + 16], [0, 1], { extrapolateRight: "clamp" });
          const cardY = interpolate(frame, [delay, delay + 16], [28, 0], { extrapolateRight: "clamp" });
          const cardScale = spring({ frame, fps, config: { damping: 14, stiffness: 120 }, delay });
          const arrowDelay = delay + 12;
          const arrowOpacity = interpolate(frame, [arrowDelay, arrowDelay + 10], [0, 1], { extrapolateRight: "clamp" });

          return (
            <React.Fragment key={i}>
              <div style={{
                opacity: cardOpacity,
                transform: `translateY(${cardY}px) scale(${cardScale})`,
                display: "flex", flexDirection: "column", alignItems: "center",
                background: "#FFFFFF",
                border: `2px solid ${step.bg}`,
                borderRadius: 24, padding: "24px 18px",
                width: 160, flexShrink: 0,
                boxShadow: "0 4px 24px rgba(0,0,0,0.07)",
              }}>
                <div style={{
                  width: 82, height: 82, borderRadius: "50%",
                  background: step.bg,
                  display: "flex", alignItems: "center", justifyContent: "center",
                  fontSize: 40, marginBottom: 14,
                }}>{step.emoji}</div>
                <div style={{ fontSize: 28, fontWeight: 800, color: "#1E1B4B", textAlign: "center" }}>{step.zh}</div>
                <div style={{ fontSize: 20, color: "#9CA3AF", marginTop: 3, textAlign: "center" }}>{step.en}</div>
                <div style={{ marginTop: 12, fontSize: 20, color: "#6B7280", textAlign: "center", lineHeight: 1.6, whiteSpace: "pre-line" }}>{step.desc}</div>
                <div style={{ marginTop: 12, width: 28, height: 4, borderRadius: 2, background: step.color }} />
              </div>
              {i < STEPS.length - 1 && (
                <div style={{ opacity: arrowOpacity, display: "flex", alignItems: "center", flexShrink: 0, width: 32 }}>
                  <div style={{ flex: 1, height: 2.5, background: "linear-gradient(90deg, #E5E7EB, #CBD5E1)", borderRadius: 2 }} />
                  <div style={{ fontSize: 28, color: "#CBD5E1", marginLeft: -4 }}>›</div>
                </div>
              )}
            </React.Fragment>
          );
        })}
      </div>

      {/* Bottom bar */}
      <div style={{
        height: 6,
        background: "linear-gradient(90deg, #7C3AED, #0891B2, #059669, #D97706, #DC2626, #7C3AED)",
        opacity: interpolate(frame, [fps * 3.2, fps * 4], [0, 1], { extrapolateRight: "clamp" }),
      }} />
    </div>
  );
};
