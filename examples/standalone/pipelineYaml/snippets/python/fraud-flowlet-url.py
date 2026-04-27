import json
tx = json.loads(input)
return f"http://internal-fraud-ml-service/score?userId={tx['userId']}&amount={tx['amount']}"
