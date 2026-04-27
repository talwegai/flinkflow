import json
from datetime import datetime

order = json.loads(input)
amount = order["amount"]
order["tax_amount"] = round(amount * 0.07, 2)
order["total_amount"] = round(amount + order["tax_amount"], 2)
order["processed_at"] = datetime.now().isoformat()
return json.dumps(order)
