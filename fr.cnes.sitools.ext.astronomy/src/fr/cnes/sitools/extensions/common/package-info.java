/**
 This package contains the common classes for the plugins.
 <p>
 Classes in this package are used in most of the plugins:
 <ul>
 <li>an Utility class</li>
 <li>AstroCoodinate to convert from a coordinates system to another one</li>
 <li>a set of classes for input validation</li>
 </ul>
<img src="../../../../../images/package-info-common.png"/>
</p>
@copyright 2011-2013 CNES
@author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
@startuml package-info-common.png
 scale 2/3
 abstract class Validation
 abstract class ValidationDecorator
 Validation <|-- ValidationDecorator
 Validation <|-- ApplicationParameterValidation
 Validation <|-- InputsAttributesValidation
 Validation <|-- InputsValidation
 ValidationDecorator <|-- NotNullAndNotEmptyValidation
 ValidationDecorator <|-- NumberArrayValidation
 ValidationDecorator <|-- NumberValidation
 ValidationDecorator <|-- RangeValidation
 ValidationDecorator <|-- SpatialGeoValidation
 ValidationDecorator <|-- StatusValidation
 ValidationDecorator o-- Validation
 Validation : Map<String, String> getMap()
 Validation : Map<String, String> getMap()
 Validation : setMap(final Map<String, String> mapToValidate)
 Validation : StatusValidation getStatusValidation()
 Validation : {abstract}#processValidation()
 ValidationDecorator : -validation
 ValidationDecorator : processValidation()
 ValidationDecorator : Map<String, String> getMap()
 ValidationDecorator : {abstract}#Map<String, String> localValidation()
 ApplicationParameterValidation : #processValidation()
 InputsAttributesValidation: #processValidation()
 InputsValidation: #processValidation()
 NotNullAndNotEmptyValidation: #Map<String, String> localValidation()
 NumberArrayValidation: #Map<String, String> localValidation()
 NumberValidation: #Map<String, String> localValidation()
 RangeValidation: #Map<String, String> localValidation()
 SpatialGeoValidation: #Map<String, String> localValidation()
 StatusValidation: #Map<String, String> localValidation()
 class AstroCoordinate
 abstract class Utility
 class VoDictionary
 note "Coordinates system conversion" as N1
 note "Utility class" as N2
 note "Create dictionary from VOTable" as N3
 AstroCoordinate .. N1
 Utility .. N2
 VoDictionary .. N3
@enduml
 */
package fr.cnes.sitools.extensions.common;
