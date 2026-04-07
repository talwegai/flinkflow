<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <!--
    Test XSLT stylesheet: uppercases whatever plain-text string is passed in
    as the context item. Used by DataMapperFunctionTest.
  -->
  <xsl:output method="text" omit-xml-declaration="yes"/>

  <xsl:template match=".[. instance of xs:string]"
                xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <xsl:value-of select="upper-case(.)"/>
  </xsl:template>
</xsl:stylesheet>
