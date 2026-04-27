import json
obj = json.loads(input)
status = "CRITICAL" if obj["temperature"] > 30 else "NORMAL"
return f"Truck {obj['truckId']} is {status} ({obj['temperature']}C)"
