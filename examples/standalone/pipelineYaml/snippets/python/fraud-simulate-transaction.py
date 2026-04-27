import json
import random

parts = input.split("-")
index = int(parts[-1])
user_id = f"USER_{index % 1000:03d}"
amount = round(10.0 + random.random() * 4990.0, 2)
locations = ["NYC", "LAX", "CHI", "HOU", "PHX"]
return json.dumps({"userId": user_id, "amount": amount, "location": locations[index % 5]})
