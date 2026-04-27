import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def tx = new JsonSlurper().parseText(body)
tx.fraudScore = ((Math.random() * 1000).round()) / 1000.0
return JsonOutput.toJson(tx)
