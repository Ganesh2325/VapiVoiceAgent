"""
Patch a Vapi assistant's server URL using credentials from the environment.

Required environment variables:
  VAPI_API_KEY          Vapi private API key (never commit this)
  VAPI_ASSISTANT_ID     Assistant UUID to patch
  VAPI_WEBHOOK_SECRET   Shared secret for x-vapi-secret
  VAPI_SERVER_URL       Public webhook URL (e.g. https://host/api/webhooks/vapi)

This script does not print secret values.
"""
import json
import os
import sys
import urllib.error
import urllib.request

api_key = os.environ.get("VAPI_API_KEY", "").strip()
assistant_id = os.environ.get("VAPI_ASSISTANT_ID", "").strip()
webhook_secret = os.environ.get("VAPI_WEBHOOK_SECRET", "").strip()
server_url = os.environ.get("VAPI_SERVER_URL", "").strip()

missing = [name for name, value in (
    ("VAPI_API_KEY", api_key),
    ("VAPI_ASSISTANT_ID", assistant_id),
    ("VAPI_WEBHOOK_SECRET", webhook_secret),
    ("VAPI_SERVER_URL", server_url),
) if not value]

if missing:
    print("Missing required environment variables: " + ", ".join(missing), file=sys.stderr)
    sys.exit(1)

url = "https://api.vapi.ai/assistant/" + assistant_id
headers = {
    "Authorization": "Bearer " + api_key,
    "Content-Type": "application/json",
    "User-Agent": "VoiceOS-patch-vapi",
}

data = {
    "server": {
        "url": server_url,
        "timeoutSeconds": 20,
        "secret": webhook_secret,
    }
}

req = urllib.request.Request(url, data=json.dumps(data).encode("utf-8"), headers=headers, method="PATCH")

try:
    with urllib.request.urlopen(req) as response:
        print("Assistant server URL updated. HTTP", response.status)
except urllib.error.URLError as e:
    print("Error updating Vapi assistant:", e, file=sys.stderr)
    if hasattr(e, "read"):
        print(e.read().decode(), file=sys.stderr)
    sys.exit(1)
