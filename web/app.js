import { initializeApp } from "https://www.gstatic.com/firebasejs/10.14.0/firebase-app.js";
import {
  getAuth, GoogleAuthProvider, signInWithPopup, signOut, onAuthStateChanged,
} from "https://www.gstatic.com/firebasejs/10.14.0/firebase-auth.js";
import {
  getFirestore, collection, doc, query, orderBy,
  onSnapshot, updateDoc, deleteDoc, getDoc, setDoc,
} from "https://www.gstatic.com/firebasejs/10.14.0/firebase-firestore.js";

emailjs.init({ publicKey: "CaJ7sQbJPC3OCcRHw" });

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
const adminPanelBtn   = document.getElementById("admin-panel-btn");
const pollsContainer  = document.getElementById("polls-container");
const noPollsEl       = document.getElementById("no-polls");

document.getElementById("denied-logout-btn").addEventListener("click", () => signOut(auth));

let pollsData        = [];
let currentUser      = null;
let currentUserRole  = null; // 'super_admin' | 'admin' | 'operator'
let usersData        = [];
let usersUnsub       = null;
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
  if (role === "admin" || role === "super_admin") {
    adminPanelBtn.classList.remove("hidden");
    adminPanelBtn.onclick = openAdminPanel;
    subscribeToUsers();
  }
  subscribeToPolls();
}

async function verifyAccess(user) {
  showChecking();
  try {
    const snap = await getDoc(doc(db, "users", user.uid));

    if (!snap.exists()) {
      // 帳號從未在 App 登錄過 — 嘗試自動建立 pending 文件讓管理員審核
      try {
        await setDoc(doc(db, "users", user.uid), {
          uid: user.uid,
          email: user.email ?? "",
          displayName: user.displayName ?? "",
          role: "operator",
          status: "pending",
          createdAt: Date.now()
        });
        showDenied("⏳", "帳號待審核",
          `帳號 ${user.email} 已送出申請，請等待管理員核准後再登入。`);
      } catch {
        // 寫入也被擋 → Rules 限制或帳號完全不在系統中
        showDenied("🚫", "無存取權限",
          `目前登入的帳號是 ${user.email}。\n\n` +
          `此帳號尚未在系統中登錄。請確認：\n` +
          `① 網頁與 Android App 使用的是同一個 Google 帳號\n` +
          `② 請先在 Android App 登入，或請管理員在「預先授權」輸入此 Email`);
      }
      return;
    }

    const data   = snap.data();
    const role   = data.role ?? "operator";
    const status = data.status ?? (role === "super_admin" ? "approved" : "pending");

    if (!data.status && role === "super_admin") {
      updateDoc(doc(db, "users", user.uid), { status: "approved" }).catch(() => {});
    }

    if (status !== "approved") {
      showDenied("⏳", "帳號待審核",
        `帳號 ${user.email} 尚未獲得管理員核准，請稍候或聯絡管理員。`);
      return;
    }

    currentUserRole = role;
    showResults(user, role);
  } catch (e) {
    showDenied("⚠️", "驗證失敗",
      `帳號 ${user.email} 驗證失敗。\n\n` +
      `錯誤：${e.message}\n\n` +
      `如果剛剛剛才部署了 Firestore Rules，請確認規則已生效（firebase deploy --only firestore:rules）`);
  }
}

function cleanup() {
  if (unsubscribePolls) { unsubscribePolls(); unsubscribePolls = null; }
  if (usersUnsub)       { usersUnsub(); usersUnsub = null; }
  voteListeners.forEach(u => u()); voteListeners.clear();
  voteCounts.clear();
  chartInstances.forEach(c => c.destroy()); chartInstances.clear();
  chartTypes.clear();
  animFrames.forEach(id => cancelAnimationFrame(id)); animFrames.clear();
  prevTotals.clear();
  pollsData = []; usersData = [];
  currentUserRole = null;
  document.getElementById("_admin_panel")?.remove();
}

// ── Admin panel ───────────────────────────────────────────────
function subscribeToUsers() {
  usersUnsub = onSnapshot(collection(db, "users"), snap => {
    usersData = snap.docs.map(d => ({ id: d.id, ...d.data() }));
    refreshAdminPanel();
  });
}

function openAdminPanel() {
  document.getElementById("_admin_panel")?.remove();
  const el = document.createElement("div");
  el.id = "_admin_panel";
  el.className = "admin-overlay";
  el.innerHTML = `
    <div class="admin-drawer">
      <div class="admin-drawer-header">
        <h2>帳號管理</h2>
        <button class="admin-close-btn">✕</button>
      </div>
      <div class="admin-tabs">
        <button class="atab active" data-tab="pending">待審核</button>
        <button class="atab" data-tab="preapprove">預先授權</button>
        <button class="atab" data-tab="allusers">所有帳號</button>
      </div>
      <div id="admin-tab-pending"    class="admin-tab-content"></div>
      <div id="admin-tab-preapprove" class="admin-tab-content hidden"></div>
      <div id="admin-tab-allusers"   class="admin-tab-content hidden"></div>
    </div>`;
  document.body.appendChild(el);

  el.querySelector(".admin-close-btn").addEventListener("click", () => el.remove());
  el.addEventListener("click", e => { if (e.target === el) el.remove(); });
  el.querySelectorAll(".atab").forEach(btn => {
    btn.addEventListener("click", () => {
      el.querySelectorAll(".atab").forEach(b => b.classList.remove("active"));
      btn.classList.add("active");
      el.querySelectorAll(".admin-tab-content").forEach(t => t.classList.add("hidden"));
      el.querySelector(`#admin-tab-${btn.dataset.tab}`).classList.remove("hidden");
    });
  });

  refreshAdminPanel();
}

function refreshAdminPanel() {
  const el = document.getElementById("_admin_panel");
  if (!el) return;

  // ── Tab: 待審核 ──
  const pending = usersData.filter(u => u.status === "pending");
  const pendingTab = el.querySelector("#admin-tab-pending");
  if (pending.length === 0) {
    pendingTab.innerHTML = `<p class="admin-empty">目前沒有待審核帳號</p>`;
  } else {
    pendingTab.innerHTML = pending.map(u => `
      <div class="admin-user-row">
        <div class="admin-user-info">
          <span class="admin-user-name">${esc(u.displayName || "（未命名）")}</span>
          <span class="admin-user-email">${esc(u.email || "")}</span>
        </div>
        <div class="admin-user-actions">
          <button class="abtn abtn-approve" data-uid="${u.id}">核准</button>
          <button class="abtn abtn-reject"  data-uid="${u.id}">拒絕</button>
        </div>
      </div>`).join("");
    pendingTab.querySelectorAll(".abtn-approve").forEach(btn =>
      btn.addEventListener("click", () => approveUserWeb(btn.dataset.uid))
    );
    pendingTab.querySelectorAll(".abtn-reject").forEach(btn =>
      btn.addEventListener("click", () => {
        showConfirm("確定要拒絕並刪除此帳號？", () => rejectUserWeb(btn.dataset.uid));
      })
    );
  }

  // ── Tab: 預先授權 ──
  const preTab = el.querySelector("#admin-tab-preapprove");
  if (!preTab.querySelector(".pre-form")) {
    preTab.innerHTML = `
      <p class="admin-hint">輸入對方的 Google 帳號 Email，對方登入時會自動核准並獲得指定角色。</p>
      <div class="pre-form">
        <input id="pre-email" type="email" placeholder="example@gmail.com" />
        <select id="pre-role">
          <option value="operator">操作員</option>
          <option value="admin">管理員</option>
        </select>
        <button id="pre-submit" class="abtn abtn-approve">新增並發信</button>
      </div>
      <p id="pre-msg" class="admin-msg hidden"></p>
      <div id="pre-list"></div>`;
    preTab.querySelector("#pre-submit").addEventListener("click", async () => {
      const email = preTab.querySelector("#pre-email").value.trim().toLowerCase();
      const role  = preTab.querySelector("#pre-role").value;
      const msg   = preTab.querySelector("#pre-msg");
      if (!email.includes("@")) { showPreMsg(msg, "請輸入有效的 Email", "error"); return; }
      const submitBtn = preTab.querySelector("#pre-submit");
      submitBtn.disabled = true;
      submitBtn.textContent = "發送中…";
      try {
        const roleText = role === "admin" ? "管理員" : "操作員";

        // 1. 寫入 pre_approved（登入自動核准）
        await setDoc(doc(db, "pre_approved", email), {
          email, role,
          createdBy: currentUser?.uid ?? "",
          createdAt: Date.now()
        });

        // 2. 用 EmailJS 直接從瀏覽器發送邀請信
        await emailjs.send("service_z7q6mq4", "template_z4nfj6r", {
          to_email: email,
          role_text: roleText,
        }, { publicKey: "CaJ7sQbJPC3OCcRHw" });

        preTab.querySelector("#pre-email").value = "";
        showPreMsg(msg, `✓ 已新增並發送邀請信給 ${email}`, "ok");
        loadPreApproved(preTab.querySelector("#pre-list"));
      } catch (e) {
        console.error("emailjs error", e);
        showPreMsg(msg, "失敗：" + (e.text || e.message || JSON.stringify(e)), "error");
      } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = "新增並發信";
      }
    });
    loadPreApproved(preTab.querySelector("#pre-list"));
  }

  // ── Tab: 所有帳號 ──
  const allTab = el.querySelector("#admin-tab-allusers");
  const roleLabel = r => ({ super_admin: "👑 總管理員", admin: "⭐ 管理員", operator: "操作員" }[r] ?? r);
  const approved  = usersData.filter(u => u.status === "approved");
  allTab.innerHTML = approved.length === 0 ? `<p class="admin-empty">尚無已核准帳號</p>` :
    approved.map(u => `
      <div class="admin-user-row">
        <div class="admin-user-info">
          <span class="admin-user-name">${esc(u.displayName || "（未命名）")}</span>
          <span class="admin-user-email">${esc(u.email || "")}</span>
        </div>
        <span class="role-badge role-${u.role}">${roleLabel(u.role)}</span>
      </div>`).join("");
}

async function loadPreApproved(container) {
  const snap = await import("https://www.gstatic.com/firebasejs/10.14.0/firebase-firestore.js")
    .then(m => m.getDocs(m.collection(db, "pre_approved")));
  if (snap.empty) { container.innerHTML = `<p class="admin-empty" style="margin-top:12px">尚無預先授權記錄</p>`; return; }
  const roleLabel = r => r === "admin" ? "管理員" : "操作員";
  container.innerHTML = `<div style="margin-top:12px;font-size:.8rem;color:#888;margin-bottom:6px">已授權清單</div>` +
    snap.docs.map(d => `
      <div class="admin-user-row">
        <div class="admin-user-info">
          <span class="admin-user-email">${esc(d.id)}</span>
        </div>
        <span class="role-badge role-${d.data().role}">${roleLabel(d.data().role)}</span>
        <button class="abtn abtn-reject" data-email="${d.id}" style="margin-left:8px">移除</button>
      </div>`).join("");
  container.querySelectorAll(".abtn-reject").forEach(btn =>
    btn.addEventListener("click", async () => {
      await deleteDoc(doc(db, "pre_approved", btn.dataset.email));
      loadPreApproved(container);
    })
  );
}

function showPreMsg(el, msg, type) {
  el.textContent = msg;
  el.className = "admin-msg " + type;
  el.classList.remove("hidden");
  setTimeout(() => el.classList.add("hidden"), 4000);
}

async function approveUserWeb(uid) {
  await updateDoc(doc(db, "users", uid), { status: "approved" });
}

async function rejectUserWeb(uid) {
  await deleteDoc(doc(db, "users", uid));
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
