/**
This package contains classes related to map projection.<br/>
fr.cnes.sitools.astro.graph is a Java package aiming to create density maps 
from Healpix index in a given projection. For this, we use two external libraries:
<ul>
 <li>Healpix for representing the sky</li>
 <li>jhlaps for the map projection</li>
</ul>
<br/>The design of classes inside this package is based on a decorator pattern.
<ul>
 <li>The component is the Graph class</li>
 <li>The concrete component is the GenerericProjection class</li>
 <li>The decorator is the AbstractGraphDecorator. The aim of this class is to
 decorate the graph with several decorators</li>
 <li>The concrete decorators are : CircleDecorator, CoordinateDecorator, 
 HealpixDensityMapDecorator, HealpixFootprint, HealpixGridDecorator and 
 ImageBackgroundDecorador</li>
</ul>  
<img src="doc-files/Graph.png"/>
<br/><br/><br/>
 
Finally, here is an example for which we create a map using AITOFF projection.
Moreover, a grid of coordinates and a circle are overlapped on this map.<br/>
<pre>
<code>
Graph graph = new GenericProjection(Graph.ProjectionType.AITOFF);
graph = new CoordinateDecorator(graph, Color.yellow, 0.5f);
graph = new CircleDecorator(graph, 0, 0, 20, 4, Scheme.RING, Color.CYAN, 0.5f);
</code>
</pre>

@copyright 2010-2013 CNES
@author Jean-Christophe Malapert
*/
package fr.cnes.sitools.astro.graph;
