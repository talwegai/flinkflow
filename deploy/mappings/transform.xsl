<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:fn="http://www.w3.org/2005/xpath-functions"
    version="3.0"
    expand-text="yes">
    
    <xsl:output method="text" encoding="UTF-8"/>

    <!-- This template matches the raw string input passed as an atomic value -->
    <xsl:template match=".[. instance of xs:string]">
        <xsl:variable name="inputJson" select="json-to-xml(.)"/>
        
        <!-- Define the output structure using the XSLT 3.0 XPath mapping format -->
        <xsl:variable name="outputMap">
            <fn:map>
                <fn:number key="user_id">{$inputJson//fn:number[@key='id']}</fn:number>
                <fn:string key="full_name">{$inputJson//fn:string[@key='name']}</fn:string>
                <fn:string key="contact">{$inputJson//fn:string[@key='email']}</fn:string>
                <fn:string key="processed_by">Flinkflow DataMapper (Saxon-HE)</fn:string>
                <fn:string key="timestamp">{current-dateTime()}</fn:string>
            </fn:map>
        </xsl:variable>
        
        <!-- Convert back to JSON string -->
        <xsl:value-of select="xml-to-json($outputMap)"/>
    </xsl:template>

</xsl:stylesheet>
