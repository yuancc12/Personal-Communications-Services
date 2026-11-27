import { initializeApp } from "https://www.gstatic.com/firebasejs/10.14.0/firebase-app.js";
import {
  getAuth, GoogleAuthProvider, signInWithPopup, signInWithRedirect,
  getRedirectResult, signOut, onAuthStateChanged,
} from "https://www.gstatic.com/firebasejs/10.14.0/firebase-auth.js";
import {
  getFirestore, collection, doc, query, orderBy, where,
  onSnapshot, updateDoc, deleteDoc, getDoc, setDoc, getDocs,
} from "https://www.gstatic.com/firebasejs/10.14.0/firebase-firestore.js";

// EmailJS 用 fetch 直接呼叫，不依賴 SDK 初始化
async function sendInviteEmail(toEmail, roleText) {
  const publicKey  = window.EMAILJS_PUBLIC_KEY  || "";
  const privateKey = window.EMAILJS_PRIVATE_KEY || "";
  if (!publicKey) throw new Error("EmailJS public key 未設定，請在 config.js 填入 EMAILJS_PUBLIC_KEY");
  const res = await fetch("https://api.emailjs.com/api/v1.0/email/send", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      service_id:     "service_z7q6mq4",
      template_id:    "template_z4nfj6r",
      user_id:        publicKey,
      accessToken:    privateKey,
      template_params: { to_email: toEmail, role_text: roleText }
    })
  });
  const text = await res.text();
  if (!res.ok) throw { status: res.status, text };
}

// ── 主題切換 ──────────────────────────────────────────────────
(function () {
  const saved = localStorage.getItem("ui_theme") || "";
  if (saved) document.body.dataset.theme = saved;

  document.addEventListener("click", e => {
    const dot = e.target.closest(".theme-dot");
    if (!dot) return;
    const theme = dot.dataset.theme || "";
    document.body.dataset.theme = theme;
    localStorage.setItem("ui_theme", theme);
    document.querySelectorAll(".theme-dot").forEach(d =>
      d.classList.toggle("active", (d.dataset.theme || "") === theme)
    );
  });
})();

// Load AI provider + key from localStorage
(function () {
  const p = localStorage.getItem("ai_provider") || "gemini";
  const k = localStorage.getItem("ai_key_" + p) || "";
  window._aiProvider = p;
  window._aiApiKey   = k;
  if (p === "gemini" && k) window.GEMINI_API_KEY = k;
})();

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

const CHART_COLORS = ["#1565C0", "#00897B", "#F9A825", "#E53935", "#0288D1"];

// ── Auth ──────────────────────────────────────────────────────
onAuthStateChanged(auth, (user) => {
  currentUser = user;
  if (user) { verifyAccess(user); } else { showLogin(); }
});

let signingIn = false;
loginBtn.addEventListener("click", async () => {
  if (signingIn) return;
  signingIn = true;
  loginError.classList.add("hidden");
  loginBtn.disabled = true;
  const provider = new GoogleAuthProvider();
  try {
    await signInWithPopup(auth, provider);
  } catch (e) {
    if (e.code === "auth/popup-blocked" || e.code === "auth/popup-closed-by-user") {
      await signInWithRedirect(auth, provider);
    } else if (e.code !== "auth/cancelled-popup-request") {
      loginError.textContent = "登入失敗：" + e.message;
      loginError.classList.remove("hidden");
    }
  } finally {
    signingIn = false;
    loginBtn.disabled = false;
  }
});

getRedirectResult(auth).catch(() => {});

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
  document.getElementById("help-btn").onclick = () => openHelpModal(false);
  subscribeToPolls();
  // 自動顯示操作說明（除非已勾選不再顯示）
  setTimeout(() => openHelpModal(true), 600);
}

async function verifyAccess(user) {
  showChecking();
  try {
    const snap = await getDoc(doc(db, "users", user.uid));

    if (!snap.exists()) {
      // Check pre_approved first — auto-approve if admin pre-authorized this email
      const preSnap = await getDoc(doc(db, "pre_approved", user.email.toLowerCase()));
      if (preSnap.exists()) {
        const preData = preSnap.data();
        const role = preData.role || "operator";
        await setDoc(doc(db, "users", user.uid), {
          uid: user.uid,
          email: user.email ?? "",
          displayName: user.displayName ?? "",
          role: role,
          status: "approved",
          createdAt: Date.now()
        });
        currentUserRole = role;
        showResults(user, role);
        return;
      }

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

    if (status === "rejected") {
      showDenied("🚫", "帳號已停用",
        `帳號 ${user.email} 已被管理員停用。\n如有疑問，請聯絡管理員。`);
      return;
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
        <button class="atab" data-tab="settings">⚙️ 設定</button>
      </div>
      <div id="admin-tab-pending"    class="admin-tab-content"></div>
      <div id="admin-tab-preapprove" class="admin-tab-content hidden"></div>
      <div id="admin-tab-allusers"   class="admin-tab-content hidden"></div>
      <div id="admin-tab-settings"   class="admin-tab-content hidden"></div>
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

        // 1. 寫入 pre_approved（登入自動核准）— 一定要成功
        await setDoc(doc(db, "pre_approved", email), {
          email, role,
          createdBy: currentUser?.uid ?? "",
          createdAt: Date.now()
        });
        preTab.querySelector("#pre-email").value = "";
        loadPreApproved(preTab.querySelector("#pre-list"));

        // 2. 嘗試發邀請信（非關鍵，失敗只顯示警告）
        try {
          await sendInviteEmail(email, roleText);
          showPreMsg(msg, `✓ 已新增並發送邀請信給 ${email}`, "ok");
        } catch (emailErr) {
          console.error("❌ email error — status:", emailErr.status, "| text:", emailErr.text, "| full:", emailErr);
          const status = emailErr.status || "";
          const detail = emailErr.text || emailErr.message || JSON.stringify(emailErr);
          const hint = detail.toLowerCase().includes("publickeyisrequired") || detail.toLowerCase().includes("publickey")
            ? "（EmailJS 公鑰未設定，請確認 config.js 的 EMAILJS_PUBLIC_KEY）"
            : status === 400
              ? "（請至 EmailJS 後台確認 Template 變數名稱是否正確）"
              : status === 429 || detail.includes("limit")
                ? "（已達本月免費額度 200 封上限）"
                : "";
          showPreMsg(msg, `✓ 已新增 ${email}，但邀請信失敗${hint}：${detail}`, "warn");
        }
      } catch (e) {
        console.error("firestore error", e);
        showPreMsg(msg, "新增失敗：" + (e.message || JSON.stringify(e)), "error");
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
  allTab.innerHTML = (approved.length === 0 ? `<p class="admin-empty">尚無已核准帳號</p>` :
    approved.map(u => `
      <div class="admin-user-row">
        <div class="admin-user-info">
          <span class="admin-user-name">${esc(u.displayName || "（未命名）")}</span>
          <span class="admin-user-email">${esc(u.email || "")}</span>
        </div>
        <span class="role-badge role-${u.role}">${roleLabel(u.role)}</span>
        ${u.role !== "super_admin" ? `<button class="abtn abtn-reject allusers-remove-btn" data-uid="${u.id}" title="移除此帳號" style="margin-left:4px">移除</button>` : ""}
      </div>`).join("")) + `<p id="allusers-msg" class="admin-msg hidden" style="margin-top:8px"></p>`;
  allTab.querySelectorAll(".allusers-remove-btn").forEach(btn =>
    btn.addEventListener("click", () => {
      const name = btn.closest(".admin-user-row").querySelector(".admin-user-name").textContent;
      const msgEl = allTab.querySelector("#allusers-msg");
      showConfirm(`確定要停用「${name}」的帳號？對方將無法再登入，此動作可聯絡管理員復原。`,
        () => removeApprovedUser(btn.dataset.uid, msgEl));
    })
  );

  // ── Tab: 設定 ──
  const settingsTab = el.querySelector("#admin-tab-settings");
  if (!settingsTab.querySelector(".settings-form")) {
    const providerNames = { gemini: "Gemini API Key（Google AI Studio）", openai: "API Key（OpenAI）", claude: "API Key（Anthropic）" };
    settingsTab.innerHTML = `
      <p class="admin-hint">設定 AI 分析助理的服務與 API Key，Key 只儲存在你的瀏覽器本機。</p>
      <div class="settings-form">
        <div class="settings-row">
          <label>AI 服務</label>
          <select id="settings-provider">
            <option value="gemini">🔵 Gemini（Google）</option>
            <option value="openai">🟢 GPT（OpenAI）</option>
            <option value="claude">🟠 Claude（Anthropic）</option>
          </select>
        </div>
        <div class="settings-row">
          <label id="settings-key-label">${providerNames[window._aiProvider || "gemini"]}</label>
          <input type="password" id="settings-key-input" placeholder="貼上 API Key…" autocomplete="off" />
        </div>
        <button id="settings-save-btn" class="abtn abtn-approve">💾 儲存</button>
        <p id="settings-msg" class="admin-msg hidden"></p>
      </div>
      <hr style="margin:20px 0;border:none;border-top:1px solid var(--border)">
      <p class="admin-hint">為沒有分類的舊投票自動補上 AI 分類標籤。</p>
      <button id="classify-old-btn" class="abtn abtn-approve">🏷️ 分類舊投票</button>
      <p id="classify-msg" class="admin-msg hidden"></p>`;

    const providerSel = settingsTab.querySelector("#settings-provider");
    const keyInput    = settingsTab.querySelector("#settings-key-input");
    const keyLabel    = settingsTab.querySelector("#settings-key-label");
    const saveBtn     = settingsTab.querySelector("#settings-save-btn");
    const settingsMsg = settingsTab.querySelector("#settings-msg");

    // Load saved values
    providerSel.value = window._aiProvider || "gemini";
    keyInput.value    = localStorage.getItem("ai_key_" + providerSel.value) || "";
    keyLabel.textContent = providerNames[providerSel.value];

    providerSel.addEventListener("change", () => {
      keyLabel.textContent = providerNames[providerSel.value];
      keyInput.value = localStorage.getItem("ai_key_" + providerSel.value) || "";
    });

    saveBtn.addEventListener("click", () => {
      const p = providerSel.value;
      const k = keyInput.value.trim();
      if (!k) { settingsMsg.textContent = "請輸入 API Key"; settingsMsg.className = "admin-msg error"; settingsMsg.classList.remove("hidden"); return; }
      localStorage.setItem("ai_provider", p);
      localStorage.setItem("ai_key_" + p, k);
      window._aiProvider = p;
      window._aiApiKey   = k;
      if (p === "gemini") window.GEMINI_API_KEY = k;
      settingsMsg.textContent = "✓ 已儲存";
      settingsMsg.className = "admin-msg ok";
      settingsMsg.classList.remove("hidden");
      setTimeout(() => settingsMsg.classList.add("hidden"), 3000);
    });

    const classifyBtn = settingsTab.querySelector("#classify-old-btn");
    const classifyMsg = settingsTab.querySelector("#classify-msg");
    classifyBtn.addEventListener("click", async () => {
      classifyBtn.disabled = true;
      classifyMsg.textContent = "⏳ 讀取投票中…";
      classifyMsg.className = "admin-msg ok";
      classifyMsg.classList.remove("hidden");
      try {
        const snap = await getDocs(collection(db, "polls"));
        const unclassified = snap.docs.filter(d => !d.data().category);
        if (unclassified.length === 0) {
          classifyMsg.textContent = "✅ 所有投票已有分類，無需更新";
          classifyBtn.disabled = false;
          return;
        }
        let done = 0;
        for (const d of unclassified) {
          const data = d.data();
          const opts = data.options || [data.optionA, data.optionB, data.optionC].filter(Boolean);
          classifyMsg.textContent = `⏳ 分類中 ${done + 1} / ${unclassified.length}：${data.question?.slice(0, 20)}…`;
          const cat = await jevClassify(data.question || "", opts);
          await updateDoc(doc(db, "polls", d.id), { category: cat });
          done++;
        }
        classifyMsg.textContent = `✅ 完成！已分類 ${done} 筆投票`;
      } catch (e) {
        classifyMsg.textContent = `❌ 失敗：${e.message}`;
        classifyMsg.className = "admin-msg error";
      }
      classifyBtn.disabled = false;
    });
  }
}

async function jevClassify(question, options) {
  try {
    const res = await fetch("https://api.typesafe.ai/v1/systemone", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${window.JEV_API_KEY}`
      },
      body: JSON.stringify({
        state: `投票問題：${question}\n選項：${options.join("、")}`,
        model: "jev-latest",
        questions: {
          category: {
            type: "choice",
            instructions: "這個投票屬於哪個類別？",
            criteria: {
              "政治": "政治相關、選舉、政府政策、公共事務",
              "美食": "食物、餐廳、料理、飲食偏好",
              "娛樂": "電影、音樂、遊戲、娛樂活動",
              "體育": "運動、競技、球賽、健身",
              "工作": "職場、工作、商業、職業",
              "教育": "學習、學校、課程、教育",
              "健康": "健康、醫療、養生、身體",
              "生活": "日常生活、生活習慣、居家",
              "科技": "科技、電腦、手機、網路",
              "其他": "不屬於以上任何類別"
            }
          }
        }
      })
    });
    if (!res.ok) return "其他";
    const json = await res.json();
    return json.answers?.category?.choice || "其他";
  } catch { return "其他"; }
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
  // 待審核帳號：直接刪除（此人從未被核准過，可重新申請）
  await deleteDoc(doc(db, "users", uid));
}

async function removeApprovedUser(uid, msgEl) {
  // 已核准帳號：軟刪除（設 rejected），防止用戶重整後自動重建 pending
  try {
    await updateDoc(doc(db, "users", uid), { status: "rejected" });
  } catch (e) {
    if (msgEl) {
      msgEl.textContent = "移除失敗：" + (e.message || JSON.stringify(e));
      msgEl.className = "admin-msg error";
      msgEl.classList.remove("hidden");
    }
  }
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

  const sentCount    = (poll.sentRecipientIds || []).length;
  const responseRate = sentCount > 0 ? ((total / sentCount) * 100).toFixed(1) + "%" : "—";

  const rows = [
    ["投票問題", poll.question],
    ["總票數", total],
    ["發出人數", sentCount || "—"],
    ["回覆率", responseRate],
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

// ── Vote details (admin only) ─────────────────────────────────
async function showVoteDetails(poll) {
  document.getElementById("_vote_details")?.remove();
  const el = document.createElement("div");
  el.id = "_vote_details";
  el.className = "admin-overlay";
  el.innerHTML = `
    <div class="admin-drawer">
      <div class="admin-drawer-header">
        <h2>📋 投票明細</h2>
        <button class="admin-close-btn">✕</button>
      </div>
      <p style="font-size:.9rem;color:#555;margin:0 0 12px">${esc(poll.question)}</p>
      <div id="_vd_body"><div class="spinner" style="margin:32px auto"></div></div>
    </div>`;
  document.body.appendChild(el);
  el.querySelector(".admin-close-btn").addEventListener("click", () => el.remove());
  el.addEventListener("click", e => { if (e.target === el) el.remove(); });

  try {
    const [votesSnap, recipientsSnap] = await Promise.all([
      getDocs(collection(db, "polls", poll.id, "votes")),
      getDocs(collection(db, "recipients")),
    ]);

    const recipientMap = new Map();
    recipientsSnap.forEach(d => recipientMap.set(d.id, d.data()));

    const rawOpts = poll.options || [poll.optionA, poll.optionB, poll.optionC].filter(Boolean);

    const rows = votesSnap.docs
      .map(d => ({ ...d.data(), _id: d.id }))
      .sort((a, b) => (a.timestamp || 0) - (b.timestamp || 0));

    const body = el.querySelector("#_vd_body");
    if (rows.length === 0) {
      body.innerHTML = `<p class="admin-empty">尚無投票記錄</p>`;
      return;
    }

    const tableRows = rows.map(v => {
      const rec    = recipientMap.get(v.recipientId || "");
      const name   = rec ? esc(rec.name || "（未命名）") : `<span style="color:#999">未知</span>`;
      const phone  = rec ? maskPhone(rec.phoneNumber || "") : `<span style="color:#999">—</span>`;
      const choices = (v.choices || (v.choice ? [v.choice] : [])).map(c => {
        const text = rawOpts[c.charCodeAt(0) - 65] || "";
        return `<span class="choice-badge">${esc(c)}${text ? `&nbsp;${esc(text)}` : ""}</span>`;
      }).join(" ");
      const time = v.timestamp
        ? new Date(v.timestamp).toLocaleString("zh-TW", { month: "numeric", day: "numeric", hour: "2-digit", minute: "2-digit" })
        : "—";
      return `<tr><td>${name}</td><td>${phone}</td><td>${choices}</td><td style="color:#999;font-size:.8rem">${time}</td></tr>`;
    }).join("");

    // Unreplied section
    const sentIds    = new Set(poll.sentRecipientIds || []);
    const repliedIds = new Set(rows.filter(v => v.recipientId).map(v => v.recipientId));
    const unrepliedIds = [...sentIds].filter(id => !repliedIds.has(id));
    const unrepliedRows = unrepliedIds.map(id => {
      const rec   = recipientMap.get(id);
      const name  = rec ? esc(rec.name || "（未命名）") : `<span style="color:#999">未知</span>`;
      const phone = rec ? maskPhone(rec.phoneNumber || "") : `<span style="color:#999">—</span>`;
      return `<tr><td>${name}</td><td>${phone}</td><td><span style="color:#aaa;font-size:.8rem">未回覆</span></td><td style="color:#aaa;font-size:.8rem">—</td></tr>`;
    }).join("");

    const rateText = sentIds.size > 0
      ? `發出 ${sentIds.size} 人　·　回覆 ${rows.length} 票　·　回覆率 <strong>${Math.round(rows.length / sentIds.size * 100)}%</strong>`
      : `共 <strong>${rows.length}</strong> 票`;

    body.innerHTML = `
      <p style="font-size:.85rem;color:#666;margin-bottom:10px">${rateText}</p>
      <div style="overflow-x:auto">
        <table class="details-table">
          <thead><tr><th>姓名</th><th>電話</th><th>選擇</th><th>時間</th></tr></thead>
          <tbody>${tableRows}${unrepliedRows}</tbody>
        </table>
      </div>`;
  } catch (e) {
    el.querySelector("#_vd_body").innerHTML =
      `<p class="admin-msg error">讀取失敗：${esc(e.message)}</p>`;
  }
}

function maskPhone(phone) {
  if (!phone || phone.length < 6) return esc(phone);
  return esc(phone.slice(0, 4) + "***" + phone.slice(-3));
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
  if (pollsData.length === 0) {
    pollsContainer.innerHTML = "";
    noPollsEl.classList.remove("hidden");
    return;
  }
  noPollsEl.classList.add("hidden");

  const newIds = new Set(pollsData.map(p => p.id));

  // Remove cards for polls that no longer exist
  pollsContainer.querySelectorAll(".poll-card").forEach(card => {
    if (!newIds.has(card.dataset.pollId)) {
      const id = card.dataset.pollId;
      if (chartInstances.has(id)) { chartInstances.get(id).destroy(); chartInstances.delete(id); }
      card.remove();
    }
  });

  // Update existing cards or build new ones in order
  pollsData.forEach((poll, index) => {
    const existing = pollsContainer.querySelector(`[data-poll-id="${poll.id}"]`);
    if (existing) {
      updatePollCard(existing, poll);
    } else {
      const cards = pollsContainer.querySelectorAll(".poll-card");
      const newCard = buildPollCard(poll);
      if (index < cards.length) pollsContainer.insertBefore(newCard, cards[index]);
      else pollsContainer.appendChild(newCard);
    }
  });

  // Animate totals and update charts without destroying them
  pollsData.forEach(poll => {
    const counts = voteCounts.get(poll.id) || {};
    const total  = Object.values(counts).reduce((s, v) => s + v, 0);
    animateTo(poll.id, total);
    if (chartTypes.get(poll.id) === "pie") updatePieChart(poll.id, poll);
  });
}

function updatePollCard(card, poll) {
  const counts    = voteCounts.get(poll.id) || {};
  const rawOpts   = poll.options || [poll.optionA, poll.optionB, poll.optionC].filter(Boolean);
  const total     = rawOpts.reduce((s, _, i) => s + (counts[String.fromCharCode(65 + i)] || 0), 0);
  const isExpired = poll.endTime && Date.now() > poll.endTime;

  card.className = "poll-card" + (isExpired ? " poll-expired" : "");

  const badge = card.querySelector(".badge-active, .badge-expired");
  if (badge) {
    badge.textContent = isExpired ? "已截止" : "進行中";
    badge.className = `badge ${isExpired ? "badge-expired" : "badge-active"}`;
  }

  const toggleBtn = card.querySelector(".btn-toggle-poll");
  if (toggleBtn) {
    toggleBtn.dataset.expired = String(isExpired);
    toggleBtn.textContent = isExpired ? "🔓 重新開啟" : "🔒 截止投票";
  }

  card.querySelectorAll(".bar-row").forEach((row, i) => {
    const label = String.fromCharCode(65 + i);
    const count = counts[label] || 0;
    const pct   = total > 0 ? Math.round((count / total) * 100) : 0;
    const fill  = row.querySelector(".bar-fill");
    const cnt   = row.querySelector(".bar-count");
    if (fill) fill.style.width = pct + "%";
    if (cnt)  cnt.innerHTML = `${count}<br><small>${pct}%</small>`;
  });

  const sentCount = (poll.sentRecipientIds || []).length;
  const rateEl = card.querySelector(`#rate-line-${poll.id}`);
  if (rateEl) {
    if (sentCount > 0) {
      const pct = Math.round(total / sentCount * 100);
      rateEl.innerHTML = `發出 ${sentCount} 人 &nbsp;·&nbsp; 回覆率 <strong>${pct}%</strong>`;
      rateEl.style.display = "";
    } else {
      rateEl.style.display = "none";
    }
  }
}

function updatePieChart(pollId, poll) {
  const chart = chartInstances.get(pollId);
  if (!chart) { createPieChart(pollId, poll); return; }
  const counts  = voteCounts.get(pollId) || {};
  const rawOpts = poll.options || [poll.optionA, poll.optionB, poll.optionC].filter(Boolean);
  const total   = rawOpts.reduce((s, _, i) => s + (counts[String.fromCharCode(65 + i)] || 0), 0);
  chart.data.datasets[0].data = rawOpts.map((_, i) => counts[String.fromCharCode(65 + i)] || 0);
  chart.options.plugins.tooltip.callbacks.label = ctx => {
    const pct = total > 0 ? Math.round(ctx.parsed / total * 100) : 0;
    return `  ${ctx.parsed} 票（${pct}%）`;
  };
  chart.update("active");
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
  const sentCount  = (poll.sentRecipientIds || []).length;
  const type       = chartTypes.get(poll.id) || "bar";
  const initTotal  = prevTotals.has(poll.id) ? prevTotals.get(poll.id) : 0;

  const endTimeStr = poll.endTime
    ? new Date(poll.endTime).toLocaleString("zh-TW", { month: "numeric", day: "numeric", hour: "2-digit", minute: "2-digit" })
    : null;

  const card = document.createElement("div");
  card.className = "poll-card" + (isExpired ? " poll-expired" : "");
  card.dataset.pollId = poll.id;

  card.innerHTML = `
    <div class="poll-card-accent"></div>
    <div class="poll-card-inner">
    <div class="poll-header">
      <h2>${esc(poll.question)}</h2>
      ${poll.category ? `<span class="badge badge-category">${esc(poll.category)}</span>` : ""}
      <span class="badge ${isExpired ? "badge-expired" : "badge-active"}">${isExpired ? "已截止" : "進行中"}</span>
    </div>
    <p class="poll-meta">
      總票數：<strong id="total-${poll.id}">${initTotal}</strong>
      ${endTimeStr ? ` &nbsp;·&nbsp; 截止：${endTimeStr}` : ""}
      ${maxChoices > 1 ? ` &nbsp;·&nbsp; 最多選 ${maxChoices} 個` : ""}
    </p>
    <p class="poll-rate" id="rate-line-${poll.id}" ${sentCount === 0 ? 'style="display:none"' : ""}>
      發出 ${sentCount} 人 &nbsp;·&nbsp; 回覆率 <strong>${sentCount > 0 ? Math.round(initTotal / sentCount * 100) : 0}%</strong>
    </p>

    <div class="poll-actions">
      ${isAdmin() ? `
        <button class="action-btn btn-toggle-poll" data-poll-id="${poll.id}" data-expired="${isExpired}">
          ${isExpired ? "🔓 重新開啟" : "🔒 截止投票"}
        </button>` : ""}
      <button class="action-btn btn-export" data-poll-id="${poll.id}">⬇ 匯出 CSV</button>
      <button class="action-btn btn-ai" data-poll-id="${poll.id}">🤖 AI 分析</button>
      ${isAdmin() ? `
        <button class="action-btn btn-details" data-poll-id="${poll.id}">📋 投票明細</button>
        <button class="action-btn btn-sim" data-poll-id="${poll.id}">🧪 攻擊模擬</button>
        <button class="action-btn btn-delete" data-poll-id="${poll.id}">🗑 刪除</button>` : ""}
    </div>

    <div id="ai-result-${poll.id}" class="ai-result hidden"></div>
    <div id="sim-result-${poll.id}" class="sim-result hidden"></div>

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
    </div>
  `;

  // Action buttons
  card.querySelector(".btn-toggle-poll")?.addEventListener("click", async (e) => {
    const btn = e.currentTarget;
    btn.disabled = true;
    try {
      if (btn.dataset.expired === "true") await reopenPoll(poll.id);
      else await closePoll(poll.id);
    } finally { btn.disabled = false; }
  });

  card.querySelector(".btn-export").addEventListener("click", () => exportCsv(poll));

  card.querySelector(".btn-ai").addEventListener("click", () => runAiAnalysis(poll, card));

  card.querySelector(".btn-sim")?.addEventListener("click", () => runFraudSimulation(poll, card));

  card.querySelector(".btn-details")?.addEventListener("click", () => showVoteDetails(poll));

  card.querySelector(".btn-delete")?.addEventListener("click", () =>
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

async function runFraudSimulation(poll, card) {
  const resultEl = card.querySelector(`#sim-result-${poll.id}`);
  const btn      = card.querySelector(".btn-sim");
  btn.disabled = true;

  resultEl.innerHTML = `<div class="sim-running">⏳ 模擬攻擊中，請稍候…</div>`;
  resultEl.classList.remove("hidden");

  const prefix = `_sim_${Date.now()}`;
  const results = [];

  // ── Test 1: 重複投票（同 voterHash 寫兩次）────────────────────
  const dupHash = prefix + "_dup";
  const dupRef  = doc(db, "polls", poll.id, "votes", dupHash);
  const vote1   = { pollId: poll.id, voterHash: dupHash, choices: ["A"], timestamp: Date.now(), _simTest: true };
  const vote2   = { pollId: poll.id, voterHash: dupHash, choices: ["B"], timestamp: Date.now(), _simTest: true };
  try {
    await setDoc(dupRef, vote1);
    try {
      await setDoc(dupRef, vote2);
      results.push({ label: "重複投票（同一 hash）", ok: false, msg: "⚠️ 成功覆蓋同一票 — Firestore 規則需加 allow create only" });
    } catch {
      results.push({ label: "重複投票（同一 hash）", ok: true, msg: "✅ 第二次寫入被 Firestore 規則擋住" });
    }
    await deleteDoc(dupRef).catch(() => {});
  } catch {
    results.push({ label: "重複投票（同一 hash）", ok: true, msg: "✅ Firestore 規則不允許從 Web 直接寫票" });
  }

  // ── Test 2: 截止後強寫票 ──────────────────────────────────────
  if (poll.endTime && Date.now() > poll.endTime) {
    const expHash = prefix + "_expired";
    const expRef  = doc(db, "polls", poll.id, "votes", expHash);
    try {
      await setDoc(expRef, { pollId: poll.id, voterHash: expHash, choices: ["A"], timestamp: Date.now(), _simTest: true });
      results.push({ label: "截止後強寫票", ok: false, msg: "⚠️ 成功寫入已截止投票 — Firestore 規則未驗證 endTime" });
      await deleteDoc(expRef).catch(() => {});
    } catch {
      results.push({ label: "截止後強寫票", ok: true, msg: "✅ Firestore 規則擋住截止後投票" });
    }
  } else {
    results.push({ label: "截止後強寫票", ok: null, msg: "⏭ 此投票尚未截止，跳過" });
  }

  // ── Test 3: 大量灌票（5 筆不同 hash）─────────────────────────
  let spamCount = 0;
  const spamRefs = [];
  for (let i = 0; i < 5; i++) {
    const h = prefix + `_spam${i}`;
    const r = doc(db, "polls", poll.id, "votes", h);
    try {
      await setDoc(r, { pollId: poll.id, voterHash: h, choices: ["A"], timestamp: Date.now(), _simTest: true });
      spamCount++;
      spamRefs.push(r);
    } catch { break; }
  }
  for (const r of spamRefs) await deleteDoc(r).catch(() => {});
  if (spamCount === 0) {
    results.push({ label: "偽造灌票 × 5", ok: true, msg: "✅ Firestore 規則擋住所有偽造票" });
  } else {
    results.push({ label: "偽造灌票 × 5", ok: false, msg: `⚠️ 成功寫入 ${spamCount}/5 筆假票（已清除）— 建議加 Firestore 寫入條件` });
  }

  // ── Render ────────────────────────────────────────────────────
  const rows = results.map(r => {
    const cls = r.ok === true ? "sim-ok" : r.ok === false ? "sim-fail" : "sim-skip";
    return `<div class="sim-row ${cls}">
      <span class="sim-label">${r.label}</span>
      <span class="sim-msg">${r.msg}</span>
    </div>`;
  }).join("");
  resultEl.innerHTML = `<strong class="sim-title">🧪 攻擊模擬結果</strong>${rows}
    <p class="sim-note">所有測試資料已從 Firestore 清除。SMS 層防護由 Android SmsReceiver 處理，不在此測試範圍。</p>`;

  btn.disabled = false;
}

async function runAiAnalysis(poll, card) {
  const resultEl  = card.querySelector(`#ai-result-${poll.id}`);
  const btn       = card.querySelector(".btn-ai");
  const provider  = window._aiProvider || localStorage.getItem("ai_provider") || "gemini";
  const apiKey    = window._aiApiKey   || localStorage.getItem("ai_key_" + provider) || window.GEMINI_API_KEY;

  if (!apiKey || apiKey === "YOUR_GEMINI_API_KEY") {
    resultEl.textContent = "⚠️ 請在「帳號管理 → ⚙️ 設定」輸入 AI API Key";
    resultEl.classList.remove("hidden");
    return;
  }

  btn.disabled = true;
  resultEl.innerHTML = `<span class="ai-loading">⏳ AI 分析中（${provider}）…</span>`;
  resultEl.classList.remove("hidden");

  const counts  = voteCounts.get(poll.id) || {};
  const rawOpts = poll.options || [poll.optionA, poll.optionB, poll.optionC].filter(Boolean);
  const total   = rawOpts.reduce((s, _, i) => s + (counts[String.fromCharCode(65 + i)] || 0), 0);
  const optLines = rawOpts.map((text, i) => {
    const key = String.fromCharCode(65 + i);
    return `${key}. ${text}：${counts[key] || 0} 票`;
  }).join("\n");

  const prompt = [
    "你是一個投票結果分析師，請用繁體中文分析以下投票結果，回覆在 200 字以內，語氣簡潔專業：",
    "",
    `投票主題：${poll.question}`,
    ...(poll.maxChoices > 1 ? [`投票方式：複選（最多選 ${poll.maxChoices} 個）`] : []),
    `選項與得票（共 ${total} 票）：`,
    optLines,
    "",
    "請依序提供：① 結果摘要 ② 主要趨勢 ③ 一句建議",
  ].join("\n");

  try {
    let text;
    if (provider === "openai") {
      const res = await fetch("https://api.openai.com/v1/chat/completions", {
        method: "POST",
        headers: { "Content-Type": "application/json", "Authorization": "Bearer " + apiKey },
        body: JSON.stringify({ model: "gpt-4o-mini", messages: [{ role: "user", content: prompt }], max_tokens: 600 }),
      });
      if (!res.ok) { const e = await res.json().catch(() => ({})); throw new Error(`HTTP ${res.status}: ${e.error?.message || ""}`); }
      const data = await res.json();
      text = data.choices?.[0]?.message?.content ?? "無法生成分析";
    } else if (provider === "claude") {
      const res = await fetch("https://api.anthropic.com/v1/messages", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "x-api-key": apiKey,
          "anthropic-version": "2023-06-01",
          "anthropic-dangerous-direct-browser-access": "true",
        },
        body: JSON.stringify({ model: "claude-haiku-4-5-20251001", max_tokens: 600, messages: [{ role: "user", content: prompt }] }),
      });
      if (!res.ok) { const e = await res.json().catch(() => ({})); throw new Error(`HTTP ${res.status}: ${e.error?.message || ""}`); }
      const data = await res.json();
      text = data.content?.[0]?.text ?? "無法生成分析";
    } else {
      const res = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey}`,
        { method: "POST", headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ contents: [{ parts: [{ text: prompt }] }] }) }
      );
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      text = data.candidates?.[0]?.content?.parts?.[0]?.text ?? "無法生成分析";
    }
    resultEl.innerHTML = `<strong>🤖 AI 分析</strong><p>${esc(text).replace(/\n/g, "<br>")}</p>`;
  } catch (e) {
    resultEl.innerHTML = `<span class="ai-error">分析失敗：${esc(e.message)}</span>`;
  } finally {
    btn.disabled = false;
  }
}

function helpStep(num, emoji, title, where, desc) {
  return `<div class="help-step">
    <div class="help-step-num">${num}</div>
    <div class="help-step-content">
      <strong>${emoji} ${title}</strong>
      <span class="help-where">${where}</span>
      <p>${desc}</p>
    </div>
  </div>`;
}

function openHelpModal(autoShow = false) {
  if (document.getElementById("_help_modal")) return;
  if (autoShow && sessionStorage.getItem("help_shown") === "1") return;

  const el = document.createElement("div");
  el.id = "_help_modal";
  el.className = "dialog-overlay help-overlay";
  el.innerHTML = `
    <div class="help-modal">
      <div class="help-header">
        <h2>📖 操作說明</h2>
        <button class="admin-close-btn" id="help-close">✕</button>
      </div>
      <div class="help-body">
        ${helpStep(1,"🗳️","建立投票","App 投票頁 → 右下角「＋ 建立投票」","輸入題目與選項（A/B/C…），可設截止時間與複選上限。每個選項以換行分隔，系統自動編號。")}
        ${helpStep(2,"📋","新增投票對象","App 底部 → 聯絡人 → 新增","新增受訪者手機號碼（+886 格式）建立聯絡人名冊，支援批次管理。")}
        ${helpStep(3,"📱","發送 SMS 通知","App 投票卡片 → 「發送通知」","選擇聯絡人後確認，系統透過裝置 SIM 卡發送含題目、選項代碼及回覆說明的 SMS。")}
        ${helpStep(4,"💬","受訪者回覆 SMS","受訪者自行回覆即可","受訪者回傳「A」或「B,C」（複選）到同一號碼，系統自動解析計票。重複回覆以最後一次為準，完全匿名。")}
        ${helpStep(5,"📊","即時查看結果","本網頁 或 App 底部 → 結果","即時票數與圖表，可切換長條圖 / 圓餅圖，支援匯出 PDF 完整報告。")}
        ${helpStep(6,"🤖","AI 智能分析","結果卡片 → 「🤖 AI 分析」按鈕","AI 自動生成摘要、趨勢與建議。需先在「帳號管理 → ⚙️ 設定」選擇服務（Gemini / GPT / Claude）並輸入 API Key。")}
        ${helpStep(7,"👥","帳號管理","右上角 → 「👥 帳號管理」","可審核待批帳號、預先授權 Email、查看所有帳號、移除帳號、設定 AI Provider。")}
      </div>
      <div class="help-footer">
        <span class="help-tip">下次可點右上角「📖 說明」重新查看</span>
        <button class="help-close-btn" id="help-close-btn">知道了 ✓</button>
      </div>
    </div>`;
  document.body.appendChild(el);

  const close = () => {
    sessionStorage.setItem("help_shown", "1");
    el.remove();
  };
  el.querySelector("#help-close").addEventListener("click", close);
  el.querySelector("#help-close-btn").addEventListener("click", close);
  el.addEventListener("click", e => { if (e.target === el) close(); });
}
