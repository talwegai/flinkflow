import json
import random

parts = input.split("-")
index = int(parts[-1])
trucks = ["TRUCK_01", "TRUCK_02", "TRUCK_03"]
truck_id = trucks[index % 3]
temp = round(-18.0 + (random.random() * 45.0), 2)
return json.dumps({"truckId": truck_id, "temperature": temp})
