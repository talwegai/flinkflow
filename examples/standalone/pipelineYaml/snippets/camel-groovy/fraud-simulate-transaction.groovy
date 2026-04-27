import groovy.json.JsonOutput

def parts = body.split("-")
def index = parts[-1].toLong()
def userId = String.format("USER_%03d", index % 1000)
def amount = (((10.0 + Math.random() * 4990.0) * 100).round()) / 100.0
def locations = ["NYC", "LAX", "CHI", "HOU", "PHX"]

return JsonOutput.toJson([
    userId: userId,
    amount: amount,
    location: locations[(index % 5).toInteger()]
])
