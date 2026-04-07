<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <!--
    Test XSLT stylesheet: prepends "FILTERED:" to strings that contain the
    word "error" (case-insensitive) and passes others through unchanged.
    Used by DataMapperFunctionTest to verify conditional XSLT logic.
  -->
  <xsl:output method="text" omit-xml-declaration="yes"/>

  <xsl:template match=".[. instance of xs:string]">
    <xsl:choose>
      <xsl:when test="contains(lower-case(.), 'error')">
        <xsl:value-of select="concat('FILTERED:', .)"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="."/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>
