/**
This package contains classes related to name resolvers and reverse name resolvers.<br/>

A name resolver is a service providing sky positions given an object name.<br/>
The reverse name resolvers provides an object name given both a sky position in ICRS
and a radius.

<h2>Name resolver</h2>
Three name resolvers are currently implemented:
<ul>
 <li>CDS name resolver for objects</li>
 <li>IMCCE name resolver for asteroïd, planets, comets</li>
 <li>COROT ID name resolver for Corot objects</li>
</ul>

<img src="../../../../../images/Resolver.png"/>
<br/><br/><br/>

Here is an example how to use the CDS name resolver:<br/><br/>
<code>
AbstractNameResolver cds = new CDSNameResolver("m31", CDSNameResolver.NameResolverService.all);<br/>
NameResolverResponse response = cds.getResponse();
List<fr.cnes.sitools.extensions.common.AstroCoordinate> coordinates = response.getAstroCoordinates();<br/>
</code><br/>

<h2>Reverse Name Resolver</h2>
One reverse name resolver is currently implemented:
<ul>
 <li>Reverse name resolver from CDS</li>
</ul>

<img src="../../../../../images/ReverseResolver.png"/>
<br/><br/><br/>

Here is an example how to use the CDS reverse name resolver:<br/><br/>
<code>
ReverseNameResolver reverse = new ReverseNameResolver("23:55:20.04 -0:52:23.68",10);<br/>
System.out.println(reverse.getJsonResponse().toString());</code><br/> <br/>
The returned response is the following:<br/>
<pre>
{<br/>
totalResults=1, <br/>
features=[{<br/>
     properties={<br/>
         title=IC 1515 , <br/>
         magnitude=14.8, <br/>
         credits=CDS, <br/>
         seeAlso=http://simbad.u-strasbg.fr/simbad/sim-id?Ident=IC 1515 , <br/>
         type=Seyfert_2, <br/>
         identifier=IC 1515 <br/>
     },<br/>
     geometry={<br/>
         crs=EQUATORIAL.ICRS, <br/>
         type=Point, <br/>
         coordinates=[23.934419722222223,-0.9884027777777777]<br/>
     }<br/>
}]<br/>
}<br/>
</pre>
@copyright 2010-2013 CNES
@author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
@startuml Resolver.png
abstract class AbstractNameResolver {
  void setNext(AbstractNameResolver successorVal)
  {abstract} getResponse() : NameResolverResponse
  getSuccessor() : AbstractNameResolver
  void setSuccessor(AbstractNameResolver successorVal)
}

class CDSNameResolver {
  CDSNameResolver(String objectNameVal, NameResolverService service)
  getResponse() : NameResolverResponse
}

enum NameResolverService {
  ned
  simbad
  vizier
  all
}

class ConstellationNameResolver {
  ConstellationNameResolver(String constellationName)
  getResponse() : NameResolverResponse
}

class CorotIdResolver {
  CorotIdResolver(String corotIdVal)
  getResponse() : NameResolverResponse
}

class IMCCESsoResolver {
  IMCCESsoResolver(String objectNameVal, String epochVal)
  getResponse() : NameResolverResponse
}

AbstractNameResolver o-- NameResolverResponse
CDSNameResolver ..> ConstellationNameResolver
ConstellationNameResolver ..> IMCCESsoResolver
IMCCESsoResolver ..> CorotIdResolver
CDSNameResolver --|> AbstractNameResolver
IMCCESsoResolver --|> AbstractNameResolver
CorotIdResolver --|> AbstractNameResolver
ConstellationNameResolver --|> AbstractNameResolver
CDSNameResolver *-- NameResolverService

class NameResolverException
@enduml

@startuml ReverseResolver.png
class NameResolverException

class ReverseNameResolver
@enduml
*/
package fr.cnes.sitools.astro.resolver;

