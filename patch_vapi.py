import urllib.request
import urllib.error
import json

url = "https://api.vapi.ai/assistant/913e96d8-b178-40fa-ab9f-b86d47309057"
headers = {
    "Authorization": "Bearer c7ec70cb-9574-46a6-af44-5d061e748bac",
    "Content-Type": "application/json",
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
}

data = {
    "server": {
        "url": "https://caption-cranial-senate.ngrok-free.dev/api/webhooks/vapi",
        "timeoutSeconds": 20,
        "secret": "ganeshmaheshwaram"
    }
}

req = urllib.request.Request(url, data=json.dumps(data).encode('utf-8'), headers=headers, method='PATCH')

try:
    with urllib.request.urlopen(req) as response:
        print("Success:", response.read().decode())
except urllib.error.URLError as e:
    print("Error:", e)
    if hasattr(e, 'read'):
        print(e.read().decode())
