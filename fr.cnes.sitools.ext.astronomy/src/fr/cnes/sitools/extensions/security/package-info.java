/**
This package contains the security classes for the data storage.

 <p>
 A data storage is a directory from the file system that is put online on the web.

 When the administrator configures a data storage, all files in this data storage are
 available.<br/><br/>
 The access to a datastorage can be restricted to a specific profile.
 However, only the access to specific extensions and the access to some files
 for some users who are included in the same profile are not possible.
 Thus, the purpose of these extensions is to customize the access to the files
 by different ways:
 <ul>
 <li>by checking a pattern in the filename that is requested</li>
 <li>by delegating the access rights to an external database</li>
 </ul>
 All these extensions are based on the following sequence diagram:
 <br/>
 <img src="../../../../../images/package-info-security.png"/>
 <br/>
 </p>
@copyright 2011-2013 CNES
@author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
@startuml package-info-security.png
User -> filter: File Request
activate filter
filter --> User: File Request is not accepted
filter -> dataStorage: File Request is accepted
deactivate filter
activate dataStorage
dataStorage --> User: file content
deactivate dataStorage
@enduml
 */
package fr.cnes.sitools.extensions.security;
