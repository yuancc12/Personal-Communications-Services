import React from "react";
import { useCurrentFrame, useVideoConfig, interpolate, spring } from "remotion";

const TEAM = [
  { name: "許綾恩", id: "B11902150", school: "臺大資工三", role: "專案管理 · 創意發想", avatar: "許", color: "#7C3AED", bg: "#EDE9FE" },
  { name: "孫源智", id: "61415001E", school: "臺師大圖資碩二", role: "系統架構規劃與執行", avatar: "孫", color: "#059669", bg: "#D1FAE5" },
];

const TECHS = ["Kotlin", "Jetpack Compose", "Node.js", "Jev AI", "SMS API", "PDF Export"];

export const OutroScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const logoSpring = spring({ frame, fps, config: { damping: 12, stiffness: 70 } });
  const logoOpacity = interpolate(frame, [0, 15], [0, 1], { extrapolateRight: "clamp" });

  const titleOpacity = interpolate(frame, [15, 32], [0, 1], { extrapolateRight: "clamp" });
  const titleY = interpolate(frame, [15, 32], [24, 0], { extrapolateRight: "clamp" });

  const dividerW = interpolate(frame, [35, 60], [0, 400], { extrapolateRight: "clamp" });

  const teamOpacity = interpolate(frame, [60, 80], [0, 1], { extrapolateRight: "clamp" });
  const techOpacity = interpolate(frame, [80, 100], [0, 1], { extrapolateRight: "clamp" });

  return (
    <div style={{
      width: "100%", height: "100%",
      background: "linear-gradient(135deg, #F8FAFC 0%, #EEF2FF 50%, #F0FDF4 100%)",
      display: "flex", flexDirection: "column",
      alignItems: "center", justifyContent: "center",
      fontFamily: "'Helvetica Neue', Arial, 'PingFang TC', 'Microsoft JhengHei', sans-serif",
      position: "relative", overflow: "hidden",
      gap: 0,
    }}>
      {/* Background decorations */}
      <div style={{
        position: "absolute", width: 600, height: 600, borderRadius: "50%",
        background: "radial-gradient(circle, rgba(124,58,237,0.07) 0%, transparent 70%)",
        top: "-150px", left: "-150px",
      }} />
      <div style={{
        position: "absolute", width: 400, height: 400, borderRadius: "50%",
        background: "radial-gradient(circle, rgba(5,150,105,0.07) 0%, transparent 70%)",
        bottom: "-100px", right: "-100px",
      }} />

      {/* Logo */}
      <div style={{ opacity: logoOpacity, transform: `scale(${logoSpring})`, marginBottom: 20, zIndex: 2 }}>
        <div style={{
          width: 90, height: 90, borderRadius: 24,
          background: "linear-gradient(135deg, #7C3AED, #A78BFA)",
          display: "flex", alignItems: "center", justifyContent: "center",
          fontSize: 44, boxShadow: "0 16px 50px rgba(124,58,237,0.3)",
        }}>
          💬
        </div>
      </div>

      {/* Title */}
      <div style={{ opacity: titleOpacity, transform: `translateY(${titleY}px)`, textAlign: "center", zIndex: 2, marginBottom: 10 }}>
        <div style={{
          fontSize: 80, fontWeight: 900, letterSpacing: "-2px", color: "#1E1B4B", lineHeight: 1,
        }}>
          SMS<span style={{
            background: "linear-gradient(135deg, #7C3AED, #06B6D4)",
            WebkitBackgroundClip: "text", WebkitTextFillColor: "transparent",
          }}>Vote</span>
        </div>
        <div style={{ fontSize: 28, color: "#6B7280", marginTop: 12 }}>
          智慧簡訊投票平台 · Smart SMS Voting Platform
        </div>
      </div>

      {/* Divider */}
      <div style={{
        width: dividerW, height: 3,
        background: "linear-gradient(90deg, #7C3AED, #06B6D4, #10B981)",
        borderRadius: 100, margin: "24px 0 28px", zIndex: 2,
      }} />

      {/* Team cards */}
      <div style={{ opacity: teamOpacity, display: "flex", gap: 32, zIndex: 2, marginBottom: 32 }}>
        {TEAM.map((m, i) => {
          const cardSpring = spring({ frame, fps, config: { damping: 14, stiffness: 100 }, delay: 65 + i * 15 });
          return (
            <div key={i} style={{
              transform: `scale(${cardSpring})`,
              background: "#FFFFFF",
              borderRadius: 24,
              padding: "24px 36px",
              textAlign: "center",
              boxShadow: "0 8px 40px rgba(0,0,0,0.08)",
              border: `2px solid ${m.bg}`,
              minWidth: 260,
            }}>
              <div style={{
                width: 64, height: 64, borderRadius: "50%",
                background: m.bg, margin: "0 auto 14px",
                display: "flex", alignItems: "center", justifyContent: "center",
                fontSize: 24, fontWeight: 800, color: m.color,
              }}>
                {m.avatar}
              </div>
              <div style={{ fontSize: 28, fontWeight: 800, color: "#1E1B4B" }}>{m.name}</div>
              <div style={{ fontSize: 16, color: m.color, fontWeight: 600, marginTop: 6 }}>
                {m.id} · {m.school}
              </div>
              <div style={{ fontSize: 18, color: "#6B7280", marginTop: 6 }}>{m.role}</div>
            </div>
          );
        })}
      </div>

      {/* Tech stack pills */}
      <div style={{ opacity: techOpacity, display: "flex", gap: 10, flexWrap: "wrap", justifyContent: "center", zIndex: 2, maxWidth: 700 }}>
        {TECHS.map((t, i) => (
          <div key={i} style={{
            padding: "8px 18px", borderRadius: 100,
            background: "#FFFFFF", border: "1.5px solid #E5E7EB",
            fontSize: 14, color: "#6B7280", fontWeight: 500,
            boxShadow: "0 2px 8px rgba(0,0,0,0.05)",
          }}>
            {t}
          </div>
        ))}
      </div>

      {/* Bottom credit */}
      <div style={{
        position: "absolute", bottom: 28,
        opacity: interpolate(frame, [fps * 3.5, fps * 4.5], [0, 1], { extrapolateRight: "clamp" }),
        fontSize: 13, color: "#CBD5E1", zIndex: 2,
      }}>
        2024 期末專案 · 系統分析與設計
      </div>
    </div>
  );
};
