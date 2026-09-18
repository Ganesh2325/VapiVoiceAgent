"""
Patch a Vapi assistant's server URL and calculator tool using environment credentials.

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

def load_env_file(path):
    if not os.path.isfile(path):
        return
    with open(path, encoding="utf-8") as handle:
        for raw in handle:
            line = raw.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            key = key.strip()
            value = value.strip().strip('"').strip("'")
            os.environ.setdefault(key, value)


load_env_file(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".env"))

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
    "User-Agent": "Mozilla/5.0 VoiceOS-patch-vapi/1.0",
    "Accept": "application/json",
}

server_config = {
    "url": server_url,
    "timeoutSeconds": 20,
    "secret": webhook_secret,
}

calculator_tool = {
    "type": "function",
    "async": False,
    "server": server_config,
    "function": {
        "name": "calculator",
        "description": "Evaluate a mathematical expression. Always call this instead of computing the answer yourself.",
        "parameters": {
            "type": "object",
            "properties": {
                "expression": {
                    "type": "string",
                    "description": "Arithmetic expression such as 125 * 24",
                }
            },
            "required": ["expression"],
        },
    },
}

voiceos_request_tool = {
    "type": "function",
    "async": False,
    "server": server_config,
    "function": {
        "name": "voiceos_request",
        "description": "Route any non-arithmetic user request to VoiceOS agents. Pass the original utterance. Do not invent the answer.",
        "parameters": {
            "type": "object",
            "properties": {
                "utterance": {
                    "type": "string",
                    "description": "The user's original spoken or typed request, unchanged",
                }
            },
            "required": ["utterance"],
        },
    },
}

system_prompt = (
    "You are VoiceOS, a voice operations assistant. "
    "For arithmetic, ALWAYS call the calculator tool with argument expression. "
    "Never invent or guess the numeric result. "
    "For any other user request, ALWAYS call voiceos_request with the user's utterance. "
    "Do not answer general questions yourself."
)

data = {
    "serverUrl": server_url,
    "serverUrlSecret": webhook_secret,
    "server": server_config,
    "model": {
        "provider": "openai",
        "model": "gpt-4o",
        "messages": [{"role": "system", "content": system_prompt}],
        "tools": [calculator_tool, voiceos_request_tool],
    },
}


def request(method, payload=None):
    body = None if payload is None else json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    with urllib.request.urlopen(req) as response:
        raw = response.read()
        print("Assistant", method, "HTTP", response.status)
        return json.loads(raw.decode("utf-8")) if raw else {}


try:
    request("PATCH", data)
except urllib.error.URLError as e:
    print("Error updating Vapi assistant:", type(e).__name__, file=sys.stderr)
    if hasattr(e, "code"):
        print("HTTP", e.code, file=sys.stderr)
    sys.exit(1)
