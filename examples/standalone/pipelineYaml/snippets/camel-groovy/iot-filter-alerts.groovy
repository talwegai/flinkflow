import groovy.json.JsonSlurper
return new JsonSlurper().parseText(body).STATUS == "CRITICAL_ALERT"
