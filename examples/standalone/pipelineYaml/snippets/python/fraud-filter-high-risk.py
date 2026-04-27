import json
from datetime import datetime

event = json.loads(input)
if event.get("fraudScore", 0.0) > 0.85:
    event["alertTime"] = datetime.now().isoformat()
    return json.dumps(event)
else:
    return None
