/**
This package contains classes to build a SOLR query String.<br/>

These classes are used to query a SOLR server.

Here is an example how to use it:<br/><br/>
<pre>
<code>
AbstractSolrQueryRequestFactory querySolr = AbstractSolrQueryRequestFactory.createInstance(queryParameters, coordSystem, getSolrBaseUrl(), healpixScheme);
querySolr.createQueryBuilder();
String query = querySolr.getSolrQueryRequest();
</code>
</pre>
@copyright 2012 2013 CNES
@author Jean-Christophe Malapert
*/
package fr.cnes.sitools.solr.query;                        