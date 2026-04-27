import groovy.json.JsonSlurper
return new JsonSlurper().parseText(body).truckId
