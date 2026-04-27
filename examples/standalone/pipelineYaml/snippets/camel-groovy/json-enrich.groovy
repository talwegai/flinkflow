import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.time.Instant

def order = new JsonSlurper().parseText(body)
def amount = order.amount as Double
order.tax_amount = ((amount * 0.07) * 100).round() / 100.0
order.total_amount = ((amount + order.tax_amount) * 100).round() / 100.0
order.processed_at = Instant.now().toString()
order.engine = "Camel-Groovy"
return JsonOutput.toJson(order)
