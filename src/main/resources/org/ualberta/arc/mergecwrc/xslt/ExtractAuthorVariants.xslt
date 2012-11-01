<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                version="2.0"
                exclude-result-prefixes="fn">     
    <xsl:output method="text" encoding="UTF-8"/> 
    
    <xsl:template match="/cwrc">
        <xsl:value-of select="fn:count(entity)"/>
        <xsl:text>&#xa;</xsl:text>
        <xsl:for-each select="entity/person/identity/variantForms/variant">
            <xsl:choose>
                <xsl:when test="(namePart[@partType = 'surname']) and (namePart[@partType = 'forename'])">
                    <xsl:value-of select="namePart[@partType = 'surname']"/>
                    <xsl:text> @ </xsl:text>
                    <xsl:value-of select="namePart[@partType = 'forename']"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="namePart"/>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:text>&#xa;</xsl:text>
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>