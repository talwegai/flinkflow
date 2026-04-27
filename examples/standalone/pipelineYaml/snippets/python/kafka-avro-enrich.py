import json
obj = json.loads(input)
obj["processed"] = True
return json.dumps(obj)
