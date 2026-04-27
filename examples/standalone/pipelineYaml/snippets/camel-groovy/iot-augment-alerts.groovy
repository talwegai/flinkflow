import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def obj = new JsonSlurper().parseText(body)
def avg = (obj.get("averageTemperature") ?: 0.0).toDouble()
if (avg > 0.0) {
    obj.STATUS = "CRITICAL_ALERT"
    obj.message = "Refrigeration failure detected! Cargo at risk."
} else {
    obj.STATUS = "NORMAL"
}
return JsonOutput.toJson(obj)
