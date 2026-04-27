import groovy.json.JsonSlurper
return new JsonSlurper().parseText(body).status == "delivered"
