import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def tx = new JsonSlurper().parseText(body)
def ml = new JsonSlurper().parseText(headers.get("response").toString())
tx.fraudScore = (ml.probability_score ?: 0.0) as Double
return JsonOutput.toJson(tx)
