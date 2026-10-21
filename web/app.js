import { initializeApp } from "https://www.gstatic.com/firebasejs/10.14.0/firebase-app.js";
import {
  getAuth, GoogleAuthProvider, signInWithPopup, signOut, onAuthStateChanged,
} from "https://www.gstatic.com/firebasejs/10.14.0/firebase-auth.js";
import {
  getFirestore, collection, query, orderBy, onSnapshot,
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

const loginSection   = document.getElementById("login-section");
const resultsSection = document.getElementById("results-section");
const loginBtn       = document.getElementById("login-btn");
const logoutBtn      = document.getElementById("logout-btn");
const loginError     = document.getElementById("login-error");
const userNameEl     = document.getElementById("user-name");
const pollsContainer = document.getElementById("polls-container");
const noPollsEl      = document.getElementById("no-polls");

let pollsData = [];
const voteCounts  = new Map();   // pollId -> { A: n, B: n, ... }
const voteListeners = new Map(); // pollId -> unsubscribe
let unsubscribePolls = null;

// ── Auth ──────────────────────────────────────────────────────
onAuthStateChanged(auth, (user) => {
  if (user) { showResults(user); } else { showLogin(); }
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

function showLogin() {
  cleanup();
  loginSection.classList.remove("hidden");
  resultsSection.classList.add("hidden");
}

function showResults(user) {
  loginSection.classList.add("hidden");
  resultsSection.classList.remove("hidden");
  userNameEl.textContent = user.displayName || user.email;
  subscribeToPolls();
}

function cleanup() {
  if (unsubscribePolls) { unsubscribePolls(); unsubscribePolls = null; }
  voteListeners.forEach(unsub => unsub());
  voteListeners.clear();
  voteCounts.clear();
  pollsData = [];
}

// ── Firestore ─────────────────────────────────────────────────
function subscribeToPolls() {
  const q = query(collection(db, "polls"), orderBy("createdAt", "desc"));
  unsubscribePolls = onSnapshot(q, (snapshot) => {
    const currentIds = new Set(snapshot.docs.map(d => d.id));
    voteListeners.forEach((unsub, id) => {
      if (!currentIds.has(id)) { unsub(); voteListeners.delete(id); voteCounts.delete(id); }
    });
    pollsData = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    pollsData.forEach(poll => {
      if (!voteListeners.has(poll.id)) subscribeToVotes(poll.id);
    });
    renderAll();
  });
}

function subscribeToVotes(pollId) {
  const unsub = onSnapshot(collection(db, "polls", pollId, "votes"), (snapshot) => {
    const counts = {};
    snapshot.forEach(doc => {
      const data = doc.data();
      // Support both new `choices` array and legacy `choice` string
      const choices = data.choices || (data.choice ? [data.choice] : []);
      choices.forEach(c => { counts[c] = (counts[c] || 0) + 1; });
    });
    voteCounts.set(pollId, counts);
    renderAll();
  });
  voteListeners.set(pollId, unsub);
}

// ── Render ────────────────────────────────────────────────────
function renderAll() {
  pollsContainer.innerHTML = "";
  if (pollsData.length === 0) { noPollsEl.classList.remove("hidden"); return; }
  noPollsEl.classList.add("hidden");
  pollsData.forEach(poll => pollsContainer.appendChild(buildPollCard(poll)));
}

function buildPollCard(poll) {
  const counts = voteCounts.get(poll.id) || {};
  const rawOptions = poll.options || [poll.optionA, poll.optionB, poll.optionC].filter(Boolean);
  const options = rawOptions.map((text, i) => {
    const label = String.fromCharCode(65 + i);
    return { label, text, count: counts[label] || 0 };
  });
  const total = options.reduce((sum, o) => sum + o.count, 0);
  const isExpired = poll.endTime && Date.now() > poll.endTime;
  const maxChoices = poll.maxChoices || 1;

  const endTimeStr = poll.endTime
    ? new Date(poll.endTime).toLocaleString("zh-TW", { month: "numeric", day: "numeric", hour: "2-digit", minute: "2-digit" })
    : null;

  const card = document.createElement("div");
  card.className = "poll-card" + (isExpired ? " poll-expired" : "");

  card.innerHTML = `
    <div class="poll-header">
      <h2>${esc(poll.question)}</h2>
      <span class="badge ${isExpired ? "badge-expired" : "badge-active"}">${isExpired ? "已截止" : "進行中"}</span>
    </div>
    <p class="poll-meta">
      總票數：<strong>${total}</strong>
      ${endTimeStr ? ` &nbsp;·&nbsp; 截止：${endTimeStr}` : ""}
      ${maxChoices > 1 ? ` &nbsp;·&nbsp; 最多選 ${maxChoices} 個` : ""}
    </p>
    ${options.map(({ label, text, count }) => {
      const pct = total > 0 ? Math.round((count / total) * 100) : 0;
      return `
        <div class="bar-row">
          <span class="bar-label">${label}</span>
          <div class="bar-col">
            <span class="bar-option-text">${esc(text)}</span>
            <div class="bar-track">
              <div class="bar-fill" style="width:${pct}%"></div>
            </div>
          </div>
          <span class="bar-count">${count}<br><small>${pct}%</small></span>
        </div>`;
    }).join("")}
  `;
  return card;
}

const esc = s => String(s).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;");
