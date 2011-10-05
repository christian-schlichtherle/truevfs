<?xml version="1.0" encoding="UTF-8"?>
<!--
  - Copyright (C) 2011 Schlichtherle IT Services
  -
  - All rights reserved. This program and the accompanying materials
  - are made available under the terms of the Eclipse Public License v1.0
  - which accompanies this distribution, and is available at
  - http://www.eclipse.org/legal/epl-v10.html
  -->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:html="http://www.w3.org/1999/xhtml">
    <xsl:output method="text"/>

    <xsl:template match="html:head">
        <xsl:text>    ---&#x0a;    </xsl:text>
        <xsl:value-of select="normalize-space(html:title)"/>
        <xsl:text>&#x0a;    ---&#x0a;    </xsl:text>
        <xsl:value-of select="normalize-space(html:meta[@name='Author']/@content)"/>
        <xsl:value-of select="normalize-space(html:meta[@name='author']/@content)"/>
        <xsl:value-of select="normalize-space(html:meta[@name='AUTHOR']/@content)"/>
        <xsl:text>&#x0a;    ---&#x0a;&#x0a;</xsl:text>
    </xsl:template>

    <xsl:template match="html:table">
        <xsl:text>*</xsl:text>
        <xsl:for-each select=".//html:tr[1]/html:td">
            <xsl:text>--+</xsl:text>
        </xsl:for-each>
        <xsl:text>&#x0a;</xsl:text>
        <xsl:for-each select=".//html:tr">
            <xsl:text>|</xsl:text>
            <xsl:for-each select="html:th|html:td">
                <xsl:text> </xsl:text>
                <xsl:value-of select="normalize-space(.)"/>
                <xsl:text> |</xsl:text>
            </xsl:for-each>
            <xsl:text>&#x0a;*</xsl:text>
            <xsl:for-each select=".//html:tr[1]/html:td">
                <xsl:text>--+</xsl:text>
            </xsl:for-each>
            <xsl:text>&#x0a;&#x0a;</xsl:text>
        </xsl:for-each>
        <xsl:text>&#x0a;</xsl:text>
    </xsl:template>

    <xsl:template match="html:ul">
        <xsl:for-each select=".//html:li">
            <xsl:text>    * </xsl:text>
            <xsl:apply-templates/>
            <xsl:text>&#x0a;&#x0a;</xsl:text>
        </xsl:for-each>
        <xsl:text>    []&#x0a;&#x0a;</xsl:text>
    </xsl:template>

    <xsl:template match="html:ol">
        <xsl:for-each select=".//html:li">
            <xsl:text>    [[1]] </xsl:text>
            <xsl:apply-templates/>
            <xsl:text>&#x0a;&#x0a;</xsl:text>
        </xsl:for-each>
        <xsl:text>    []&#x0a;&#x0a;</xsl:text>
    </xsl:template>

    <xsl:template match="html:h2">
        <xsl:apply-templates/>
        <xsl:text>&#x0a;&#x0a;</xsl:text>
    </xsl:template>

    <xsl:template match="html:h3">
        <xsl:text>* </xsl:text>
        <xsl:apply-templates/>
        <xsl:text>&#x0a;&#x0a;</xsl:text>
    </xsl:template>

    <xsl:template match="html:h4">
        <xsl:text>** </xsl:text>
        <xsl:apply-templates/>
        <xsl:text>&#x0a;&#x0a;</xsl:text>
    </xsl:template>

    <xsl:template match="html:h5">
        <xsl:text>*** </xsl:text>
        <xsl:apply-templates/>
        <xsl:text>&#x0a;&#x0a;</xsl:text>
    </xsl:template>

    <xsl:template match="html:h6|html:h7|html:h8|html:h9">
        <xsl:text>**** </xsl:text>
        <xsl:apply-templates/>
        <xsl:text>&#x0a;&#x0a;</xsl:text>
    </xsl:template>

    <xsl:template match="html:p">
        <xsl:text>    </xsl:text>
        <xsl:apply-templates/>
        <xsl:text>&#x0a;&#x0a;</xsl:text>
    </xsl:template>

    <xsl:template match="html:a[@name]">
        <xsl:text> {</xsl:text>
        <xsl:apply-templates/>
        <xsl:text>} </xsl:text>
    </xsl:template>

    <xsl:template match="html:a[@href]">
        <xsl:text> {{{</xsl:text>
        <xsl:value-of select="@href"/>
        <xsl:text>}</xsl:text>
        <xsl:apply-templates/>
        <xsl:text>}} </xsl:text>
    </xsl:template>

    <xsl:template match="html:em">
        <xsl:text> &lt;</xsl:text>
        <xsl:apply-templates/>
        <xsl:text>&gt; </xsl:text>
    </xsl:template>

    <xsl:template match="html:strong|html:b">
        <xsl:text> &lt;&lt;</xsl:text>
        <xsl:apply-templates/>
        <xsl:text>&gt;&gt; </xsl:text>
    </xsl:template>

    <xsl:template match="html:code">
        <xsl:text> &lt;&lt;&lt;</xsl:text>
        <xsl:apply-templates/>
        <xsl:text>&gt;&gt;&gt; </xsl:text>
    </xsl:template>

    <xsl:template match="html:pre">
        <xsl:text>&#x0a;+--&#x0a;</xsl:text>
        <xsl:apply-templates/>
        <xsl:text>&#x0a;+--&#x0a;&#x0a;</xsl:text>
    </xsl:template>

    <xsl:template match="text()">
        <xsl:value-of select="normalize-space(.)"/>
    </xsl:template>

    <xsl:template match="@*"/>
</xsl:stylesheet>
