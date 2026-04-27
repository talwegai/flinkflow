import json

order = json.loads(input)
result = []
for item in order.get("items", []):
    result.append(json.dumps({
        "order_id": order["id"],
        "item_name": item,
        "total_amount": order["total_amount"],
        "processed_at": order["processed_at"]
    }))
return result
