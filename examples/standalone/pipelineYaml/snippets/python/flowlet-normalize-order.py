parts = input.split(",")
if len(parts) >= 3:
    parts[2] = parts[2].strip().upper()
return ",".join(parts)
