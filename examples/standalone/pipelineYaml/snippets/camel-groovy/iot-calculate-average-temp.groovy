import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def v1 = headers.get("value1")
def v2 = headers.get("value2")
if (v1 == null || v2 == null) return v1 ?: v2

def slurper = new JsonSlurper()
def o1 = slurper.parseText(v1.toString())
def o2 = slurper.parseText(v2.toString())

def t1 = (o1.get("sumTemp") ?: o1.get("temperature") ?: 0.0).toDouble()
def t2 = (o2.get("sumTemp") ?: o2.get("temperature") ?: 0.0).toDouble()
def c1 = (o1.get("count") ?: 1).toInteger()
def c2 = (o2.get("count") ?: 1).toInteger()

def totalSum = t1 + t2
def totalCount = c1 + c2
def avg = ((totalSum / totalCount) * 100).round() / 100.0

return JsonOutput.toJson([
    truckId: o1.truckId ?: o2.truckId,
    sumTemp: (totalSum * 100).round() / 100.0,
    count: totalCount,
    averageTemperature: avg
])
