String[] parts = input.split(",", -1);
if (parts.length >= 3) {
    parts[2] = parts[2].trim().toUpperCase();
}
return String.join(",", parts);
