import React from "react";
import { useCurrentFrame, useVideoConfig, interpolate, spring } from "remotion";

const POLLS = [
  { title: "2024員工旅遊票選", votes: "47 票", before: "未分類", after: "活動規劃 🎉", color: "#F59E0B" },
  { title: "新產品功能意見回饋", votes: "31 票", before: "未分類", after: "產品意見 📦", color: "#7C3AED" },
  { title: "主管評核問卷調查", votes: "58 票", before: "未分類", after: "工作滿意度 💼", color: "#0891B2" },
];

export const Feature3Scene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const headerOpacity = interpolate(frame, [0, 20], [0, 1], { extrapolateRight: "clamp" });
  const headerY = interpolate(frame, [0, 20], [-28, 0], { extrapolateRight: "clamp" });
  const showResult = interpolate(frame, [30, fps * 2.8], [0, 1], { extrapolateRight: "clamp" }) > 0.9;
  const statsOpacity = interpolate(frame, [fps * 3.4, fps * 4.2], [0, 1], { extrapolateRight: "clamp" });

  return (
    <div style={{
      width: "100%", height: "100%", background: "#FFFFFF",
      display: "flex", flexDirection: "column",
      fontFamily: "'Helvetica Neue', Arial, 'PingFang TC', 'Microsoft JhengHei', sans-serif",
    }}>
      {/* HEADER */}
      <div style={{
        padding: "32px 100px 24px",
        borderBottom: "1px solid #F3F4F6",
        display: "flex", alignItems: "center", gap: 20,
        opacity: headerOpacity, transform: `translateY(${headerY}px)`,
      }}>
        <div style={{ width: 76, height: 76, borderRadius: 18, background: "linear-gradient(135deg, #EDE9FE, #DDD6FE)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 38 }}>🤖</div>
        <div>
          <div style={{ fontSize: 22, color: "#7C3AED", fontWeight: 600, letterSpacing: 1, textTransform: "uppercase" }}>功能 03 · AI Categorization</div>
          <div style={{ fontSize: 48, fontWeight: 900, color: "#1E1B4B" }}>AI 智慧分類引擎</div>
          <div style={{ fontSize: 28, color: "#6B7280", marginTop: 4 }}>
            使用 <strong style={{ color: "#7C3AED" }}>Jev TypeSafe AI</strong> 自動辨識投票主題，零幻覺、高準確率，歷史投票也能補標籤
          </div>
        </div>
        <div style={{
          marginLeft: "auto",
          background: showResult ? "#D1FAE5" : "#FEF3C7",
          border: `1.5px solid ${showResult ? "#10B981" : "#F59E0B"}`,
          borderRadius: 100, padding: "10px 24px",
          fontSize: 22, fontWeight: 600,
          color: showResult ? "#059669" : "#D97706",
          display: "flex", alignItems: "center", gap: 8,
          whiteSpace: "nowrap",
        }}>
          <div style={{ width: 10, height: 10, borderRadius: "50%", background: showResult ? "#10B981" : "#F59E0B" }} />
          {showResult ? "分類完成" : "分析中..."}
        </div>
      </div>

      {/* POLL CARDS */}
      <div style={{ flex: 1, padding: "24px 100px", display: "flex", flexDirection: "column", gap: 16 }}>
        {POLLS.map((poll, i) => {
          const cardDelay = 22 + i * 14;
          const cardOpacity = interpolate(frame, [cardDelay, cardDelay + 14], [0, 1], { extrapolateRight: "clamp" });
          const pollProgress = interpolate(frame, [fps * 0.8 + i * 14, fps * 1.5 + i * 14], [0, 1], { extrapolateRight: "clamp" });
          const hasResult = pollProgress > 0.85;

          return (
            <div key={i} style={{
              opacity: cardOpacity,
              background: "#FAFAFA",
              border: `1.5px solid ${hasResult ? poll.color + "44" : "#E5E7EB"}`,
              borderRadius: 20, padding: "22px 28px",
              display: "flex", alignItems: "center", gap: 28,
              boxShadow: hasResult ? `0 4px 20px ${poll.color}18` : "0 2px 8px rgba(0,0,0,0.04)",
            }}>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 32, fontWeight: 700, color: "#1E1B4B" }}>{poll.title}</div>
                <div style={{ fontSize: 22, color: "#9CA3AF", marginTop: 4 }}>2024 · {poll.votes}</div>
              </div>
              <div style={{ fontSize: 32, color: "#D1D5DB" }}>→</div>
              <div style={{ width: 220 }}>
                <div style={{ height: 8, background: "#F3F4F6", borderRadius: 100, marginBottom: 8 }}>
                  <div style={{ width: `${pollProgress * 100}%`, height: "100%", background: `linear-gradient(90deg, #7C3AED, ${poll.color})`, borderRadius: 100 }} />
                </div>
                <div style={{ fontSize: 20, color: "#9CA3AF" }}>{hasResult ? "分析完成 ✓" : `分析中 ${Math.round(pollProgress * 100)}%`}</div>
              </div>
              <div style={{ fontSize: 32, color: "#D1D5DB" }}>→</div>
              <div style={{
                minWidth: 180, textAlign: "center",
                background: hasResult ? poll.color + "15" : "#F3F4F6",
                border: `1.5px solid ${hasResult ? poll.color + "44" : "transparent"}`,
                borderRadius: 14, padding: "14px 24px",
                fontSize: 24, fontWeight: 700,
                color: hasResult ? poll.color : "#D1D5DB",
              }}>{hasResult ? poll.after : "等待分類..."}</div>
            </div>
          );
        })}
      </div>

      {/* STATS BAR */}
      <div style={{
        height: "19%",
        background: "linear-gradient(135deg, #1E1B4B, #312E81)",
        display: "flex", alignItems: "center", justifyContent: "center",
        gap: 100, opacity: statsOpacity,
      }}>
        {[
          { value: "98%", label: "分類準確率", icon: "🎯" },
          { value: "< 2s", label: "平均分析速度", icon: "⚡" },
          { value: "7", label: "支援分類主題數", icon: "🏷️" },
          { value: "100%", label: "歷史資料補標籤", icon: "🔄" },
        ].map((s, i) => (
          <div key={i} style={{ textAlign: "center" }}>
            <div style={{ fontSize: 34, marginBottom: 6 }}>{s.icon}</div>
            <div style={{ fontSize: 48, fontWeight: 900, color: "#FFF" }}>{s.value}</div>
            <div style={{ fontSize: 24, color: "rgba(255,255,255,0.6)" }}>{s.label}</div>
          </div>
        ))}
      </div>
    </div>
  );
};
