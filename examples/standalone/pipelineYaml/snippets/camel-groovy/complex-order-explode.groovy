import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def order = new JsonSlurper().parseText(body)
return order.items.collect { item ->
    JsonOutput.toJson([
        order_id: order.id,
        item_name: item,
        total_amount: order.total_amount,
        processed_at: order.processed_at
    ])
}
