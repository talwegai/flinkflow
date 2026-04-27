import json
tx = json.loads(input)
ml_response = json.loads(response)
tx["fraudScore"] = ml_response.get("probability_score", 0.0)
return json.dumps(tx)
