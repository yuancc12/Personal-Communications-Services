import React from "react";
import { useCurrentFrame, useVideoConfig, interpolate, spring } from "remotion";

export const IntroScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const logoSpring = spring({ frame, fps, config: { damping: 14, stiffness: 70 } });
  const logoOpacity = interpolate(frame, [0, 15], [0, 1], { extrapolateRight: "clamp" });
  const titleOpacity = interpolate(frame, [18, 35], [0, 1], { extrapolateRight: "clamp" });
  const titleY = interpolate(frame, [18, 35], [40, 0], { extrapolateRight: "clamp" });
  const subOpacity = interpolate(frame, [32, 50], [0, 1], { extrapolateRight: "clamp" });
  const subY = interpolate(frame, [32, 50], [20, 0], { extrapolateRight: "clamp" });
  const descOpacity = interpolate(frame, [50, 68], [0, 1], { extrapolateRight: "clamp" });
  const pillsOpacity = interpolate(frame, [68, 85], [0, 1], { extrapolateRight: "clamp" });

  return (
    <div style={{
      width: "100%", height: "100%",
      background: "linear-gradient(135deg, #EEF2FF 0%, #F0FDF4 50%, #FEF3C7 100%)",
      display: "flex", flexDirection: "column",
      alignItems: "center", justifyContent: "center",
      fontFamily: "'Helvetica Neue', Arial, 'PingFang TC', 'Microsoft JhengHei', sans-serif",
      position: "relative", overflow: "hidden",
    }}>
      {/* Background orbs */}
      <div style={{ position: "absolute", width: 600, height: 600, borderRadius: "50%", background: "radial-gradient(circle, rgba(124,58,237,0.12) 0%, transparent 70%)", top: "-150px", left: "-150px" }} />
      <div style={{ position: "absolute", width: 500, height: 500, borderRadius: "50%", background: "radial-gradient(circle, rgba(16,185,129,0.1) 0%, transparent 70%)", bottom: "-120px", right: "-120px" }} />

      {/* Logo */}
      <div style={{ opacity: logoOpacity, transform: `scale(${logoSpring})`, marginBottom: 24, zIndex: 2 }}>
        <div style={{
          width: 110, height: 110, borderRadius: 32,
          background: "linear-gradient(135deg, #7C3AED, #A78BFA)",
          display: "flex", alignItems: "center", justifyContent: "center",
          fontSize: 56, boxShadow: "0 24px 64px rgba(124,58,237,0.35)",
        }}>💬</div>
      </div>

      {/* Title */}
      <div style={{ opacity: titleOpacity, transform: `translateY(${titleY}px)`, textAlign: "center", zIndex: 2 }}>
        <div style={{ fontSize: 120, fontWeight: 900, lineHeight: 1, color: "#1E1B4B", letterSpacing: "-4px" }}>
          SMS<span style={{ background: "linear-gradient(135deg, #7C3AED, #06B6D4)", WebkitBackgroundClip: "text", WebkitTextFillColor: "transparent" }}>Vote</span>
        </div>
      </div>

      {/* Chinese subtitle */}
      <div style={{ opacity: subOpacity, transform: `translateY(${subY}px)`, textAlign: "center", marginTop: 16, zIndex: 2 }}>
        <div style={{ fontSize: 38, color: "#374151", fontWeight: 600 }}>智慧簡訊投票平台</div>
        <div style={{ fontSize: 24, color: "#9CA3AF", marginTop: 8 }}>Smart SMS Voting Platform</div>
      </div>

      {/* One-liner description */}
      <div style={{ opacity: descOpacity, textAlign: "center", marginTop: 24, zIndex: 2, maxWidth: 860 }}>
        <div style={{
          fontSize: 26, color: "#6B7280", lineHeight: 1.6,
          background: "rgba(255,255,255,0.7)", borderRadius: 16,
          padding: "16px 36px",
        }}>
          不需要 App、不需要帳號，<strong style={{ color: "#7C3AED" }}>一則 SMS 就能參與投票</strong>，收到即可回覆，操作門檻極低
        </div>
      </div>

      {/* Tech pills */}
      <div style={{ opacity: pillsOpacity, display: "flex", gap: 14, marginTop: 32, zIndex: 2 }}>
        {["Android · Kotlin", "AI-Powered", "Real-time SMS", "Web Dashboard", "PDF Export"].map((label, i) => (
          <div key={i} style={{
            padding: "10px 24px", borderRadius: 100,
            background: i % 2 === 0 ? "rgba(124,58,237,0.1)" : "rgba(5,150,105,0.1)",
            border: `1.5px solid ${i % 2 === 0 ? "rgba(124,58,237,0.3)" : "rgba(5,150,105,0.3)"}`,
            fontSize: 18, fontWeight: 600,
            color: i % 2 === 0 ? "#7C3AED" : "#059669",
          }}>{label}</div>
        ))}
      </div>
    </div>
  );
};
