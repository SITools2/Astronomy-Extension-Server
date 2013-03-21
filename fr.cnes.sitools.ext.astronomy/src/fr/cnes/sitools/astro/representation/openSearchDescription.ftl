<?xml version="1.0" encoding="UTF-8"?>
<OpenSearchDescription xmlns="http://a9.com/-/spec/opensearch/1.1/"
                       <#if referenceSystem == "geocentric">xmlns:geo="http://a9.com/-/opensearch/extensions/geo/1.0/"</#if>
                       <#if referenceSystem == "ICRS">xmlns:astro="http://a9.com/-/opensearch/extensions/astro/1.0/"</#if>
                       xmlns:sitools="http://sitools2.sourceforge.net/opensearchextensions/1.0"
                       xmlns:time="http://a9.com/-/opensearch/extensions/time/1.0/">
    <ShortName>${shortName}</ShortName>
    <Description>${description}</Description>
    <#if syndicationRight != "closed">
    <Url type="application/json" template="${templateURL}"/>
    <#if describe?exists><Url type="application/json" rel="mspdesc" template="${describe}"/></#if>
    </#if>
    <#if clusterTemplateURL?exists><Url type="application/json" rel="clusterdesc" template="${clusterTemplateURL}"/></#if>            
    <#if mocdescribe?exists><Url type="application/json" rel="mocdesc" template="${mocdescribe}"/></#if>    
    <#if dicodescribe?exists><Url type="text/plain" rel="dicodesc" template="${dicodescribe}"/></#if>        
    <#if contact?exists><Contact>${contact}</Contact></#if>
    <#if tags?exists><Tags>${tags}</Tags></#if>
    <#if longName?exists><LongName>${longName}</LongName></#if>
    <#if imagePng?exists><Image height="64" width="64" type="image/png">${imagePng}</Image></#if>
    <#if imageIcon?exists><Image height="16" width="16" type="image/vnd.microsoft.icon">${imageIcon}</Image></#if>
<!--    <Query role="example" searchTerms="optique" /> -->
    <Developer>Jean-Christophe Malapert - SITools2 Team</Developer>
    <#if syndicationRight?exists><SyndicationRight>${syndicationRight}</SyndicationRight></#if>
    <Attribution>Search data Copyright 2012 2013, SITools2, All Rights Reserved</Attribution>
    <Language>en-us</Language>
</OpenSearchDescription>