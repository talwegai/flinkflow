import json
order = json.loads(input)
return order.get("status") == "delivered"
