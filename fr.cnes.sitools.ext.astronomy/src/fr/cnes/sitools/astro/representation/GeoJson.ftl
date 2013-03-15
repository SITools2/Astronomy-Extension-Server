{
    "totalResults": ${totalResults?string.computer},
    "type": "FeatureCollection",
    "features": [
        <#list features as feature>
        {
            "geometry": {
                "coordinates": ${feature.geometry.coordinates},
                "type": "${feature.geometry.type}"
            },
            "properties": {
                "crs": {
                    "type": "name",
                    "properties": {
                        "name": "${feature.geometry.crs}"
                    }
                },
                <#list feature.properties?keys as key>
                <#if key?exists>
                <#assign value = feature.properties[key]>
                "${key}":<#if value?is_number>${value?string.computer}<#else>"${value}"</#if><#if key_has_next>,</#if>
                </#if>
                </#list>                        
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