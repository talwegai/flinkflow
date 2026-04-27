import json
obj = json.loads(input)
if obj.get("averageTemperature", 0.0) > 0.0:
    obj["STATUS"] = "CRITICAL_ALERT"
    obj["message"] = "Refrigeration failure detected! Cargo at risk."
else:
    obj["STATUS"] = "NORMAL"
return json.dumps(obj)
