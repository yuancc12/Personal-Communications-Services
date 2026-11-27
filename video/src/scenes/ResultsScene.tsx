import React from "react";
import { useCurrentFrame, useVideoConfig, interpolate, spring } from "remotion";

const RESULTS = [
  { label: "非常滿意", pct: 42, color: "#059669", emoji: "😊" },
  { label: "滿意", pct: 31, color: "#0891B2", emoji: "🙂" },
  { label: "普通", pct: 18, color: "#D97706", emoji: "😐" },
  { label: "不滿意", pct: 9, color: "#DC2626", emoji: "😞" },
];

const INSIGHTS = [
  { icon: "🎯", text: "即時查看回覆進度", sub: "每筆回覆即時更新，隨時掌握最新投票狀況" },
  { icon: "⚡", text: "AI 自動彙整結果", sub: "分類標籤自動標注，快速找出投票主要趨勢" },
  { icon: "📈", text: "視覺化圖表呈現", sub: "圓餅圖、長條圖一目了然，易於簡報分享" },
];

const DonutChart: React.FC<{ progress: number }> = ({ progress }) => {
  const size = 240;
  const r = 88;
  const cx = size / 2;
  const cy = size / 2;
  const circumference = 2 * Math.PI * r;
  let acc = 0;

  return (
    <div style={{ position: "relative", width: size, height: size }}>
      <svg width={size} height={size}>
        {RESULTS.map((seg, i) => {
          const sp = seg.pct / 100;
          const offset = acc;
          acc += sp;
          const animPct = Math.min(sp, Math.max(0, (progress - offset * 0.9) / (sp + 0.01)));
          const strokeDash = animPct * circumference;
          const strokeOffset = circumference * (1 - offset);
          return (
            <circle key={i} cx={cx} cy={cy} r={r} fill="none" stroke={seg.color} strokeWidth={30}
              strokeDasharray={`${strokeDash} ${circumference}`} strokeDashoffset={strokeOffset}
              style={{ transform: `rotate(-90deg)`, transformOrigin: `${cx}px ${cy}px` }} />
          );
        })}
        <circle cx={cx} cy={cy} r={58} fill="white" />
      </svg>
      <div style={{ position: "absolute", inset: 0, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center" }}>
        <div style={{ fontSize: 38, fontWeight: 900, color: "#1E1B4B" }}>{Math.round(progress * 100)}%</div>
        <div style={{ fontSize: 18, color: "#9CA3AF" }}>集計完成</div>
      </div>
    </div>
  );
};

export const ResultsScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const headerOpacity = interpolate(frame, [0, 20], [0, 1], { extrapolateRight: "clamp" });
  const headerY = interpolate(frame, [0, 20], [-28, 0], { extrapolateRight: "clamp" });
  const donutProgress = interpolate(frame, [22, fps * 2.8], [0, 1], { extrapolateRight: "clamp" });
  const donutOpacity = interpolate(frame, [18, 36], [0, 1], { extrapolateRight: "clamp" });

  return (
    <div style={{
      width: "100%", height: "100%", background: "#F8FAFC",
      display: "flex", flexDirection: "column",
      fontFamily: "'Helvetica Neue', Arial, 'PingFang TC', 'Microsoft JhengHei', sans-serif",
    }}>
      {/* HEADER */}
      <div style={{
        background: "linear-gradient(135deg, #1E1B4B, #312E81)",
        padding: "30px 100px",
        opacity: headerOpacity, transform: `translateY(${headerY}px)`,
        display: "flex", alignItems: "center", gap: 24,
      }}>
        <div>
          <div style={{ fontSize: 22, color: "#A5B4FC", letterSpacing: 3, textTransform: "uppercase", marginBottom: 8 }}>Results · 投票結果</div>
          <div style={{ fontSize: 52, fontWeight: 900, color: "#FFF", lineHeight: 1 }}>即時查看投票結果</div>
          <div style={{ fontSize: 28, color: "rgba(255,255,255,0.65)", marginTop: 8 }}>
            2024 員工滿意度調查 · 189 筆回覆 · 回覆率 78%
          </div>
        </div>
        <div style={{ marginLeft: "auto", textAlign: "center" }}>
          <div style={{ fontSize: 68, fontWeight: 900, color: "#10B981" }}>189</div>
          <div style={{ fontSize: 24, color: "rgba(255,255,255,0.5)" }}>總回覆數</div>
        </div>
      </div>

      {/* MAIN */}
      <div style={{ flex: 1, display: "flex" }}>
        {/* LEFT — Chart */}
        <div style={{ width: "42%", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", padding: "24px 56px", background: "#FFFFFF", borderRight: "1px solid #F3F4F6", opacity: donutOpacity }}>
          <DonutChart progress={donutProgress} />
          <div style={{ display: "flex", flexDirection: "column", gap: 10, marginTop: 20, width: "100%" }}>
            {RESULTS.map((r, i) => {
              const barW = interpolate(frame, [fps * 1.2 + i * 12, fps * 2.2 + i * 12], [0, r.pct], { extrapolateRight: "clamp" });
              const itemOpacity = interpolate(frame, [fps * 1 + i * 10, fps * 1.5 + i * 10], [0, 1], { extrapolateRight: "clamp" });
              return (
                <div key={i} style={{ opacity: itemOpacity }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 5 }}>
                    <span style={{ fontSize: 26 }}>{r.emoji}</span>
                    <span style={{ fontSize: 24, color: "#374151", flex: 1, fontWeight: 500 }}>{r.label}</span>
                    <span style={{ fontSize: 24, fontWeight: 700, color: r.color }}>{Math.round(barW)}%</span>
                  </div>
                  <div style={{ background: "#F3F4F6", borderRadius: 100, height: 10 }}>
                    <div style={{ width: `${barW}%`, height: "100%", background: r.color, borderRadius: 100 }} />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* RIGHT — Insights */}
        <div style={{ flex: 1, display: "flex", flexDirection: "column", justifyContent: "center", padding: "28px 60px", gap: 18 }}>
          <div style={{ fontSize: 32, fontWeight: 700, color: "#1E1B4B", marginBottom: 4, opacity: interpolate(frame, [fps * 1.5, fps * 1.9], [0, 1], { extrapolateRight: "clamp" }) }}>
            關鍵洞察 Key Insights
          </div>
          {INSIGHTS.map((ins, i) => {
            const delay = fps * 1.8 + i * 20;
            const opacity = interpolate(frame, [delay, delay + 18], [0, 1], { extrapolateRight: "clamp" });
            const x = interpolate(frame, [delay, delay + 18], [30, 0], { extrapolateRight: "clamp" });
            const insSpring = spring({ frame, fps, config: { damping: 14 }, delay });
            return (
              <div key={i} style={{
                opacity, transform: `translateX(${x}px) scale(${insSpring})`,
                background: "#FFFFFF", borderRadius: 20, padding: "22px 26px",
                display: "flex", alignItems: "center", gap: 20,
                boxShadow: "0 4px 24px rgba(0,0,0,0.06)",
                border: "1.5px solid #F3F4F6",
              }}>
                <div style={{ width: 72, height: 72, borderRadius: 16, background: "#F8FAFC", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 36, flexShrink: 0 }}>{ins.icon}</div>
                <div>
                  <div style={{ fontSize: 28, fontWeight: 800, color: "#1E1B4B" }}>{ins.text}</div>
                  <div style={{ fontSize: 22, color: "#6B7280", marginTop: 4 }}>{ins.sub}</div>
                </div>
              </div>
            );
          })}
          <div style={{
            background: "linear-gradient(135deg, #7C3AED, #A78BFA)",
            borderRadius: 20, padding: "20px 26px",
            display: "flex", alignItems: "center", gap: 16,
            boxShadow: "0 8px 30px rgba(124,58,237,0.3)",
            opacity: interpolate(frame, [fps * 3.4, fps * 4], [0, 1], { extrapolateRight: "clamp" }),
          }}>
            <span style={{ fontSize: 36 }}>📄</span>
            <div>
              <div style={{ fontSize: 28, fontWeight: 800, color: "#FFF" }}>匯出完整分析報表</div>
              <div style={{ fontSize: 22, color: "rgba(255,255,255,0.7)" }}>一鍵生成 PDF，含圖表摘要與建議，可直接簡報給主管</div>
            </div>
            <div style={{ marginLeft: "auto", background: "rgba(255,255,255,0.2)", borderRadius: 12, padding: "12px 24px", fontSize: 22, fontWeight: 700, color: "#FFF" }}>下載 PDF →</div>
          </div>
        </div>
      </div>
    </div>
  );
};
