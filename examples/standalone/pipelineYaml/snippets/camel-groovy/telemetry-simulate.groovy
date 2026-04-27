import groovy.json.JsonOutput

def parts = body.split("-")
def index = parts[-1].toLong()
def trucks = ["TRUCK_01", "TRUCK_02", "TRUCK_03"]
def truckId = trucks[(index % 3).toInteger()]
def temp = (( -18.0 + Math.random() * 58.0) * 100).round() / 100.0
return JsonOutput.toJson([truckId: truckId, temperature: temp])
