const baseUrlInput = document.getElementById("baseUrl");
const usernameInput = document.getElementById("username");
const passwordInput = document.getElementById("password");
const sourceEl = document.getElementById("source");
const resultEl = document.getElementById("result");
const statusEl = document.getElementById("status");
const translateBtn = document.getElementById("translateBtn");
const readAloudBtn = document.getElementById("readAloudBtn");
const listenGoogleBtn = document.getElementById("listenGoogleBtn");

const DEFAULT_BASE = "http://localhost:8080/translator-service/api";

function setStatus(msg, isError = false) {
  statusEl.textContent = msg;
  statusEl.classList.toggle("error", isError);
}

async function loadSettings() {
  const { apiBaseUrl, apiUser, apiPassword } = await chrome.storage.sync.get({
    apiBaseUrl: DEFAULT_BASE,
    apiUser: "translator",
    apiPassword: "changeme",
  });
  baseUrlInput.value = apiBaseUrl;
  usernameInput.value = apiUser;
  passwordInput.value = apiPassword;
}

document.getElementById("saveConfig").addEventListener("click", async () => {
  await chrome.storage.sync.set({
    apiBaseUrl: baseUrlInput.value.trim() || DEFAULT_BASE,
    apiUser: usernameInput.value,
    apiPassword: passwordInput.value,
  });
  setStatus("Settings saved.");
});

function basicHeader(user, pass) {
  const token = btoa(`${user}:${pass}`);
  return `Basic ${token}`;
}

async function translate() {
  const { apiBaseUrl, apiUser, apiPassword } = await chrome.storage.sync.get({
    apiBaseUrl: DEFAULT_BASE,
    apiUser: "DarijaTranslator",
    apiPassword: "Morocco",
  });
  const text = sourceEl.value.trim();
  if (!text) {
    setStatus("Enter text to translate.", true);
    return;
  }
  const url = `${apiBaseUrl.replace(/\/$/, "")}/translator/translate`;
  translateBtn.disabled = true;
  setStatus("Translating...");
  resultEl.value = "";
  try {
    const res = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: basicHeader(apiUser, apiPassword),
      },
      body: JSON.stringify({ text }),
    });
    const body = await res.json().catch(() => ({}));
    if (!res.ok) {
      throw new Error(body.error || res.statusText || String(res.status));
    }
    resultEl.value = body.translatedText || "";
    setStatus("Done.");
  } catch (e) {
    setStatus(e.message || String(e), true);
  } finally {
    translateBtn.disabled = false;
  }
}

translateBtn.addEventListener("click", translate);

function textHasArabicScript(s) {
  return /[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]/.test(s);
}

function pickArabicVoice(voices) {
  if (!voices?.length) return null;
  const prefer = (pred) => voices.find(pred);
  return (
    prefer((v) => /^ar-(MA|SA|EG|DZ)/i.test(v.lang)) ||
    prefer((v) => /^ar[-_]/i.test(v.lang || "")) ||
    prefer((v) => String(v.lang || "").toLowerCase().startsWith("ar")) ||
    prefer((v) => /arabic|عربي|hoda|naayf|lamia|kalima|microsoft.*ar/i.test(String(v.name) + String(v.lang)))
  );
}

function speakLatinWebSpeech(text) {
  window.speechSynthesis.cancel();
  const u = new SpeechSynthesisUtterance(text);
  u.lang = "en-US";
  u.onend = () => setStatus("Done reading.");
  u.onerror = () => setStatus("Read aloud failed.", true);
  window.speechSynthesis.speak(u);
  setStatus("Reading aloud...");
}

function speakArabicWebSpeech(text) {
  window.speechSynthesis.cancel();
  let ran = false;
  const run = () => {
    if (ran) return;
    const voices = window.speechSynthesis.getVoices();
    const arVoice = pickArabicVoice(voices);
    if (!arVoice) {
      ran = true;
      setStatus("No Arabic voice on this PC - use Listen (Google) or install an Arabic speech pack.", true);
      return;
    }
    ran = true;
    const u = new SpeechSynthesisUtterance(text);
    u.voice = arVoice;
    u.lang = arVoice.lang;
    u.rate = 0.95;
    u.onerror = () => setStatus("Read aloud failed (Web Speech).", true);
    u.onend = () => setStatus("Done reading.");
    window.speechSynthesis.speak(u);
    setStatus("Reading aloud...");
  };

  if (window.speechSynthesis.getVoices().length > 0) {
    run();
    return;
  }
  window.speechSynthesis.addEventListener("voiceschanged", run, { once: true });
  window.speechSynthesis.getVoices();
  setTimeout(() => {
    if (!ran) run();
  }, 500);
}

function speakDarija(text) {
  window.speechSynthesis.cancel();
  if (typeof chrome !== "undefined" && chrome.tts && typeof chrome.tts.speak === "function") {
    try {
      chrome.tts.stop(() => {});
    } catch (_) {}
  }

  if (!textHasArabicScript(text)) {
    if (typeof chrome !== "undefined" && chrome.tts) {
      chrome.tts.speak(text, { lang: "en-US", enqueue: false, rate: 1 }, () => {
        if (chrome.runtime.lastError) {
          speakLatinWebSpeech(text);
        } else {
          setStatus("Done reading.");
        }
      });
      setStatus("Reading aloud...");
      return;
    }
    speakLatinWebSpeech(text);
    return;
  }

  if (typeof chrome !== "undefined" && chrome.tts && typeof chrome.tts.getVoices === "function") {
    chrome.tts.getVoices((voices) => {
      const ar = (voices || []).find((v) => v.lang && /^ar/i.test(v.lang));
      const opts = { lang: ar?.lang || "ar-SA", enqueue: false, rate: 0.95 };
      if (ar?.voiceName) opts.voiceName = ar.voiceName;
      chrome.tts.speak(text, opts, () => {
        if (chrome.runtime.lastError) {
          speakArabicWebSpeech(text);
          return;
        }
        setStatus("Done reading.");
      });
      setStatus("Reading aloud...");
    });
    return;
  }

  speakArabicWebSpeech(text);
}

readAloudBtn.addEventListener("click", () => {
  const text = resultEl.value.trim();
  if (!text) {
    setStatus("Nothing to read.", true);
    return;
  }
  speakDarija(text);
});

listenGoogleBtn.addEventListener("click", () => {
  const text = resultEl.value.trim();
  if (!text) {
    setStatus("Nothing to play.", true);
    return;
  }
  const q = encodeURIComponent(text);
  const url = `https://translate.google.com/?sl=ar&tl=en&q=${q}&op=translate`;
  window.open(url, "_blank", "noopener,noreferrer");
  setStatus("Google Translate opened - click the speaker icon under Arabic text.");
});

let lastHandledDispatch = 0;

async function applyPendingText(text, dispatchId) {
  const trimmed = text?.trim();
  if (!trimmed) return;
  if (dispatchId != null && dispatchId === lastHandledDispatch) {
    await chrome.storage.session.remove(["pendingTranslationText", "pendingDispatchId"]);
    return;
  }
  lastHandledDispatch = dispatchId ?? Date.now();
  sourceEl.value = trimmed;
  await chrome.storage.session.remove(["pendingTranslationText", "pendingDispatchId"]);
  await translate();
}

async function consumePendingFromSession() {
  const data = await chrome.storage.session.get(["pendingTranslationText", "pendingDispatchId"]);
  const pending = data.pendingTranslationText?.trim();
  if (!pending) return;
  await applyPendingText(pending, data.pendingDispatchId);
}

chrome.runtime.onMessage.addListener((msg) => {
  if (msg?.type === "PENDING_TRANSLATION" && msg.text?.trim()) {
    void applyPendingText(msg.text, msg.dispatch);
  }
});

chrome.storage.session.onChanged.addListener((changes, areaName) => {
  if (areaName !== "session" || !changes.pendingDispatchId) return;
  void consumePendingFromSession();
});

(async function tryInitialPending() {
  for (let i = 0; i < 40; i++) {
    const data = await chrome.storage.session.get(["pendingTranslationText", "pendingDispatchId"]);
    if (data.pendingTranslationText?.trim()) {
      await consumePendingFromSession();
      return;
    }
    await new Promise((r) => setTimeout(r, 50));
  }
})();

loadSettings();
