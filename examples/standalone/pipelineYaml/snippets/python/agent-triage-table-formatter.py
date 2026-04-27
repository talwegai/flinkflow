import json

outer = json.loads(input)
raw = outer.get("output", "")
raw = raw.replace("```json", "").replace("```", "").strip()

try:
    inner = json.loads(raw)
except Exception:
    return f"[ERROR] Could not parse AI response: {raw}"

tid = inner.get("ticketId", "-")
urgency = inner.get("urgency", "LOW")
category = inner.get("category", "-")
action = inner.get("action", "-")

return f"{tid} | {urgency} | {category} | {action}"
