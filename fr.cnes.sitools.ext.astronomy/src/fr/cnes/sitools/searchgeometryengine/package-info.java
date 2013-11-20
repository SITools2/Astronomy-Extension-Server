/**
 This package contains classes related to a Search Geometry Engine based on Healpix.<br/>
 fr.cnes.sitools.SearchGeometryEngine is JAVA package aiming to compute HEALPIX
 index based on a geometry input. For this, we use two external libraries :
 <ul>
 <li>Healpix library</li>
 <li>MOC library from CDS</li>
</ul>
 
 As below, we present the class diagram of the library:<br/>
<img src="doc-files/SearchGeometryEngine.png"/>
<br/><br/><br/> 
Finally, here is an example on how to use it
<pre>
<code>
Shape bbox = new Polygon(new Point(20,-30,CoordSystem.EQUATORIAL), new Point(30,30,CoordSystem.EQUATORIAL));        
Index index = GeometryIndex.createIndex(bbox, Scheme.RING);
GeometryIndex.displayAsString(index.getIndex());                       
</code>
</pre>
 @copyright 2011, 2012 CNES
 @author Jean-Christophe Malapert 
 */
package fr.cnes.sitools.searchgeometryengine;
