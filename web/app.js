import { initializeApp } from "https://www.gstatic.com/firebasejs/10.14.0/firebase-app.js";
import {
  getAuth, GoogleAuthProvider, signInWithPopup, signOut, onAuthStateChanged,
} from "https://www.gstatic.com/firebasejs/10.14.0/firebase-auth.js";
import {
  getFirestore, collection, doc, query, orderBy,
  onSnapshot, updateDoc, deleteDoc, getDoc,
} from "https://www.gstatic.com/firebasejs/10.14.0/firebase-firestore.js";

const firebaseConfig = {
  apiKey: "AIzaSyCCBguFTyBavOTPpDJNbt_0duMX8SmS148",
  authDomain: "smsvote-ca6e2.firebaseapp.com",
  projectId: "smsvote-ca6e2",
  storageBucket: "smsvote-ca6e2.firebasestorage.app",
  messagingSenderId: "15818876851",
  appId: "1:15818876851:web:9ed7bf1a12c5d89a558dc6",
  measurementId: "G-X8SV0QM2G2"
};

const app  = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db   = getFirestore(app);

const loginSection    = document.getElementById("login-section");
const checkingSection = document.getElementById("checking-section");
const deniedSection   = document.getElementById("denied-section");
const resultsSection  = document.getElementById("results-section");
const loginBtn        = document.getElementById("login-btn");
const logoutBtn       = document.getElementById("logout-btn");
const loginError      = document.getElementById("login-error");
const userNameEl      = document.getElementById("user-name");
const userRoleBadge   = document.getElementById("user-role-badge");
const pollsContainer  = document.getElementById("polls-container");
const noPollsEl       = document.getElementById("no-polls");

document.getElementById("denied-logout-btn").addEventListener("click", () => signOut(auth));

let pollsData        = [];
let currentUser      = null;
let currentUserRole  = null; // 'super_admin' | 'admin' | 'operator'
const voteCounts     = new Map();   // pollId -> { A: n, B: n, ... }
const voteListeners  = new Map();   // pollId -> unsubscribe
const chartTypes     = new Map();   // pollId -> 'bar' | 'pie'
const chartInstances = new Map();   // pollId -> Chart instance
const prevTotals     = new Map();   // pollId -> last rendered total (for animation)
const animFrames     = new Map();   // pollId -> requestAnimationFrame id
let unsubscribePolls = null;

const CHART_COLORS = ["#1565C0", "#2E7D32", "#E65100", "#6A1B9A", "#C62828"];

// ── Auth ──────────────────────────────────────────────────────
onAuthStateChanged(auth, (user) => {
  currentUser = user;
  if (user) { verifyAccess(user); } else { showLogin(); }
});

loginBtn.addEventListener("click", async () => {
  loginError.classList.add("hidden");
  try {
    await signInWithPopup(auth, new GoogleAuthProvider());
  } catch (e) {
    loginError.textContent = "登入失敗：" + e.message;
    loginError.classList.remove("hidden");
  }
});

logoutBtn.addEventListener("click", () => signOut(auth));

function hideAll() {
  [loginSection, checkingSection, deniedSection, resultsSection]
    .forEach(el => el.classList.add("hidden"));
}

function showLogin() {
  cleanup();
  hideAll();
  loginSection.classList.remove("hidden");
}

function showChecking() {
  hideAll();
  checkingSection.classList.remove("hidden");
}

function showDenied(icon, title, msg) {
  hideAll();
  document.getElementById("denied-icon").textContent  = icon;
  document.getElementById("denied-title").textContent = title;
  document.getElementById("denied-msg").textContent   = msg;
  deniedSection.classList.remove("hidden");
}

function showResults(user, role) {
  hideAll();
  resultsSection.classList.remove("hidden");
  userNameEl.textContent = user.displayName || user.email;
  const roleLabels = { super_admin: "👑 總管理員", admin: "⭐ 管理員", operator: "操作員" };
  userRoleBadge.textContent = roleLabels[role] || "";
  userRoleBadge.className   = "role-badge role-" + role;
  subscribeToPolls();
}

async function verifyAccess(user) {
  showChecking();
  try {
    const snap = await getDoc(doc(db, "users", user.uid));
    if (!snap.exists()) {
      showDenied("🚫", "無存取權限", "此 Google 帳號未在系統中登錄，請聯絡管理員以取得邀請碼。");
      return;
    }
    const data   = snap.data();
    const status = data.status ?? "pending";
    const role   = data.role   ?? "operator";
    if (status !== "approved") {
      showDenied("⏳", "帳號待審核", "你的帳號尚未獲得管理員核准，請稍候或聯絡管理員。");
      return;
    }
    currentUserRole = role;
    showResults(user, role);
  } catch (e) {
    showDenied("⚠️", "驗證失敗", "無法讀取帳號資料，請稍後再試。（" + e.message + "）");
  }
}

function cleanup() {
  if (unsubscribePolls) { unsubscribePolls(); unsubscribePolls = null; }
  voteListeners.forEach(u => u()); voteListeners.clear();
  voteCounts.clear();
  chartInstances.forEach(c => c.destroy()); chartInstances.clear();
  chartTypes.clear();
  animFrames.forEach(id => cancelAnimationFrame(id)); animFrames.clear();
  prevTotals.clear();
  pollsData = [];
  currentUserRole = null;
}

// ── Firestore reads ───────────────────────────────────────────
function subscribeToPolls() {
  const q = query(collection(db, "polls"), orderBy("createdAt", "desc"));
  unsubscribePolls = onSnapshot(q, (snapshot) => {
    const currentIds = new Set(snapshot.docs.map(d => d.id));
    voteListeners.forEach((unsub, id) => {
      if (!currentIds.has(id)) { unsub(); voteListeners.delete(id); voteCounts.delete(id); }
    });
    pollsData = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));
    pollsData.forEach(poll => { if (!voteListeners.has(poll.id)) subscribeToVotes(poll.id); });
    renderAll();
  });
}

function subscribeToVotes(pollId) {
  const unsub = onSnapshot(collection(db, "polls", pollId, "votes"), (snapshot) => {
    const counts = {};
    snapshot.forEach(d => {
      const data = d.data();
      const choices = data.choices || (data.choice ? [data.choice] : []);
      choices.forEach(c => { counts[c] = (counts[c] || 0) + 1; });
    });
    voteCounts.set(pollId, counts);
    renderAll();
  });
  voteListeners.set(pollId, unsub);
}

// ── Firestore writes ──────────────────────────────────────────
async function closePoll(pollId) {
  await updateDoc(doc(db, "polls", pollId), { endTime: Date.now() });
}

async function reopenPoll(pollId) {
  await updateDoc(doc(db, "polls", pollId), { endTime: null });
}

async function deletePollConfirmed(pollId, question) {
  showConfirm(`確定要刪除「${question}」？此動作無法復原。`, async () => {
    await deleteDoc(doc(db, "polls", pollId));
  });
}

// ── CSV export ────────────────────────────────────────────────
function exportCsv(poll) {
  const counts  = voteCounts.get(poll.id) || {};
  const rawOpts = poll.options || [poll.optionA, poll.optionB, poll.optionC].filter(Boolean);
  const total   = rawOpts.reduce((s, _, i) => s + (counts[String.fromCharCode(65 + i)] || 0), 0);

  const rows = [
    ["投票問題", poll.question],
    ["總票數", total],
    ["截止時間", poll.endTime ? new Date(poll.endTime).toLocaleString("zh-TW") : "無截止"],
    [],
    ["選項", "內容", "票數", "百分比"],
  ];
  rawOpts.forEach((text, i) => {
    const label = String.fromCharCode(65 + i);
    const count = counts[label] || 0;
    const pct   = total > 0 ? ((count / total) * 100).toFixed(1) : "0.0";
    rows.push([label, text, count, pct + "%"]);
  });

  const csv  = rows.map(r => r.map(c => `"${String(c).replace(/"/g, '""')}"`).join(",")).join("\n");
  const blob = new Blob(["﻿" + csv], { type: "text/csv;charset=utf-8;" });
  const url  = URL.createObjectURL(blob);
  const a    = Object.assign(document.createElement("a"), { href: url, download: `${poll.question.slice(0, 20)}_結果.csv` });
  a.click();
  URL.revokeObjectURL(url);
}

// ── Confirmation dialog ───────────────────────────────────────
function showConfirm(message, onOk) {
  document.getElementById("_dialog")?.remove();
  const el = document.createElement("div");
  el.id = "_dialog";
  el.className = "dialog-overlay";
  el.innerHTML = `
    <div class="dialog-box">
      <p class="dialog-msg">${esc(message)}</p>
      <div class="dialog-actions">
        <button class="btn-cancel">取消</button>
        <button class="btn-danger-ok">確認刪除</button>
      </div>
    </div>`;
  document.body.appendChild(el);
  el.querySelector(".btn-cancel").addEventListener("click", () => el.remove());
  el.querySelector(".btn-danger-ok").addEventListener("click", () => { el.remove(); onOk(); });
  el.addEventListener("click", e => { if (e.target === el) el.remove(); });
}

// ── Count-up animation ────────────────────────────────────────
function animateTo(pollId, target) {
  const el = document.getElementById(`total-${pollId}`);
  if (!el) return;
  const from = prevTotals.get(pollId) ?? 0;
  if (from === target) { el.textContent = target; return; }

  if (animFrames.has(pollId)) { cancelAnimationFrame(animFrames.get(pollId)); }

  const duration = Math.min(900, Math.max(300, Math.abs(target - from) * 60));
  const start    = performance.now();

  function step(now) {
    const p     = Math.min((now - start) / duration, 1);
    const eased = 1 - Math.pow(1 - p, 3);
    el.textContent = Math.round(from + (target - from) * eased);
    if (p < 1) { animFrames.set(pollId, requestAnimationFrame(step)); }
    else        { el.textContent = target; prevTotals.set(pollId, target); animFrames.delete(pollId); }
  }
  animFrames.set(pollId, requestAnimationFrame(step));
  prevTotals.set(pollId, target);
}

// ── Render ────────────────────────────────────────────────────
function renderAll() {
  chartInstances.forEach(c => c.destroy()); chartInstances.clear();

  pollsContainer.innerHTML = "";
  if (pollsData.length === 0) { noPollsEl.classList.remove("hidden"); return; }
  noPollsEl.classList.add("hidden");

  pollsData.forEach(poll => pollsContainer.appendChild(buildPollCard(poll)));

  // Animate totals and recreate pie charts after DOM is ready
  pollsData.forEach(poll => {
    const counts = voteCounts.get(poll.id) || {};
    const total  = Object.values(counts).reduce((s, v) => s + v, 0);
    animateTo(poll.id, total);
    if (chartTypes.get(poll.id) === "pie") createPieChart(poll.id, poll);
  });
}

function buildPollCard(poll) {
  const counts    = voteCounts.get(poll.id) || {};
  const rawOpts   = poll.options || [poll.optionA, poll.optionB, poll.optionC].filter(Boolean);
  const options   = rawOpts.map((text, i) => {
    const label = String.fromCharCode(65 + i);
    return { label, text, count: counts[label] || 0 };
  });
  const total      = options.reduce((s, o) => s + o.count, 0);
  const isExpired  = poll.endTime && Date.now() > poll.endTime;
  const maxChoices = poll.maxChoices || 1;
  const type       = chartTypes.get(poll.id) || "bar";
  const initTotal  = prevTotals.has(poll.id) ? prevTotals.get(poll.id) : 0;

  const endTimeStr = poll.endTime
    ? new Date(poll.endTime).toLocaleString("zh-TW", { month: "numeric", day: "numeric", hour: "2-digit", minute: "2-digit" })
    : null;

  const card = document.createElement("div");
  card.className = "poll-card" + (isExpired ? " poll-expired" : "");
  card.dataset.pollId = poll.id;

  card.innerHTML = `
    <div class="poll-header">
      <h2>${esc(poll.question)}</h2>
      <span class="badge ${isExpired ? "badge-expired" : "badge-active"}">${isExpired ? "已截止" : "進行中"}</span>
    </div>
    <p class="poll-meta">
      總票數：<strong id="total-${poll.id}">${initTotal}</strong>
      ${endTimeStr ? ` &nbsp;·&nbsp; 截止：${endTimeStr}` : ""}
      ${maxChoices > 1 ? ` &nbsp;·&nbsp; 最多選 ${maxChoices} 個` : ""}
    </p>

    <div class="poll-actions">
      ${isAdmin() ? `
        <button class="action-btn btn-toggle-poll" data-poll-id="${poll.id}" data-expired="${isExpired}">
          ${isExpired ? "🔓 重新開啟" : "🔒 截止投票"}
        </button>` : ""}
      <button class="action-btn btn-export" data-poll-id="${poll.id}">⬇ 匯出 CSV</button>
      ${isAdmin() ? `
        <button class="action-btn btn-delete" data-poll-id="${poll.id}">🗑 刪除</button>` : ""}
    </div>

    <div class="chart-toggle">
      <button class="toggle-btn ${type === "bar" ? "active" : ""}" data-type="bar">📊 長條圖</button>
      <button class="toggle-btn ${type === "pie" ? "active" : ""}" data-type="pie">🥧 圓餅圖</button>
    </div>

    <div class="bar-rows" ${type === "pie" ? 'style="display:none"' : ""}>
      ${options.map(({ label, text, count }) => {
        const pct = total > 0 ? Math.round((count / total) * 100) : 0;
        return `
          <div class="bar-row">
            <span class="bar-label">${label}</span>
            <div class="bar-col">
              <span class="bar-option-text">${esc(text)}</span>
              <div class="bar-track"><div class="bar-fill" style="width:${pct}%"></div></div>
            </div>
            <span class="bar-count">${count}<br><small>${pct}%</small></span>
          </div>`;
      }).join("")}
    </div>
    <div class="chart-canvas-wrapper" ${type === "bar" ? 'style="display:none"' : ""}>
      <canvas id="chart-${poll.id}"></canvas>
    </div>
  `;

  // Action buttons
  card.querySelector(".btn-toggle-poll").addEventListener("click", async (e) => {
    const btn = e.currentTarget;
    btn.disabled = true;
    try {
      if (btn.dataset.expired === "true") await reopenPoll(poll.id);
      else await closePoll(poll.id);
    } finally { btn.disabled = false; }
  });

  card.querySelector(".btn-export").addEventListener("click", () => exportCsv(poll));

  card.querySelector(".btn-delete").addEventListener("click", () =>
    deletePollConfirmed(poll.id, poll.question)
  );

  // Chart toggle
  card.querySelectorAll(".toggle-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      const newType = btn.dataset.type;
      if (chartTypes.get(poll.id) === newType) return;
      chartTypes.set(poll.id, newType);
      card.querySelectorAll(".toggle-btn").forEach(b =>
        b.classList.toggle("active", b.dataset.type === newType)
      );
      const barRows       = card.querySelector(".bar-rows");
      const canvasWrapper = card.querySelector(".chart-canvas-wrapper");
      if (newType === "pie") {
        barRows.style.display = "none";
        canvasWrapper.style.display = "block";
        createPieChart(poll.id, poll);
      } else {
        if (chartInstances.has(poll.id)) { chartInstances.get(poll.id).destroy(); chartInstances.delete(poll.id); }
        canvasWrapper.style.display = "none";
        barRows.style.display = "block";
      }
    });
  });

  return card;
}

function createPieChart(pollId, poll) {
  const canvas = document.getElementById(`chart-${pollId}`);
  if (!canvas) return;
  if (chartInstances.has(pollId)) { chartInstances.get(pollId).destroy(); chartInstances.delete(pollId); }

  const counts  = voteCounts.get(pollId) || {};
  const rawOpts = poll.options || [poll.optionA, poll.optionB, poll.optionC].filter(Boolean);
  const labels  = rawOpts.map((text, i) => `${String.fromCharCode(65 + i)}  ${text}`);
  const data    = rawOpts.map((_, i) => counts[String.fromCharCode(65 + i)] || 0);
  const total   = data.reduce((s, v) => s + v, 0);

  chartInstances.set(pollId, new Chart(canvas, {
    type: "pie",
    data: {
      labels,
      datasets: [{
        data,
        backgroundColor: CHART_COLORS.slice(0, rawOpts.length),
        borderWidth: 2,
        borderColor: "#fff",
      }]
    },
    options: {
      responsive: true,
      animation: { duration: 700 },
      plugins: {
        legend: { position: "bottom", labels: { font: { size: 13 }, padding: 16, color: "#333" } },
        tooltip: {
          callbacks: {
            label: ctx => {
              const pct = total > 0 ? Math.round(ctx.parsed / total * 100) : 0;
              return `  ${ctx.parsed} 票（${pct}%）`;
            }
          }
        }
      }
    }
  }));
}

const isAdmin = () => currentUserRole === "admin" || currentUserRole === "super_admin";
const esc = s => String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
