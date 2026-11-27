import React from "react";
import { useCurrentFrame, useVideoConfig, interpolate, spring } from "remotion";

const RealPhoneUI: React.FC<{ progress: number }> = ({ progress }) => {
  const fields = [
    { label: "投票標題", value: "2024 員工滿意度調查", filled: progress > 0.1 },
    { label: "選項 A", value: "非常滿意 😊", filled: progress > 0.3 },
    { label: "選項 B", value: "需要改善 🤔", filled: progress > 0.5 },
    { label: "選項 C", value: "不滿意 😞", filled: progress > 0.7 },
  ];

  return (
    <div style={{
      width: 300, height: 600,
      background: "#FAFAFA", borderRadius: 48,
      border: "10px solid #1E1B4B", overflow: "hidden",
      boxShadow: "0 32px 80px rgba(30,27,75,0.5)",
      display: "flex", flexDirection: "column",
    }}>
      <div style={{ background: "#7C3AED", padding: "12px 20px 8px", display: "flex", justifyContent: "space-between" }}>
        <div style={{ fontSize: 13, color: "#FFF", fontWeight: 600 }}>9:41</div>
        <div style={{ display: "flex", gap: 4 }}>
          {[1, 1, 1, 0.4].map((o, i) => <div key={i} style={{ width: 4, height: 10, background: `rgba(255,255,255,${o})`, borderRadius: 2 }} />)}
        </div>
      </div>
      <div style={{ background: "#7C3AED", padding: "10px 16px 18px", display: "flex", alignItems: "center", gap: 12 }}>
        <div style={{ fontSize: 18, color: "rgba(255,255,255,0.7)" }}>←</div>
        <div style={{ fontSize: 20, fontWeight: 700, color: "#FFF" }}>建立新投票</div>
      </div>
      <div style={{ flex: 1, padding: "16px 14px", display: "flex", flexDirection: "column", gap: 10, background: "#F3F4F6" }}>
        {fields.map((f, i) => (
          <div key={i} style={{ background: "#FFF", borderRadius: 12, padding: "10px 14px", boxShadow: "0 1px 4px rgba(0,0,0,0.06)", opacity: progress > i * 0.2 ? 1 : 0.3 }}>
            <div style={{ fontSize: 11, color: "#7C3AED", fontWeight: 600, marginBottom: 3 }}>{f.label}</div>
            <div style={{ fontSize: 14, color: f.filled ? "#1E1B4B" : "#D1D5DB", fontWeight: 500 }}>
              {f.filled ? f.value : "輸入文字..."}
            </div>
          </div>
        ))}
        <div style={{ background: "#FFF", borderRadius: 12, padding: "10px 14px", opacity: progress > 0.85 ? 1 : 0.3, display: "flex", justifyContent: "space-between" }}>
          <div>
            <div style={{ fontSize: 11, color: "#7C3AED", fontWeight: 600, marginBottom: 3 }}>截止日期</div>
            <div style={{ fontSize: 14, color: progress > 0.85 ? "#1E1B4B" : "#D1D5DB" }}>{progress > 0.85 ? "2024-12-31" : "選擇日期"}</div>
          </div>
          <div style={{ fontSize: 20 }}>📅</div>
        </div>
        <div style={{
          marginTop: 4,
          background: progress > 0.95 ? "linear-gradient(135deg, #7C3AED, #A78BFA)" : "#E5E7EB",
          borderRadius: 14, padding: "14px", textAlign: "center", fontSize: 16, fontWeight: 700,
          color: progress > 0.95 ? "#FFF" : "#9CA3AF",
          boxShadow: progress > 0.95 ? "0 6px 20px rgba(124,58,237,0.4)" : "none",
        }}>
          {progress > 0.95 ? "✓ 建立投票" : "建立投票"}
        </div>
      </div>
      <div style={{ background: "#FFF", padding: "8px 20px 12px", display: "flex", justifyContent: "space-around", borderTop: "1px solid #F3F4F6" }}>
        {["🏠", "🗳️", "👥", "📊"].map((icon, i) => (
          <div key={i} style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 2 }}>
            <div style={{ fontSize: 20 }}>{icon}</div>
            <div style={{ width: i === 1 ? 20 : 4, height: 3, borderRadius: 2, background: i === 1 ? "#7C3AED" : "transparent" }} />
          </div>
        ))}
      </div>
    </div>
  );
};

export const Feature1Scene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const leftOpacity = interpolate(frame, [0, 22], [0, 1], { extrapolateRight: "clamp" });
  const leftX = interpolate(frame, [0, 22], [-50, 0], { extrapolateRight: "clamp" });
  const phoneProgress = interpolate(frame, [28, fps * 4.5], [0, 1], { extrapolateRight: "clamp" });
  const phoneOpacity = interpolate(frame, [15, 32], [0, 1], { extrapolateRight: "clamp" });
  const phoneSpring = spring({ frame, fps, config: { damping: 14 }, delay: 15 });

  return (
    <div style={{
      width: "100%", height: "100%", background: "#FFFFFF",
      display: "flex",
      fontFamily: "'Helvetica Neue', Arial, 'PingFang TC', 'Microsoft JhengHei', sans-serif",
    }}>
      {/* LEFT 52% */}
      <div style={{ width: "52%", display: "flex", flexDirection: "column", justifyContent: "center", padding: "0 72px", opacity: leftOpacity, transform: `translateX(${leftX}px)` }}>
        <div style={{ display: "inline-flex", alignItems: "center", gap: 8, background: "#EDE9FE", borderRadius: 100, padding: "8px 18px", marginBottom: 24, alignSelf: "flex-start" }}>
          <div style={{ width: 10, height: 10, borderRadius: "50%", background: "#7C3AED" }} />
          <span style={{ fontSize: 16, color: "#7C3AED", fontWeight: 600, letterSpacing: 1 }}>功能 01 · Create Poll</span>
        </div>

        <div style={{ fontSize: 64, fontWeight: 900, color: "#1E1B4B", lineHeight: 1.1, marginBottom: 16 }}>
          輕鬆建立<br />
          <span style={{ background: "linear-gradient(135deg, #7C3AED, #06B6D4)", WebkitBackgroundClip: "text", WebkitTextFillColor: "transparent" }}>客製化投票</span>
        </div>
        <div style={{ fontSize: 22, color: "#6B7280", lineHeight: 1.7, marginBottom: 32 }}>
          支援<strong style={{ color: "#374151" }}>是非題、單選、多選</strong>等多種題型，<br />
          可設定截止日期與匿名選項，幾分鐘內完成設計
        </div>

        {[
          { n: "01", text: "輸入投票標題與詳細描述", detail: "支援繁體中文與英文" },
          { n: "02", text: "新增 2 ～ 10 個選項", detail: "可加入 emoji 讓選項更生動" },
          { n: "03", text: "設定截止時間與匿名模式", detail: "到期自動關閉收集" },
          { n: "04", text: "一鍵發布，即時生效", detail: "立刻可傳送給受訪者" },
        ].map((s, i) => {
          const o = interpolate(frame, [28 + i * 12, 46 + i * 12], [0, 1], { extrapolateRight: "clamp" });
          const x = interpolate(frame, [28 + i * 12, 46 + i * 12], [-16, 0], { extrapolateRight: "clamp" });
          return (
            <div key={i} style={{ display: "flex", alignItems: "flex-start", gap: 16, marginBottom: 16, opacity: o, transform: `translateX(${x}px)` }}>
              <div style={{ width: 36, height: 36, borderRadius: "50%", background: "#EDE9FE", color: "#7C3AED", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 14, fontWeight: 800, flexShrink: 0, marginTop: 2 }}>{s.n}</div>
              <div>
                <div style={{ fontSize: 22, color: "#1E1B4B", fontWeight: 600 }}>{s.text}</div>
                <div style={{ fontSize: 16, color: "#9CA3AF", marginTop: 2 }}>{s.detail}</div>
              </div>
            </div>
          );
        })}
      </div>

      {/* RIGHT 48% */}
      <div style={{
        width: "48%", height: "100%",
        background: "linear-gradient(145deg, #4C1D95 0%, #7C3AED 60%, #A78BFA 100%)",
        display: "flex", alignItems: "center", justifyContent: "center",
        position: "relative", overflow: "hidden",
      }}>
        <div style={{ position: "absolute", inset: 0, backgroundImage: "radial-gradient(circle, rgba(255,255,255,0.08) 1px, transparent 1px)", backgroundSize: "32px 32px" }} />
        <div style={{ opacity: phoneOpacity, transform: `scale(${phoneSpring}) rotate(-3deg)`, position: "relative", zIndex: 2 }}>
          <RealPhoneUI progress={phoneProgress} />
        </div>
      </div>
    </div>
  );
};
