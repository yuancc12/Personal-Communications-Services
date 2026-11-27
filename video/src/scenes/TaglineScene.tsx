import React from "react";
import { useCurrentFrame, useVideoConfig, interpolate, spring } from "remotion";

// Scene 2: 三欄三向飛入 — 左從左進、中從上切下、右從下往上
const PANELS = [
  {
    emoji: "🗳️",
    num: "01",
    title: "建立投票",
    titleEn: "Create Poll",
    desc: "自訂標題、選項與截止時間\n幾分鐘完成投票設計\n支援多種題型",
    bg: "linear-gradient(160deg, #4C1D95 0%, #7C3AED 100%)",
    textColor: "#FFF",
    subColor: "rgba(255,255,255,0.65)",
    numColor: "rgba(255,255,255,0.15)",
    accent: "#A78BFA",
  },
  {
    emoji: "📱",
    num: "02",
    title: "SMS 傳送",
    titleEn: "Send via SMS",
    desc: "批次傳送給所有聯絡人\n自動附上投票內容\n即時追蹤送達狀態",
    bg: "linear-gradient(160deg, #FFFFFF 0%, #F0F9FF 100%)",
    textColor: "#0C4A6E",
    subColor: "#0369A1",
    numColor: "rgba(8,145,178,0.08)",
    accent: "#0891B2",
  },
  {
    emoji: "🤖",
    num: "03",
    title: "AI 分析",
    titleEn: "AI Analysis",
    desc: "Jev AI 自動分類主題\n產生關鍵洞察與報表\n一鍵匯出 PDF",
    bg: "linear-gradient(160deg, #022c22 0%, #064e3b 100%)",
    textColor: "#FFF",
    subColor: "rgba(255,255,255,0.65)",
    numColor: "rgba(16,185,129,0.15)",
    accent: "#10B981",
  },
];

export const TaglineScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  // Panel 1: slides from LEFT
  const p1X = interpolate(frame, [0, 28], [-960, 0], {
    extrapolateRight: "clamp",
    easing: (t) => 1 - Math.pow(1 - t, 3),
  });
  const p1Opacity = interpolate(frame, [0, 20], [0, 1], { extrapolateRight: "clamp" });

  // Panel 2: slides from TOP DOWN
  const p2Y = interpolate(frame, [15, 42], [-540, 0], {
    extrapolateRight: "clamp",
    easing: (t) => 1 - Math.pow(1 - t, 3),
  });
  const p2Opacity = interpolate(frame, [15, 32], [0, 1], { extrapolateRight: "clamp" });

  // Panel 3: slides from BOTTOM UP
  const p3Y = interpolate(frame, [30, 56], [540, 0], {
    extrapolateRight: "clamp",
    easing: (t) => 1 - Math.pow(1 - t, 3),
  });
  const p3Opacity = interpolate(frame, [30, 46], [0, 1], { extrapolateRight: "clamp" });

  // Content fade-in inside each panel (staggered)
  const getContentOpacity = (delay: number) =>
    interpolate(frame, [delay, delay + 20], [0, 1], { extrapolateRight: "clamp" });
  const getContentY = (delay: number) =>
    interpolate(frame, [delay, delay + 20], [24, 0], { extrapolateRight: "clamp" });

  const transforms = [
    { x: p1X, y: 0, opacity: p1Opacity, contentDelay: 28 },
    { x: 0, y: p2Y, opacity: p2Opacity, contentDelay: 42 },
    { x: 0, y: p3Y, opacity: p3Opacity, contentDelay: 56 },
  ];

  return (
    <div style={{
      width: "100%", height: "100%",
      display: "flex",
      fontFamily: "'Helvetica Neue', Arial, 'PingFang TC', 'Microsoft JhengHei', sans-serif",
      overflow: "hidden",
    }}>
      {PANELS.map((panel, i) => {
        const t = transforms[i];
        return (
          <div
            key={i}
            style={{
              flex: 1,
              background: panel.bg,
              opacity: t.opacity,
              transform: `translateX(${t.x}px) translateY(${t.y}px)`,
              display: "flex",
              flexDirection: "column",
              justifyContent: "center",
              padding: "56px 52px",
              position: "relative",
              overflow: "hidden",
              borderRight: i < 2 ? "1px solid rgba(255,255,255,0.08)" : "none",
            }}
          >
            {/* Big number watermark */}
            <div style={{
              position: "absolute", bottom: 32, right: 36,
              fontSize: 160, fontWeight: 900,
              color: panel.numColor,
              lineHeight: 1,
              userSelect: "none",
            }}>
              {panel.num}
            </div>

            {/* Content */}
            <div style={{
              opacity: getContentOpacity(t.contentDelay),
              transform: `translateY(${getContentY(t.contentDelay)}px)`,
              position: "relative", zIndex: 2,
            }}>
              {/* Emoji circle */}
              <div style={{
                width: 96, height: 96, borderRadius: 28,
                background: `${panel.accent}33`,
                border: `2px solid ${panel.accent}66`,
                display: "flex", alignItems: "center", justifyContent: "center",
                fontSize: 48, marginBottom: 32,
              }}>
                {panel.emoji}
              </div>

              {/* Number label */}
              <div style={{
                fontSize: 15, fontWeight: 700, letterSpacing: 3,
                color: panel.accent, textTransform: "uppercase",
                marginBottom: 16,
              }}>
                Feature {panel.num}
              </div>

              {/* Title */}
              <div style={{
                fontSize: 72, fontWeight: 900,
                color: panel.textColor, lineHeight: 1.05,
                marginBottom: 10,
              }}>
                {panel.title}
              </div>
              <div style={{
                fontSize: 26, color: panel.accent,
                fontWeight: 500, marginBottom: 32,
              }}>
                {panel.titleEn}
              </div>

              {/* Description */}
              <div style={{
                fontSize: 24, color: panel.subColor,
                lineHeight: 1.75, whiteSpace: "pre-line",
              }}>
                {panel.desc}
              </div>

              {/* Bottom accent line */}
              <div style={{
                marginTop: 40, width: 60, height: 4,
                background: panel.accent, borderRadius: 2,
              }} />
            </div>
          </div>
        );
      })}
    </div>
  );
};
