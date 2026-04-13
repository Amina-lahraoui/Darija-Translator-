import { useState } from "react";
import {
  SafeAreaView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  ActivityIndicator,
  ScrollView,
  Platform,
} from "react-native";
import { StatusBar } from "expo-status-bar";
import { encode as base64Encode } from "base-64";

function defaultApiBase() {
  if (Platform.OS === "web") {
    return "http://127.0.0.1:8080/translator-service/api";
  }
  if (Platform.OS === "android") {
    return "http://10.0.2.2:8080/translator-service/api";
  }
  return "http://localhost:8080/translator-service/api";
}

function basicHeader(user, pass) {
  return `Basic ${base64Encode(`${user}:${pass}`)}`;
}

export default function App() {
  const [baseUrl, setBaseUrl] = useState(() => defaultApiBase());
  const [user, setUser] = useState("DarijaTranslator");
  const [password, setPassword] = useState("Morocco");
  const [source, setSource] = useState("Hello, how are you?");
  const [result, setResult] = useState("");
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(false);

  async function translate() {
    const text = source.trim();
    if (!text) {
      setStatus("Enter text to translate.");
      return;
    }
    const url = `${baseUrl.replace(/\/$/, "")}/translator/translate`;
    setLoading(true);
    setStatus("");
    setResult("");
    try {
      const res = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: basicHeader(user, password),
        },
        body: JSON.stringify({ text }),
      });
      const contentType = res.headers.get("content-type") || "";
      let body = {};
      if (contentType.includes("application/json")) {
        body = await res.json().catch(() => ({}));
      } else {
        const textBody = await res.text().catch(() => "");
        body = textBody ? { raw: textBody } : {};
      }
      if (!res.ok) {
        throw new Error(body.error || body.message || res.statusText || `HTTP ${res.status}`);
      }
      const translated =
        (typeof body.translatedText === "string" && body.translatedText) ||
        (typeof body.translation === "string" && body.translation) ||
        (typeof body.text === "string" && body.text) ||
        "";
      if (!translated.trim()) {
        throw new Error("Translation API returned success but no translation text.");
      }
      setResult(translated.trim());
      setStatus(`Done${body.model ? ` (model: ${body.model})` : ""}.`);
    } catch (e) {
      setStatus(e.message || String(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <SafeAreaView style={styles.safe}>
      <StatusBar style="dark" />
      <ScrollView contentContainerStyle={styles.scroll}>
        <Text style={styles.title}>Darija Translator</Text>
        <Text style={styles.hint}>
          Web/iOS: localhost. Android emulator: 10.0.2.2. Physical phone: your PC LAN IP.
        </Text>

        <Text style={styles.label}>API base URL</Text>
        <TextInput
          style={styles.input}
          value={baseUrl}
          onChangeText={setBaseUrl}
          autoCapitalize="none"
          autoCorrect={false}
        />

        <Text style={styles.label}>Username</Text>
        <TextInput style={styles.input} value={user} onChangeText={setUser} autoCapitalize="none" />

        <Text style={styles.label}>Password</Text>
        <TextInput style={styles.input} value={password} onChangeText={setPassword} secureTextEntry />

        <Text style={styles.label}>Source text</Text>
        <TextInput
          style={[styles.input, styles.multiline]}
          value={source}
          onChangeText={setSource}
          multiline
        />

        <TouchableOpacity style={styles.button} onPress={translate} disabled={loading}>
          {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.buttonText}>Translate</Text>}
        </TouchableOpacity>

        <Text style={styles.label}>Darija</Text>
        <TextInput style={[styles.input, styles.multiline]} value={result} multiline editable={false} />

        {!!status && <Text style={styles.status}>{status}</Text>}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: "#f6f7fb" },
  scroll: { padding: 16, paddingBottom: 32 },
  title: { fontSize: 22, fontWeight: "700", marginBottom: 8 },
  hint: { fontSize: 12, color: "#555", marginBottom: 16 },
  label: { fontSize: 13, fontWeight: "600", marginBottom: 4, marginTop: 8 },
  input: {
    borderWidth: 1,
    borderColor: "#ccc",
    borderRadius: 8,
    padding: 10,
    backgroundColor: "#fff",
    fontSize: 16,
  },
  multiline: { minHeight: 100, textAlignVertical: "top" },
  button: {
    backgroundColor: "#2563eb",
    padding: 14,
    borderRadius: 8,
    alignItems: "center",
    marginTop: 16,
  },
  buttonText: { color: "#fff", fontWeight: "700", fontSize: 16 },
  status: { marginTop: 12, fontSize: 14, color: "#333" },
});
