import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def obj = new JsonSlurper().parseText(body)
obj.processed = true
return JsonOutput.toJson(obj)
