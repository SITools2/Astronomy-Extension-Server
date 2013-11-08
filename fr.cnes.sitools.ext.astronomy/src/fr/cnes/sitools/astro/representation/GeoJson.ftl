{
    "totalResults": ${totalResults?string.computer},
    "type": "FeatureCollection",
    "features": [
        <#list features as feature>
        {
            "type": "Feature",
            "geometry": {
                "coordinates": ${feature.geometry.coordinates},
                "type": "${feature.geometry.type}"
            },
            "properties": {
                "crs": {
                    "type": "name",
                    "properties": {
                        "name": "${feature.properties.crs.properties.name}"
                    }
                },
                <#list feature.properties?keys as key><#if key?exists><#assign value = feature.properties[key]><#if value?has_content><#if value?is_number>"${key}":${value?string.computer}<#if key_has_next>,
                </#if><#elseif value?is_string>"${key}":"${value}"<#if key_has_next>,
                </#if></#if></#if></#if></#list>
            }<#if feature.services?exists>,
            "services": {
                <#if feature.services.download?exists>
                  "download":{
                   "mimetype":"${feature.services.download.mimetype}",
                   "url":"${feature.services.download.url}"
                   }
                </#if>
             }
            </#if> 
        }<#if feature_has_next>,</#if>
        </#list>
    ]
}