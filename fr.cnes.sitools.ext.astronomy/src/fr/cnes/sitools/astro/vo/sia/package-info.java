/**
 This library contains classes implementing the 
 <a href="http://www.ivoa.net/Documents/SIA/">simple image access protocol (SIAP)</a>.
 <br/>
 <img src="../../../../../../images/SIAP.png"/>
 @startuml SIAP.png 
 package "fr.cnes.sitools.astro.representation" {
  class VOTableRepresentation
 }
 class SimpleImageAccessProtocolLibrary {
   SimpleImageAccessProtocolLibrary(DataSetApplication ds, ResourceModel rm, Request rq, Context cr)
   getResponse() : VOTableRepresentation
 }
 note left : SIAP Server
 class SimpleImageAccessInputParameters {
   SimpleImageAccessInputParameters(DataSetApplication ds, Request rq, Context ct, ResourceModel rm)
   getContext() : Context
   getDataModel() : Map
   getDatasetApplication() : DataSetApplication
   getRa() : double
   getDec() : double
   getRequest() : Request
   getSize() : double[]
   getVerb() : int
 }
 interface DataModelInterface {
   getDataModel() : Map
 }
 class SimpleImageAccessResponse {
   SimpleImageAccessResponse(SimpleImageAccessInputParameters inputParameters, ResourceModel model)
   getDataModel() : Map
 }
 interface SimpleImageAccessDataModelInterface {
   getDataModel() : Map
 }
 abstract class AbstractSqlGeometryConstraint {
   {abstract} getSqlPredicat() : String
   {abstract} void setGeometry(Object geometry)
   void setInputParameters(SimpleImageAccessInputParameters inputParameters)
 }
 class SqlGeometryFactory {
   {static} create(String geometryIntersection) : AbstractSqlGeometryConstraint
 }
 SimpleImageAccessProtocolLibrary *-- SimpleImageAccessInputParameters
 SimpleImageAccessInputParameters ..> DataModelInterface
 SimpleImageAccessProtocolLibrary *-- SimpleImageAccessResponse
 SimpleImageAccessProtocolLibrary *-- VOTableRepresentation
 SimpleImageAccessResponse --> SimpleImageAccessDataModelInterface
 SimpleImageAccessResponse *-- AbstractSqlGeometryConstraint
 CenterModeIntersection --|> AbstractSqlGeometryConstraint
 OverlapsModeIntersection --|> AbstractSqlGeometryConstraint
 SqlGeometryFactory *-- CenterModeIntersection
 SqlGeometryFactory *-- OverlapsModeIntersection
 SimpleImageAccessResponse *-- SqlGeometryFactory
 class SimpleImageAccessException
 class SIASearchQuery {
   SIASearchQuery(final String urlVal)
   process(double ra, double dec, double size) : List<Map<Field, String>>
 }
 note left : SIAP Client
 @enduml
 */
package fr.cnes.sitools.astro.vo.sia;
