import json
import random

tx = json.loads(input)
tx["fraudScore"] = round(random.random(), 3)
return json.dumps(tx)
