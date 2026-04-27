# side_emit sends a record to the dead-letter side output
if input.startswith("ERROR:"):
    side_emit(input)
else:
    return input
