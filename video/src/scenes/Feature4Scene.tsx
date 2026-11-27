import React from "react";
import { useCurrentFrame, useVideoConfig, interpolate, spring } from "remotion";

const POLLS = [
  { title: "2024 員工滿意度調查", category: "工作滿意度 💼", replies: 47, total: 60, status: "進行中", statusColor: "#059669", statusBg: "#D1FAE5" },
  { title: "新產品功能意見回饋", category: "產品意見 📦", replies: 31, total: 45, status: "進行中", statusColor: "#0891B2", statusBg: "#E0F2FE" },
  { title: "部門年終旅遊票選", category: "活動規劃 🎉", replies: 58, total: 58, status: "已結束", statusColor: "#7C3AED", statusBg: "#EDE9FE" },
  { title: "主管評核問卷", category: "工作滿意度 💼", replies: 23, total: 40, status: "進行中", statusColor: "#D97706", statusBg: "#FEF3C7" },
];

export const Feature4Scene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const headerOpacity = interpolate(frame, [0, 18], [0, 1], { extrapolateRight: "clamp" });
  const headerY = interpolate(frame, [0, 18], [-24, 0], { extrapolateRight: "clamp" });

  const bubble1Opacity = interpolate(frame, [fps * 2.8, fps * 3.1], [0, 1], { extrapolateRight: "clamp" });
  const bubble1Spring = spring({ frame, fps, config: { damping: 14 }, delay: fps * 2.8 });
  const bubble2Opacity = interpolate(frame, [fps * 3.4, fps * 3.7], [0, 1], { extrapolateRight: "clamp" });
  const bubble2Spring = spring({ frame, fps, config: { damping: 14 }, delay: fps * 3.4 });

  return (
    <div style={{
      width: "100%", height: "100%", background: "#F8FAFC",
      display: "flex", flexDirection: "column",
      fontFamily: "'Helvetica Neue', Arial, 'PingFang TC', 'Microsoft JhengHei', sans-serif",
      position: "relative",
    }}>
      {/* TOP HEADER — browser chrome */}
      <div style={{
        height: "22%", background: "#FFFFFF",
        borderBottom: "1px solid #E5E7EB",
        display: "flex", alignItems: "center",
        padding: "0 72px", gap: 24,
        opacity: headerOpacity, transform: `translateY(${headerY}px)`,
        boxShadow: "0 2px 20px rgba(0,0,0,0.04)",
      }}>
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          {["#EF4444", "#F59E0B", "#10B981"].map((c, i) => (
            <div key={i} style={{ width: 14, height: 14, borderRadius: "50%", background: c }} />
          ))}
        </div>
        <div style={{ flex: 1, background: "#F3F4F6", borderRadius: 10, padding: "10px 18px", fontSize: 20, color: "#9CA3AF", display: "flex", alignItems: "center", gap: 8 }}>
          <span>🔒</span><span>localhost:3000</span>
          <span style={{ color: "#D1D5DB" }}>·</span>
          <span style={{ color: "#6B7280" }}>SMSVote Admin Dashboard</span>
        </div>

        <div style={{ display: "flex", gap: 12 }}>
          {[
            { label: "總投票數", value: "24", bg: "#EDE9FE", color: "#7C3AED" },
            { label: "已回覆", value: "159", bg: "#D1FAE5", color: "#059669" },
            { label: "進行中", value: "3", bg: "#FEF3C7", color: "#D97706" },
          ].map((s, i) => {
            const o = interpolate(frame, [8 + i * 8, 24 + i * 8], [0, 1], { extrapolateRight: "clamp" });
            return (
              <div key={i} style={{ opacity: o, background: s.bg, borderRadius: 12, padding: "12px 24px", textAlign: "center" }}>
                <div style={{ fontSize: 32, fontWeight: 900, color: s.color }}>{s.value}</div>
                <div style={{ fontSize: 18, color: s.color + "aa" }}>{s.label}</div>
              </div>
            );
          })}
        </div>
      </div>

      {/* MAIN CONTENT */}
      <div style={{ flex: 1, display: "flex", gap: 20, padding: "20px 72px" }}>

        {/* LEFT — Poll list */}
        <div style={{ flex: 3, display: "flex", flexDirection: "column", gap: 14 }}>
          <div style={{ fontSize: 28, fontWeight: 700, color: "#1E1B4B", marginBottom: 4 }}>
            投票清單
          </div>
          {POLLS.map((poll, i) => {
            const delay = 20 + i * 14;
            const o = interpolate(frame, [delay, delay + 14], [0, 1], { extrapolateRight: "clamp" });
            const x = interpolate(frame, [delay, delay + 14], [-20, 0], { extrapolateRight: "clamp" });
            const pct = poll.replies / poll.total;
            const barW = interpolate(frame, [delay + 10, delay + 40], [0, pct * 100], { extrapolateRight: "clamp" });

            return (
              <div key={i} style={{
                opacity: o, transform: `translateX(${x}px)`,
                background: "#FFFFFF", borderRadius: 18,
                padding: "18px 24px",
                boxShadow: "0 2px 12px rgba(0,0,0,0.05)",
                border: "1.5px solid #F3F4F6",
                display: "flex", alignItems: "center", gap: 20,
              }}>
                {/* Title + category */}
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 28, fontWeight: 700, color: "#1E1B4B", marginBottom: 6 }}>{poll.title}</div>
                  <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                    <span style={{ background: "#F3F4F6", borderRadius: 8, padding: "4px 12px", fontSize: 20, color: "#6B7280" }}>{poll.category}</span>
                    <span style={{ fontSize: 20, color: "#9CA3AF" }}>AI 自動分類</span>
                  </div>
                </div>

                {/* Progress bar */}
                <div style={{ width: 180 }}>
                  <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 6 }}>
                    <span style={{ fontSize: 20, color: "#9CA3AF" }}>回覆進度</span>
                    <span style={{ fontSize: 20, fontWeight: 700, color: "#374151" }}>{poll.replies}/{poll.total}</span>
                  </div>
                  <div style={{ background: "#F3F4F6", borderRadius: 100, height: 8 }}>
                    <div style={{ width: `${barW}%`, height: "100%", background: "linear-gradient(90deg, #7C3AED, #A78BFA)", borderRadius: 100 }} />
                  </div>
                </div>

                {/* Status badge */}
                <div style={{
                  background: poll.statusBg, borderRadius: 100,
                  padding: "10px 22px", fontSize: 22, fontWeight: 600,
                  color: poll.statusColor, whiteSpace: "nowrap",
                }}>{poll.status}</div>
              </div>
            );
          })}
        </div>

        {/* RIGHT — PDF export + info */}
        <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: 16 }}>
          {/* Export card */}
          <div style={{
            background: "linear-gradient(135deg, #7C3AED, #A78BFA)",
            borderRadius: 20, padding: "24px 22px",
            boxShadow: "0 8px 30px rgba(124,58,237,0.3)",
            opacity: interpolate(frame, [fps * 2, fps * 2.4], [0, 1], { extrapolateRight: "clamp" }),
          }}>
            <div style={{ fontSize: 38, marginBottom: 12 }}>📄</div>
            <div style={{ fontSize: 28, fontWeight: 800, color: "#FFF", marginBottom: 6 }}>匯出 PDF 報表</div>
            <div style={{ fontSize: 22, color: "rgba(255,255,255,0.75)", lineHeight: 1.6 }}>
              一鍵生成完整分析報告<br />含圖表、洞察與建議摘要<br />可直接簡報給主管
            </div>
            <div style={{ marginTop: 20, background: "rgba(255,255,255,0.2)", borderRadius: 12, padding: "14px", textAlign: "center", fontSize: 22, fontWeight: 700, color: "#FFF" }}>
              立即下載 ↓
            </div>
          </div>

          {/* AI tag info */}
          <div style={{
            background: "#FFFFFF", borderRadius: 20, padding: "20px 22px",
            boxShadow: "0 2px 12px rgba(0,0,0,0.05)",
            opacity: interpolate(frame, [fps * 2.4, fps * 2.8], [0, 1], { extrapolateRight: "clamp" }),
          }}>
            <div style={{ fontSize: 28, marginBottom: 10 }}>🤖</div>
            <div style={{ fontSize: 24, fontWeight: 700, color: "#1E1B4B", marginBottom: 6 }}>AI 自動標籤</div>
            <div style={{ fontSize: 20, color: "#6B7280", lineHeight: 1.6 }}>
              每筆投票自動分類<br />無需手動整理<br />支援 7 種主題類別
            </div>
          </div>
        </div>
      </div>

      {/* Floating bubbles */}
      <div style={{ position: "absolute", top: "30%", left: "52%", opacity: bubble1Opacity, transform: `scale(${bubble1Spring})`, background: "#7C3AED", color: "#FFF", borderRadius: 16, padding: "14px 26px", fontSize: 22, fontWeight: 600, boxShadow: "0 8px 30px rgba(124,58,237,0.5)", zIndex: 10, display: "flex", alignItems: "center", gap: 8, whiteSpace: "nowrap" }}>
        <span>🤖</span> AI 標籤已自動更新
      </div>
      <div style={{ position: "absolute", bottom: "22%", left: "30%", opacity: bubble2Opacity, transform: `scale(${bubble2Spring})`, background: "#059669", color: "#FFF", borderRadius: 16, padding: "14px 26px", fontSize: 22, fontWeight: 600, boxShadow: "0 8px 30px rgba(5,150,105,0.5)", zIndex: 10, display: "flex", alignItems: "center", gap: 8, whiteSpace: "nowrap" }}>
        <span>✓</span> PDF 報表已生成，共 8 頁
      </div>
    </div>
  );
};
