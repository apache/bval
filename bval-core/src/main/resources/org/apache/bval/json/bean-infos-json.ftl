bval.namespace("bval.metadata");

(function(){
<#assign var = 0>
<#assign varrefs = {}>

<#list metaBeans as metaBean>
<#assign varrefs = varrefs + {metaBean.id : var}>
var metaBean${var} = {
 "id" : "${metaBean.id}",
 <#if metaBean.beanClass??>"beanClass" : "${metaBean.beanClass.name}",</#if>
 <#if metaBean.name??>"name" : "${metaBean.name}",</#if>
 "features" :{<#rt/>
   <#list metaBean.features?keys as featureKey>
      <#assign value = metaBean.features[featureKey]>
   "${featureKey}" : <#if
          value?is_string>"${value}"<#elseif
          value?is_boolean>${value?string}<#elseif
          value?is_number>${value}<#elseif
          value?is_date>${value?string.full}<#elseif
          !(value??)>null<#elseif value?is_sequence>[<#list value as v>"${v}"<#if v_has_next>,</#if></#list>]<#elseif
          value?is_hash>{<#list value?keys as key>"${key}" : "${value[key]}"<#if key_has_next>,</#if></#list>}<#else
          >"?"</#if><#rt/><#if
          featureKey_has_next>,</#if>
   </#list>
   <#lt/>},
 "properties" :{
   <#list metaBean.properties as property>
     "${property.name}":{
       "name" : "${property.name}",
       <#if property.type??>"type" : "${property.type.name}",</#if>
       "features" : {<#if property.type?? &&
       property.type.enum>"enum" : {<#list property.type.enumConstants as enum>"${enum.name()}": "${enum.name()}"<#if enum_has_next>, </#if></#list>}<#if property.features?size &gt; 0>,</#if></#if><#list
       property.features?keys as featureKey>
       <#assign value = property.features[featureKey]>
       "${featureKey}" : <#rt/><#if
          value?is_string>"${value}"<#elseif
          value?is_boolean>${value?string}<#elseif
          value?is_number>${value}<#elseif
          value?is_date>${value?string.full}<#elseif
          !(value??)>null<#elseif value?is_sequence>[<#list value as v>"${v}"<#if v_has_next>,</#if></#list>]<#elseif
          value?is_hash_ex>{<#list value?keys as key>"${key}" : "${value[key]}"<#if key_has_next>,</#if></#list>}<#else
          >"?"</#if><#rt/><#if featureKey_has_next>,</#if>
       </#list>
       }
     }<#if property_has_next>,</#if>
   </#list>
   }
};
<#assign var = var + 1>
</#list>

<#assign var = 0>
<#list metaBeans as metaBean><#list
    metaBean.properties as property><#if
    property.metaBean?? && varrefs[property.metaBean.id]??>
metaBean${var}.properties.${property.name}.metaBean = metaBean${varrefs[property.metaBean.id]};
</#if></#list><#assign var = var + 1></#list><#assign var = 0>
bval.metadata.metaBeans = {
<#list metaBeans as metaBean>
       "${metaBean.id}" : metaBean${var}<#if metaBean_has_next>,</#if>
       <#assign var = var + 1>
</#list>};
})();