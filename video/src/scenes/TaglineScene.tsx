import React from "react";
import { useCurrentFrame, useVideoConfig, interpolate, spring } from "remotion";

const CARDS = [
  { emoji: "🗳️", title: "建立投票", sub: "Create Polls", desc: "自訂選項、截止日期\n多種題型支援", color: "#7C3AED", bg: "#EDE9FE" },
  { emoji: "📱", title: "SMS 發送", sub: "Bulk SMS Delivery", desc: "批次傳送聯絡人\n即時追蹤送達狀態", color: "#0891B2", bg: "#E0F2FE" },
  { emoji: "🤖", title: "AI 分析", sub: "Smart Categorization", desc: "自動分類主題\n產生洞察與報表", color: "#059669", bg: "#D1FAE5" },
];

export const TaglineScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const topOpacity = interpolate(frame, [0, 20], [0, 1], { extrapolateRight: "clamp" });
  const topY = interpolate(frame, [0, 20], [-30, 0], { extrapolateRight: "clamp" });
  const dividerW = interpolate(frame, [22, 48], [0, 1920], { extrapolateRight: "clamp" });

  return (
    <div style={{
      width: "100%", height: "100%",
      background: "#FFFFFF",
      display: "flex", flexDirection: "column",
      fontFamily: "'Helvetica Neue', Arial, 'PingFang TC', 'Microsoft JhengHei', sans-serif",
    }}>
      {/* TOP 42% */}
      <div style={{
        height: "42%",
        background: "linear-gradient(135deg, #1E1B4B 0%, #312E81 100%)",
        display: "flex", flexDirection: "column",
        alignItems: "center", justifyContent: "center",
        padding: "0 160px",
        opacity: topOpacity, transform: `translateY(${topY}px)`,
      }}>
        <div style={{ fontSize: 22, color: "#A5B4FC", letterSpacing: 4, textTransform: "uppercase", marginBottom: 18 }}>
          The Problem We Solve
        </div>
        <div style={{ fontSize: 72, fontWeight: 900, color: "#FFFFFF", lineHeight: 1.15, textAlign: "center" }}>
          傳統投票<span style={{ color: "#A78BFA" }}> 費時費力</span>，<br />
          SMS 直達每一個人
        </div>
        <div style={{ fontSize: 26, color: "rgba(255,255,255,0.6)", marginTop: 18, textAlign: "center" }}>
          受訪者不需下載任何 App，直接用手機簡訊回覆即可
        </div>
      </div>

      {/* Divider */}
      <div style={{ height: 5, background: "linear-gradient(90deg, #7C3AED, #06B6D4, #10B981)", width: dividerW }} />

      {/* BOTTOM 58% — 3 cards */}
      <div style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center", gap: 44, padding: "0 100px", background: "#F8FAFC" }}>
        {CARDS.map((card, i) => {
          const delay = 32 + i * 18;
          const cardOpacity = interpolate(frame, [delay, delay + 18], [0, 1], { extrapolateRight: "clamp" });
          const cardY = interpolate(frame, [delay, delay + 18], [40, 0], { extrapolateRight: "clamp" });
          const cardScale = spring({ frame, fps, config: { damping: 14, stiffness: 120 }, delay });

          return (
            <div key={i} style={{
              opacity: cardOpacity,
              transform: `translateY(${cardY}px) scale(${cardScale})`,
              flex: 1, background: "#FFFFFF",
              borderRadius: 28, padding: "36px 32px",
              textAlign: "center",
              boxShadow: "0 8px 40px rgba(0,0,0,0.08)",
              border: `2px solid ${card.bg}`,
            }}>
              <div style={{
                width: 100, height: 100, borderRadius: "50%",
                background: card.bg,
                display: "flex", alignItems: "center", justifyContent: "center",
                fontSize: 52, margin: "0 auto 20px",
              }}>{card.emoji}</div>
              <div style={{ fontSize: 40, fontWeight: 800, color: "#1E1B4B", marginBottom: 8 }}>{card.title}</div>
              <div style={{ fontSize: 22, color: "#6B7280", marginBottom: 14 }}>{card.sub}</div>
              <div style={{ fontSize: 20, color: "#9CA3AF", lineHeight: 1.6, whiteSpace: "pre-line" }}>{card.desc}</div>
              <div style={{
                marginTop: 20, display: "inline-block",
                padding: "10px 26px", borderRadius: 100,
                background: card.bg, fontSize: 18,
                color: card.color, fontWeight: 600,
              }}>了解更多 →</div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
