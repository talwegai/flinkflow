import json
return json.loads(input).get("STATUS") == "CRITICAL_ALERT"
