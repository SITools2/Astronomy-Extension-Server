/**
This package contains classes for inserting metadadata in a SOLR server.

Metadata is stored in a SGBD, so we need to configure the SOLR xml file. This file
is used to parse the SGBG by the Solr DataImportHandler. An example of this file is given
as follows:
<pre>
<code>
<dataConfig>
    <dataSource  type="JdbcDataSource" driver="org.postgresql.Driver" url="jdbc:postgresql://localhost:5432/cnes_test?schema=fuse" user="<login>" password="<pwd>" />
    <document name="headers">
        <entity name="headers" query="select * from fuse.headers" transformer="fr.cnes.sitools.solr.transformer.WcsTransformer" minOrder="3" maxOrder="13" scheme="NESTED">
            <field column="targname" name="properties.title"/>
            <field column="dataset" name="properties.identifier" />
            <field column="targname" name="properties.description" />
            <field column="ra_targ" wcs="RA"/>
            <field column="dec_targ" wcs="DEC"/>
            <field column="dateobs" name="properties.dateobs"/>
            <field column="exptime" name="properties.nostandard.exptime"/>
            <field column="aperture" name="properties.nostandard.aperture" />
            <field column="mode" name="properties.nostandard.mode" />
            <field column="expos_nbr" name="properties.nostandard.expos_nbr" />
            <field column="vmag" name="properties.nostandard.vmag" />
            <field column="sp_type" name="properties.nostandard.spectralYype" />
            <field column="ebv" name="properties.nostandard.ebv" />
            <field column="objclass" name="properties.nostandard.object class" />
            <field column="src_type" name="properties.nostandard.src_type" />
            <field column="datearchiv" name="properties.nostandard.datearchiv" />
            <field column="datepublic" name="properties.nostandard.datepublic" />
            <field column="ref" name="properties.nostandard.ref" />
            <field column="z" name="properties.nostandard.z" />
            <field column="starttime" name="properties.nostandard.startTime" />
            <field column="endtime" name="properties.nostandard.endTime" />
        </entity>
    </document>
</dataConfig>
</code>
</pre>
<br/>
Then we need to modify the schema.xml file in the SOLR core as follows:
<pre>
<code>
 <fields>
        <!-- general properties -->
	<field name="properties.identifier" type="string" indexed="true" stored="true" />
        <field name="properties.title" type="text_ws" indexed="true" stored="true"/>
        <field name="properties.description" type="text_ws" indexed="true" stored="true"/>
        <field name="properties.ele" type="sdouble" indexed="true" stored="true"/>
	<field name="properties.thumbnail" type="text_ws" indexed="false" stored="true"/>
        <field name="properties.quicklook" type="text_ws" indexed="false" stored="true"/>
       	<field name="properties.icon" type="text_ws" indexed="false" stored="true"/>

	<!-- download service -->
	<field name="properties.services.download.url" type="text_ws" indexed="false" stored="true"/>
	<field name="properties.services.download.mimetype" type="text_ws" indexed="false" stored="true"/>

	<!-- browse service -->
        <field name="properties.services.browse.title" type="text_ws" indexed="false" stored="true"/>
        <field name="properties.services.browse.layer.type" type="text_ws" indexed="false" stored="true"/>
        <field name="properties.services.browse.layer.title" type="text_ws" indexed="false" stored="true"/>
        <field name="properties.services.browse.layer.opacity" type="float" indexed="false" stored="true"/>
        <field name="properties.services.browse.layer.minlevel" type="integer" indexed="false" stored="true"/>
        <field name="properties.services.browse.layer.url" type="text_ws" indexed="false" stored="true"/>
        <field name="properties.services.browse.layer.layers" type="text_ws" indexed="false" stored="true"/>
        <field name="properties.services.browse.layer.version" type="text_ws" indexed="false" stored="true"/>
        <field name="properties.services.browse.layer.bbox" type="text_ws" indexed="false" stored="true"/>
        <field name="properties.services.browse.layer.srs" type="text_ws" indexed="false" stored="true"/>

        <!-- geometry -->
	<field name="geometry.coordinates" type="text_ws" indexed="false" stored="true"/>
        <field name="geometry.coordinates.type" type="text_ws" indexed="true" stored="true"/>
      	<field name="properties.ra" type="sdouble" indexed="true" stored="true"/>
        <field name="properties.dec" type="sdouble" indexed="true" stored="true"/>
        <field name="properties.dateobs" type="date" indexed="true" stored="true"/>
	<!-- no standard -->
	<dynamicField name="properties.nostandard.*" type="text_ws" indexed="true" stored="true"/>
	<!-- Healpix index -->
	<dynamicField name="order*" type="slong" indexed="true" stored="true" multiValued="true"/>
        <field name="searchTerms" type="text" indexed="true" stored="false" multiValued="true" />
        <copyField source="properties.identifier" dest="searchTerms" />
        <copyField source="properties.description" dest="searchTerms" />
        <copyField source="properties.nostandard.*" dest="searchTerms" />
 </fields>
</code>
</pre>
*/
package fr.cnes.sitools.solr.transformer;
