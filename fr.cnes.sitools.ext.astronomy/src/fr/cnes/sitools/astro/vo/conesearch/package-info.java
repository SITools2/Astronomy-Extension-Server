/**
 This package contains classes implementing the <a href="http://www.ivoa.net/Documents/latest/ConeSearch.html">cone search protocol</a>.
 <br/>
 <img src="../../../../../../images/SCP.png"/>
 @startuml SCP.png 
 package "fr.cnes.sitools.astro.representation" {
  class VOTableRepresentation
 }
 class ConeSearchProtocolLibrary {
   ConeSearchProtocolLibrary(DataSetApplication ds, ResourceModel rm, Request rq, Context cr)
   getResponse() : VOTableRepresentation
 }
 note left : CSP Server
 class ConeSearchInputParameters {
   ConeSearchInputParameters(DataSetApplication ds, Request rq, Context ct, ResourceModel rm)
   getContext() : Context
   getDataModel() : Map
   getDatasetApplication() : DataSetApplication
   getRa() : double
   getDec() : double
   getRequest() : Request
   getSr() : double
   getVerb() : int
 }
 interface ConeSearchDataModelInterface {
   getDataModel() : Map
 }
 class ConeSearchResponse {
   ConeSearchResponse(ConeSearchInputParameters inputParameters, ResourceModel model)
   getDataModel() : Map
 }
 ConeSearchProtocolLibrary *-- ConeSearchInputParameters
 ConeSearchInputParameters ..> ConeSearchDataModelInterface
 ConeSearchProtocolLibrary *-- ConeSearchResponse
 ConeSearchProtocolLibrary *-- VOTableRepresentation
 class ConeSearchException
 class ConeSearchQuery {
   ConeSearchQuery(final String urlVal)
   process(double ra, double dec, double radius) : List<Map<Field, String>>
 }
 note left : CSP Client
 interface ConeSearchQueryInterface
 class ConeSeachSolarObjectQuery
 note left : Client for solar object with time
 ConeSeachSolarObjectQuery ..> ConeSearchQueryInterface
 ConeSeachSolarObjectQuery *--> ConeSearchQuery
 @enduml
 */
package fr.cnes.sitools.astro.vo.conesearch;
