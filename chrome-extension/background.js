chrome.runtime.onInstalled.addListener(() => {
  void setupExtension();
});

void setupExtension();

async function setupExtension() {
  await chrome.contextMenus.removeAll();
  await chrome.contextMenus.create({
    id: "translate-darija",
    title: "Translate selection to Darija",
    contexts: ["selection"],
  });
  await chrome.sidePanel.setPanelBehavior({ openPanelOnActionClick: true });
}

chrome.contextMenus.onClicked.addListener((info, tab) => {
  void handleContextMenuClick(info, tab);
});

async function getSelectedText(info, tab) {
  let text = (info.selectionText || "").trim();
  if (text) return text;
  if (tab?.id == null) return "";

  try {
    const results = await chrome.scripting.executeScript({
      target: { tabId: tab.id, allFrames: true },
      func: () => window.getSelection()?.toString()?.trim() ?? "",
    });
    let best = "";
    for (const r of results || []) {
      const s = (r.result || "").trim();
      if (s.length > best.length) best = s;
    }
    return best;
  } catch {
    return "";
  }
}

async function handleContextMenuClick(info, tab) {
  if (info.menuItemId !== "translate-darija" || tab?.id == null) return;
  const text = await getSelectedText(info, tab);
  if (!text) return;

  const dispatch = Date.now();
  await chrome.storage.session.set({
    pendingTranslationText: text,
    pendingDispatchId: dispatch,
  });

  try {
    await chrome.sidePanel.open({ tabId: tab.id, windowId: tab.windowId });
  } catch (e) {
    console.warn("sidePanel.open failed", e);
  }

  for (let i = 0; i < 40; i++) {
    try {
      await chrome.runtime.sendMessage({ type: "PENDING_TRANSLATION", text, dispatch });
      return;
    } catch {
      await new Promise((r) => setTimeout(r, 50));
    }
  }
}
