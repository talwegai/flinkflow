import groovy.json.JsonSlurper
def tx = new JsonSlurper().parseText(body)
return "http://internal-fraud-ml-service/score?userId=${tx.userId}&amount=${tx.amount}"
