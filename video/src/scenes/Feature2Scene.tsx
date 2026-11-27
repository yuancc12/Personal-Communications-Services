import React from "react";
import { useCurrentFrame, useVideoConfig, interpolate } from "remotion";

const CONTACTS = [
  [
    { name: "陳大明", dept: "人資部", status: "sent" },
    { name: "李雅君", dept: "財務部", status: "sent" },
    { name: "王志偉", dept: "工程部", status: "sending" },
    { name: "張雅婷", dept: "行銷部", status: "pending" },
  ],
  [
    { name: "劉建宏", dept: "業務部", status: "sent" },
    { name: "林小美", dept: "設計部", status: "sending" },
    { name: "吳俊廷", dept: "產品部", status: "pending" },
    { name: "黃靜怡", dept: "法務部", status: "pending" },
  ],
  [
    { name: "蔡宗翰", dept: "資訊部", status: "sent" },
    { name: "許淑芬", dept: "客服部", status: "sent" },
    { name: "鄭明哲", dept: "研發部", status: "sending" },
    { name: "江佩珊", dept: "採購部", status: "pending" },
  ],
];

const C: Record<string, { bg: string; color: string; label: string; dot: string }> = {
  sent: { bg: "#D1FAE5", color: "#059669", label: "已傳送 ✓", dot: "#10B981" },
  sending: { bg: "#FEF3C7", color: "#D97706", label: "傳送中...", dot: "#F59E0B" },
  pending: { bg: "#F3F4F6", color: "#9CA3AF", label: "等待中", dot: "#D1D5DB" },
};

export const Feature2Scene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const bannerOpacity = interpolate(frame, [0, 20], [0, 1], { extrapolateRight: "clamp" });
  const bannerY = interpolate(frame, [0, 20], [-40, 0], { extrapolateRight: "clamp" });
  const sentProgress = interpolate(frame, [30, fps * 4.2], [0, 9], { extrapolateRight: "clamp" });

  const getStatus = (col: number, row: number): "sent" | "sending" | "pending" => {
    const idx = col * 4 + row;
    if (sentProgress > idx + 0.7) return "sent";
    if (sentProgress > idx) return "sending";
    return "pending";
  };

  return (
    <div style={{
      width: "100%", height: "100%", background: "#F8FAFC",
      display: "flex", flexDirection: "column",
      fontFamily: "'Helvetica Neue', Arial, 'PingFang TC', 'Microsoft JhengHei', sans-serif",
    }}>
      {/* TOP BANNER */}
      <div style={{
        height: "30%",
        background: "linear-gradient(135deg, #0369A1 0%, #0891B2 100%)",
        display: "flex", alignItems: "center",
        padding: "0 90px", gap: 36,
        opacity: bannerOpacity, transform: `translateY(${bannerY}px)`,
        boxShadow: "0 4px 30px rgba(8,145,178,0.3)",
      }}>
        <div style={{ width: 80, height: 80, borderRadius: 24, background: "rgba(255,255,255,0.15)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 40 }}>📱</div>
        <div>
          <div style={{ fontSize: 16, color: "rgba(255,255,255,0.6)", letterSpacing: 2, textTransform: "uppercase", marginBottom: 6 }}>功能 02 · Bulk SMS Delivery</div>
          <div style={{ fontSize: 52, fontWeight: 900, color: "#FFF", lineHeight: 1 }}>批次 SMS 傳送</div>
          <div style={{ fontSize: 22, color: "rgba(255,255,255,0.75)", marginTop: 6 }}>
            即時追蹤每筆傳送狀態，自動附上投票連結，支援上百位收件人
          </div>
        </div>
        <div style={{ marginLeft: "auto", textAlign: "center", background: "rgba(255,255,255,0.1)", borderRadius: 20, padding: "16px 28px" }}>
          <div style={{ fontSize: 52, fontWeight: 900, color: "#FFF" }}>
            {Math.min(Math.floor(sentProgress), 9)}<span style={{ fontSize: 26, color: "rgba(255,255,255,0.5)" }}>/12</span>
          </div>
          <div style={{ fontSize: 16, color: "rgba(255,255,255,0.6)" }}>已傳送</div>
        </div>
      </div>

      {/* BOTTOM — 3 columns */}
      <div style={{ flex: 1, display: "flex", padding: "20px 56px", gap: 20 }}>
        {CONTACTS.map((col, ci) => {
          const colOpacity = interpolate(frame, [15 + ci * 8, 28 + ci * 8], [0, 1], { extrapolateRight: "clamp" });
          return (
            <div key={ci} style={{ flex: 1, opacity: colOpacity, display: "flex", flexDirection: "column", gap: 10 }}>
              <div style={{ fontSize: 15, color: "#9CA3AF", textAlign: "center", marginBottom: 2, fontWeight: 600 }}>批次 {ci + 1}</div>
              {col.map((contact, ri) => {
                const status = getStatus(ci, ri);
                const c = C[status];
                return (
                  <div key={ri} style={{
                    background: "#FFF", borderRadius: 16,
                    padding: "14px 18px",
                    display: "flex", alignItems: "center", gap: 14,
                    boxShadow: "0 2px 12px rgba(0,0,0,0.06)",
                    border: `1.5px solid ${status === "sent" ? "#D1FAE5" : "#F3F4F6"}`,
                  }}>
                    <div style={{
                      width: 46, height: 46, borderRadius: "50%",
                      background: status === "sent" ? "#D1FAE5" : status === "sending" ? "#FEF3C7" : "#F3F4F6",
                      display: "flex", alignItems: "center", justifyContent: "center",
                      fontSize: 18, fontWeight: 700, color: "#374151", flexShrink: 0,
                    }}>{contact.name[0]}</div>
                    <div style={{ flex: 1 }}>
                      <div style={{ fontSize: 18, fontWeight: 700, color: "#1E1B4B" }}>{contact.name}</div>
                      <div style={{ fontSize: 14, color: "#9CA3AF" }}>{contact.dept}</div>
                      <div style={{ fontSize: 14, color: c.color, fontWeight: 600, marginTop: 2 }}>{c.label}</div>
                    </div>
                    <div style={{ width: 10, height: 10, borderRadius: "50%", background: c.dot, boxShadow: status === "sending" ? `0 0 8px ${c.dot}` : "none" }} />
                  </div>
                );
              })}
            </div>
          );
        })}
      </div>
    </div>
  );
};
