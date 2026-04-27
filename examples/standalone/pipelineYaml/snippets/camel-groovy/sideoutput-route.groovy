// camel-groovy sideoutput uses Janino API (ctx/out)
if (input.startsWith("ERROR:")) {
    ctx.output(input);
} else {
    out.collect(input);
}
