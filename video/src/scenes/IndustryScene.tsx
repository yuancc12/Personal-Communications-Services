import React from "react";
import { useCurrentFrame, useVideoConfig, interpolate, spring } from "remotion";

// Scene: 產業定位 — 上半痛點，下半 XY 定位圖
const PAIN_POINTS = [
  { icon: "📋", title: "紙本問卷", problem: "回收耗時、人工整理、易遺失", color: "#DC2626", bg: "#FEE2E2" },
  { icon: "✉️", title: "Email 問卷", problem: "開信率低、需要帳號登入、回覆率差", color: "#D97706", bg: "#FEF3C7" },
  { icon: "📲", title: "App 投票", problem: "需下載安裝、學習成本高、覆蓋率有限", color: "#0891B2", bg: "#E0F2FE" },
];

// Competitor positions: [x%, y%] on the chart (0,0 = bottom-left; 100,100 = top-right)
// X = 易用性（低→高），Y = 智慧化程度（低→高）
const COMPETITORS = [
  { name: "紙本問卷",    x: 12, y: 8,  color: "#9CA3AF", r: 14 },
  { name: "Google Forms", x: 68, y: 28, color: "#4285F4", r: 16 },
  { name: "SurveyMonkey", x: 55, y: 45, color: "#00BF6F", r: 15 },
  { name: "LINE 投票",   x: 72, y: 22, color: "#06C755", r: 14 },
  { name: "SMSVote",     x: 85, y: 85, color: "#7C3AED", r: 22, highlight: true },
];

export const IndustryScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const headerO = interpolate(frame, [0, 18], [0, 1], { extrapolateRight: "clamp" });
  const headerY = interpolate(frame, [0, 18], [-24, 0], { extrapolateRight: "clamp" });

  // Pain point cards stagger
  const getPainO = (i: number) => interpolate(frame, [18 + i * 14, 36 + i * 14], [0, 1], { extrapolateRight: "clamp" });
  const getPainX = (i: number) => interpolate(frame, [18 + i * 14, 36 + i * 14], [-30, 0], { extrapolateRight: "clamp" });

  // Chart axes draw
  const axisProgress = interpolate(frame, [fps * 1.4, fps * 2.2], [0, 1], { extrapolateRight: "clamp" });

  // Competitors appear
  const getCompO = (i: number) => {
    const delay = fps * 2.2 + i * 14;
    return interpolate(frame, [delay, delay + 16], [0, 1], { extrapolateRight: "clamp" });
  };
  const getCompScale = (i: number) => spring({ frame, fps, config: { damping: 14, stiffness: 120 }, delay: fps * 2.2 + i * 14 });

  // SMSVote label pop
  const smsLabelO = interpolate(frame, [fps * 3.4, fps * 3.8], [0, 1], { extrapolateRight: "clamp" });
  const smsLabelScale = spring({ frame, fps, config: { damping: 10, stiffness: 80 }, delay: fps * 3.4 });

  const CHART_W = 540;
  const CHART_H = 340;

  return (
    <div style={{
      width: "100%", height: "100%",
      background: "#FFFFFF",
      display: "flex", flexDirection: "column",
      fontFamily: "'Helvetica Neue', Arial, 'PingFang TC', 'Microsoft JhengHei', sans-serif",
    }}>
      {/* HEADER */}
      <div style={{
        padding: "32px 100px 24px",
        borderBottom: "1px solid #F3F4F6",
        opacity: headerO, transform: `translateY(${headerY}px)`,
      }}>
        <div style={{ fontSize: 16, color: "#7C3AED", fontWeight: 600, letterSpacing: 2, textTransform: "uppercase", marginBottom: 8 }}>
          Industry · 產業分析
        </div>
        <div style={{ fontSize: 52, fontWeight: 900, color: "#1E1B4B", lineHeight: 1 }}>
          傳統投票工具的<span style={{ background: "linear-gradient(135deg, #DC2626, #F59E0B)", WebkitBackgroundClip: "text", WebkitTextFillColor: "transparent" }}>三大痛點</span>，我們全部解決
        </div>
      </div>

      {/* MAIN: 左邊痛點 ＋ 右邊定位圖 */}
      <div style={{ flex: 1, display: "flex", padding: "24px 100px", gap: 60, alignItems: "center" }}>

        {/* LEFT — Pain points + our solution */}
        <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: 16 }}>
          {PAIN_POINTS.map((p, i) => (
            <div key={i} style={{
              opacity: getPainO(i), transform: `translateX(${getPainX(i)}px)`,
              background: "#FAFAFA", border: `1.5px solid ${p.bg}`,
              borderRadius: 18, padding: "18px 22px",
              display: "flex", alignItems: "center", gap: 18,
            }}>
              <div style={{ width: 54, height: 54, borderRadius: 14, background: p.bg, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 26, flexShrink: 0 }}>
                {p.icon}
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 20, fontWeight: 700, color: "#1E1B4B" }}>{p.title}</div>
                <div style={{ fontSize: 16, color: p.color, marginTop: 3 }}>✗ {p.problem}</div>
              </div>
            </div>
          ))}

          {/* SMSVote solution card */}
          <div style={{
            opacity: interpolate(frame, [fps * 1, fps * 1.4], [0, 1], { extrapolateRight: "clamp" }),
            transform: `translateX(${interpolate(frame, [fps * 1, fps * 1.4], [-30, 0], { extrapolateRight: "clamp" })}px)`,
            background: "linear-gradient(135deg, #EDE9FE, #DDD6FE)",
            border: "2px solid #7C3AED44",
            borderRadius: 18, padding: "18px 22px",
            display: "flex", alignItems: "center", gap: 18,
          }}>
            <div style={{ width: 54, height: 54, borderRadius: 14, background: "#7C3AED", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 26, flexShrink: 0 }}>
              💬
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 20, fontWeight: 700, color: "#7C3AED" }}>SMSVote</div>
              <div style={{ fontSize: 16, color: "#059669", marginTop: 3 }}>✓ 免 App、免帳號、AI 自動分析，全部搞定</div>
            </div>
          </div>
        </div>

        {/* RIGHT — XY Positioning Chart */}
        <div style={{
          flexShrink: 0,
          opacity: interpolate(frame, [fps * 1.3, fps * 1.7], [0, 1], { extrapolateRight: "clamp" }),
        }}>
          <div style={{ fontSize: 15, fontWeight: 600, color: "#6B7280", textAlign: "center", marginBottom: 12 }}>
            競品定位圖
          </div>

          <div style={{ position: "relative", width: CHART_W, height: CHART_H }}>
            {/* Chart background */}
            <div style={{
              position: "absolute", inset: 0,
              background: "#F8FAFC", borderRadius: 20,
              border: "1.5px solid #E5E7EB",
            }} />

            {/* Quadrant highlight for SMSVote area */}
            <div style={{
              position: "absolute",
              right: 0, top: 0,
              width: "40%", height: "50%",
              background: "rgba(124,58,237,0.05)",
              borderRadius: "0 20px 0 0",
              border: "1px dashed rgba(124,58,237,0.2)",
            }} />

            {/* Y axis line */}
            <div style={{
              position: "absolute",
              left: 60, bottom: 40,
              width: 2,
              height: `${axisProgress * (CHART_H - 60)}px`,
              background: "#E5E7EB",
            }} />
            {/* X axis line */}
            <div style={{
              position: "absolute",
              left: 60, bottom: 40,
              height: 2,
              width: `${axisProgress * (CHART_W - 80)}px`,
              background: "#E5E7EB",
            }} />

            {/* Y axis label */}
            <div style={{
              position: "absolute", left: 4, top: "50%",
              transform: "rotate(-90deg) translateX(50%)",
              fontSize: 13, color: "#9CA3AF", fontWeight: 600,
              whiteSpace: "nowrap",
            }}>
              智慧化程度 ↑
            </div>
            {/* X axis label */}
            <div style={{
              position: "absolute", bottom: 10, right: 20,
              fontSize: 13, color: "#9CA3AF", fontWeight: 600,
            }}>
              易用性 →
            </div>

            {/* Competitor dots */}
            {COMPETITORS.map((c, i) => {
              const dotX = 60 + (c.x / 100) * (CHART_W - 80);
              const dotY = CHART_H - 40 - (c.y / 100) * (CHART_H - 60);
              const co = getCompO(i);
              const cs = getCompScale(i);

              return (
                <div key={i} style={{
                  position: "absolute",
                  left: dotX, top: dotY,
                  transform: `translate(-50%, -50%) scale(${cs})`,
                  opacity: co,
                  display: "flex", flexDirection: "column", alignItems: "center", gap: 4,
                }}>
                  <div style={{
                    width: c.r * 2, height: c.r * 2, borderRadius: "50%",
                    background: c.highlight ? c.color : c.color + "33",
                    border: `2.5px solid ${c.color}`,
                    boxShadow: c.highlight ? `0 0 20px ${c.color}66` : "none",
                    display: "flex", alignItems: "center", justifyContent: "center",
                  }}>
                    {c.highlight && <span style={{ fontSize: 12 }}>💬</span>}
                  </div>
                  <div style={{
                    fontSize: c.highlight ? 14 : 12,
                    fontWeight: c.highlight ? 800 : 500,
                    color: c.highlight ? c.color : "#6B7280",
                    whiteSpace: "nowrap",
                    background: c.highlight ? "#EDE9FE" : "transparent",
                    padding: c.highlight ? "2px 8px" : "0",
                    borderRadius: 6,
                  }}>
                    {c.name}
                  </div>
                </div>
              );
            })}

            {/* SMSVote callout */}
            <div style={{
              position: "absolute",
              right: 16, top: 16,
              opacity: smsLabelO,
              transform: `scale(${smsLabelScale})`,
              background: "#7C3AED",
              borderRadius: 12, padding: "8px 16px",
              fontSize: 14, fontWeight: 700, color: "#FFF",
              whiteSpace: "nowrap",
              boxShadow: "0 4px 16px rgba(124,58,237,0.4)",
            }}>
              🏆 高易用 × 高智慧
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
