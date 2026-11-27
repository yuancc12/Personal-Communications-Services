import React from "react";
import { useCurrentFrame, useVideoConfig, interpolate, spring } from "remotion";

const BAR_DATA = [
  { label: "週一", v: 62 }, { label: "週二", v: 85 }, { label: "週三", v: 48 },
  { label: "週四", v: 93 }, { label: "週五", v: 71 }, { label: "週六", v: 55 }, { label: "週日", v: 80 },
];

export const Feature4Scene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const barProgress = interpolate(frame, [20, fps * 2.5], [0, 1], { extrapolateRight: "clamp" });
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
      {/* HEADER */}
      <div style={{
        height: "24%", background: "#FFFFFF",
        borderBottom: "1px solid #E5E7EB",
        display: "flex", alignItems: "center",
        padding: "0 72px", gap: 36,
        opacity: headerOpacity, transform: `translateY(${headerY}px)`,
        boxShadow: "0 2px 20px rgba(0,0,0,0.04)",
      }}>
        <div style={{ display: "flex", alignItems: "center", gap: 8, marginRight: 8 }}>
          {["#EF4444", "#F59E0B", "#10B981"].map((c, i) => <div key={i} style={{ width: 14, height: 14, borderRadius: "50%", background: c }} />)}
        </div>
        <div style={{ flex: 1, background: "#F3F4F6", borderRadius: 10, padding: "10px 18px", fontSize: 15, color: "#9CA3AF", display: "flex", alignItems: "center", gap: 8 }}>
          <span>🔒</span><span>localhost:3000</span><span style={{ color: "#D1D5DB" }}>·</span><span>SMSVote Admin Dashboard</span>
        </div>
        <div style={{ display: "flex", gap: 12 }}>
          {[
            { label: "總投票數", value: "24", bg: "#EDE9FE", color: "#7C3AED" },
            { label: "已回覆", value: "189", bg: "#D1FAE5", color: "#059669" },
            { label: "平均回覆率", value: "78%", bg: "#FEF3C7", color: "#D97706" },
          ].map((s, i) => {
            const o = interpolate(frame, [8 + i * 8, 24 + i * 8], [0, 1], { extrapolateRight: "clamp" });
            return (
              <div key={i} style={{ opacity: o, background: s.bg, borderRadius: 12, padding: "10px 20px", textAlign: "center" }}>
                <div style={{ fontSize: 26, fontWeight: 900, color: s.color }}>{s.value}</div>
                <div style={{ fontSize: 12, color: s.color + "aa" }}>{s.label}</div>
              </div>
            );
          })}
        </div>
      </div>

      {/* MAIN */}
      <div style={{ flex: 1, display: "flex", gap: 20, padding: "20px 72px" }}>
        {/* Chart */}
        <div style={{ flex: 2, background: "#FFFFFF", borderRadius: 20, padding: "24px 28px", boxShadow: "0 2px 20px rgba(0,0,0,0.05)" }}>
          <div style={{ fontSize: 20, fontWeight: 700, color: "#1E1B4B", marginBottom: 6 }}>每週回覆趨勢</div>
          <div style={{ fontSize: 15, color: "#9CA3AF", marginBottom: 18 }}>過去 7 天的投票回覆數量統計</div>
          <div style={{ display: "flex", alignItems: "flex-end", gap: 12, height: 150 }}>
            {BAR_DATA.map((d, i) => (
              <div key={i} style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", gap: 6 }}>
                <div style={{
                  width: "100%", height: `${d.v * barProgress}%`,
                  background: i === 3 ? "linear-gradient(180deg, #7C3AED, #A78BFA)" : "linear-gradient(180deg, #E5E7EB, #F3F4F6)",
                  borderRadius: "6px 6px 0 0", minHeight: 2,
                  boxShadow: i === 3 ? "0 4px 16px rgba(124,58,237,0.4)" : "none",
                }} />
                <div style={{ fontSize: 13, color: "#9CA3AF" }}>{d.label}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Right column */}
        <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: 16 }}>
          <div style={{ flex: 1, background: "#FFFFFF", borderRadius: 20, padding: "20px 22px", boxShadow: "0 2px 20px rgba(0,0,0,0.05)" }}>
            <div style={{ fontSize: 18, fontWeight: 700, color: "#1E1B4B", marginBottom: 14 }}>最近投票</div>
            {[
              { title: "員工滿意度", badge: "工作", color: "#7C3AED", replies: 23, rate: "79%" },
              { title: "產品意見", badge: "產品", color: "#0891B2", replies: 18, rate: "62%" },
              { title: "部門旅遊", badge: "活動", color: "#059669", replies: 31, rate: "89%" },
            ].map((p, i) => {
              const o = interpolate(frame, [15 + i * 10, 30 + i * 10], [0, 1], { extrapolateRight: "clamp" });
              return (
                <div key={i} style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 12, opacity: o }}>
                  <div style={{ background: p.color + "15", borderRadius: 8, padding: "4px 10px", fontSize: 13, fontWeight: 600, color: p.color, flexShrink: 0 }}>{p.badge}</div>
                  <div style={{ flex: 1, fontSize: 16, color: "#374151", fontWeight: 500 }}>{p.title}</div>
                  <div style={{ textAlign: "right" }}>
                    <div style={{ fontSize: 15, fontWeight: 700, color: p.color }}>{p.rate}</div>
                    <div style={{ fontSize: 12, color: "#9CA3AF" }}>{p.replies} 回覆</div>
                  </div>
                </div>
              );
            })}
          </div>
          <div style={{
            background: "linear-gradient(135deg, #059669, #10B981)",
            borderRadius: 20, padding: "20px 22px",
            boxShadow: "0 8px 24px rgba(5,150,105,0.3)",
            opacity: interpolate(frame, [fps * 2, fps * 2.4], [0, 1], { extrapolateRight: "clamp" }),
          }}>
            <div style={{ fontSize: 26, marginBottom: 8 }}>📄</div>
            <div style={{ fontSize: 20, fontWeight: 800, color: "#FFF" }}>匯出 PDF 報表</div>
            <div style={{ fontSize: 15, color: "rgba(255,255,255,0.75)", marginTop: 4 }}>一鍵生成完整分析報告，含圖表與洞察摘要</div>
            <div style={{ marginTop: 14, background: "rgba(255,255,255,0.2)", borderRadius: 10, padding: "10px", textAlign: "center", fontSize: 16, fontWeight: 700, color: "#FFF" }}>立即下載 ↓</div>
          </div>
        </div>
      </div>

      {/* Floating bubbles */}
      <div style={{ position: "absolute", top: "30%", left: "57%", opacity: bubble1Opacity, transform: `scale(${bubble1Spring})`, background: "#7C3AED", color: "#FFF", borderRadius: 16, padding: "12px 22px", fontSize: 16, fontWeight: 600, boxShadow: "0 8px 30px rgba(124,58,237,0.5)", zIndex: 10, display: "flex", alignItems: "center", gap: 8, whiteSpace: "nowrap" }}>
        <span>🤖</span> AI 標籤已自動更新
      </div>
      <div style={{ position: "absolute", bottom: "26%", left: "36%", opacity: bubble2Opacity, transform: `scale(${bubble2Spring})`, background: "#059669", color: "#FFF", borderRadius: 16, padding: "12px 22px", fontSize: 16, fontWeight: 600, boxShadow: "0 8px 30px rgba(5,150,105,0.5)", zIndex: 10, display: "flex", alignItems: "center", gap: 8, whiteSpace: "nowrap" }}>
        <span>✓</span> PDF 報表已生成，共 8 頁
      </div>
    </div>
  );
};
