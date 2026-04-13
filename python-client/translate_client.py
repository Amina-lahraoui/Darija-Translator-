#!/usr/bin/env python3
"""
Minimal Python client for the Darija Translator REST API.
"""

from __future__ import annotations

import json
import os
import sys

try:
    import requests
except ImportError:
    print("Install requests: pip install requests", file=sys.stderr)
    sys.exit(1)


def main() -> None:
    text = sys.argv[1] if len(sys.argv) > 1 else "Hello, welcome to Morocco."
    base = os.environ.get("TRANSLATOR_API_BASE", "http://localhost:8080/translator-service/api").rstrip("/")
    user = os.environ.get("TRANSLATOR_USER", "DarijaTranslator")
    password = os.environ.get("TRANSLATOR_PASSWORD", "Morocco")
    url = f"{base}/translator/translate"
    r = requests.post(
        url,
        json={"text": text},
        auth=(user, password),
        headers={"Content-Type": "application/json"},
        timeout=120,
    )
    print(f"HTTP {r.status_code}")
    try:
        data = r.json()
    except json.JSONDecodeError:
        print(r.text)
        sys.exit(1)
    if r.ok:
        print("Source:", data.get("sourceText"))
        print("Darija:", data.get("translatedText"))
    else:
        print(data)
        sys.exit(1)


if __name__ == "__main__":
    main()
