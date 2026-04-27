import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.time.Instant

def event = new JsonSlurper().parseText(body)
def riskScore = (event.fraudScore ?: 0.0) as Double
if (riskScore > 0.85) {
    event.alertTime = Instant.now().toString()
    return JsonOutput.toJson(event)
} else {
    return null
}
