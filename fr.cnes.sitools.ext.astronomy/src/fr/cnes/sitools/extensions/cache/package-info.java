/**
 This package contains the cache classes.
<p>
Two cache systems are available:
<ul>
<li>a cache directive for the browsers</li>
<li>a server cache based on Healpix</li>
</ul>
</p>
<p>
<h2>Cache directive for browsers</h2>
<pre>
<code>
// Creates a daily cache for the representation "rep"
final CacheBrowser cache = CacheBrowser.createCache(CacheBrowser.CacheDirectiveBrowser.DAILY, rep);
// Sets the cache directive in the response
getResponse().setCacheDirectives(cache.getCacheDirectives());
// Gets the cache representation
cachedRepresentation = cache.getRepresentation();
</code>
</pre>
<h2>Server cache based on Healpix</h2>
<pre>
<code>
// Creates a unique cache ID based on applicationID-order-Healpix
final String cacheID = SingletonCacheHealpixDataAccess.generateId(applicationID, String.valueOf(userParameters.getOrder()), String.valueOf(userParameters.getHealpix()));
// Retrieves the instance from the singleton
final CacheManager cacheManager = SingletonCacheHealpixDataAccess.getInstance();
// Retrieves a cache configuration, which is defined in ehcache.xml
final Cache cache = cacheManager.getCache("VOservices");
// Puts in the cache
response = csQuery.getResponseAt(rightAscension, declination, radius);
final Element element = new Element(cacheID, response);
cache.put(element);
// Retrieves a specific element from the cache
response = (List<Map<Field, String>>) cache.get(cacheID).getObjectValue();
</code>
</pre>
</p>
@copyright 2011-2013 CNES
@author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
package fr.cnes.sitools.extensions.cache;
