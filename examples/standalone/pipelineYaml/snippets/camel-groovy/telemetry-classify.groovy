import groovy.json.JsonSlurper

def obj = new JsonSlurper().parseText(body)
def temp = obj.temperature.toDouble()
def status = temp > 30.0 ? "CRITICAL" : "NORMAL"
return "Truck ${obj.truckId} is ${status} (${temp}°C)"
