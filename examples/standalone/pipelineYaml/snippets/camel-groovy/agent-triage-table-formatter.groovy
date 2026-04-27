import groovy.json.JsonSlurper

def wrapper = new JsonSlurper().parseText(body)
def raw = wrapper.output?.toString() ?: body
def cleaned = raw.replaceAll('(?s)```json\\s*', '').replaceAll('```', '').trim()
def ticket = new JsonSlurper().parseText(cleaned)
return "${ticket.ticketId} | ${ticket.urgency} | ${ticket.category} | ${ticket.action}"
