package com.newmcs.filive

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.*
import com.newmcs.filive.atomic.misc.AtomicExceptField
import com.newmcs.filive.serialize.DefaultSerializer
import com.newmcs.filive.serialize.SerializeAdapter
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Proxy
import java.util.*

/*
 * The object does not depend on the IObject method, but it acts as a utility
 * to get the necessary values. You can use these objects to get values that
 * satisfy certain conditions, which can only be used in the IObject class.
 */
object IObjectUtil {

    /**
     * Checks the string can be converted to UUID.
     * @return Returns the string maybe UUID.
     */
    fun isUUID(s : String) : Boolean
    {
        return try {
            UUID.fromString(s)
            true
        } catch(_ : IllegalArgumentException) { false }
    }
    /**
     * Determines the Field is not serializable value.
     * This is considered Internal Type when the annotation is referenced or it
     * is an unpredictable value (isExpected is False).
     *
     * @param f The field that you want to check that is internal value
     * @return returns true if the field is internal value, otherwise false
     */
    fun isInternalField(f: Field): Boolean
    {
        if(f.isAnnotationPresent(AtomicExceptField::class.java)){
            val anno = f.getAnnotation(AtomicExceptField::class.java)
            return !anno.IsExpected
        }
        return false
    }

    /**
     * Changes the annotation value for the given key of the given annotation to newValue and returns
     * the previous value.
     *
     * @param annotation the target of annotation
     * @param key the field of the target that is annotation
     * @param newValue the value that will be applied to selected key
     * @return The old value fo annotation's key
     */
    @Suppress("UNCHECKED_CAST")
    fun changeAnnotationValue(annotation: Annotation, key: String, newValue: Any): Any
    {
        val handler = Proxy.getInvocationHandler(annotation)
        val f: Field
        try {
            f = handler.javaClass.getDeclaredField("memberValues")
            f.isAccessible = true
            val memberValues: HashMap<Any, Any> = f.get(handler) as HashMap<Any, Any>
            val oldValue = memberValues[key]
            if (oldValue == null || oldValue.javaClass != newValue.javaClass) {
                throw IllegalArgumentException()
            }
            memberValues[key] = newValue
            return oldValue
        }
        catch (e: IllegalArgumentException) {
            throw IllegalStateException(e)
        } catch (e: IllegalAccessException) {
            throw IllegalStateException(e)
        }
    }
    /**
     * Configure the gson for IObject de/serialization.
     * The default adapter of IObject will be detect your made the classes.
     *
     * @param adapterColl The collection of adapter for de/serialization
     * @param IObjectTypeOf The class for default adapter of IObject type
     * @param isPretty determines the json string is easy for understanding structures
     * @return Returns gson that was configured the adapters & properties
     */
    @Deprecated(message="This method is not implemented properly.")
    fun configureIObjectGson(adapterColl : Collection<SerializeAdapter<*>>?, IObjectTypeOf : Class<out IObject<*>>? = null, isPretty : Boolean = false) : Gson
    {
        val gsonBuilder = GsonBuilder()
        if(adapterColl != null) {
            for (adapter in adapterColl) {
                // If the DefaultSerializer is in the adapter, register the class type of
                // 'IObjectTypeOf' so that it can be serialized. DefaultSerializer is available
                // for all classes for IObject Type.
                val adapterType: Class<*> = if (adapter is DefaultSerializer) {
                    IObjectTypeOf ?: adapter.getReference()
                } else adapter.getReference()
                gsonBuilder.registerTypeAdapter(adapterType, adapter)
            }
        }
        /*
        else {
            //val cz = IObjectTypeOf ?: IObject::class.java
            // gsonBuilder = IObject.registerDefaultAdapter(gsonBuilder, cz)
        }
         */
        if(isPretty) gsonBuilder.setPrettyPrinting()
        return gsonBuilder.serializeNulls().create()
    }


    fun validateCLZUri(uri : String) : List<String>?
    {
        val r = Regex("${Standard.CLZ_LINK}(.+)@([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})")
        val result = r.matchEntire(uri) ?: return null
        return result.groupValues
    }

    /**
     * Sets the property on the JsonObject.
     * @param jsonObject will be configure the object and apply the value with serialize adapter
     * @param key the key of configured elements
     * @param value the configured element by 'configuredBuilder'
     * @param configuredBuilder The gson builder that was registered adapter it is related to IObject type
     * @param disableTransient determines the annotation of transient can be ignored when serializing the entity
     * @return Returns the configured JsonObject, which was applied it
     */
    fun setProperty(jsonObject : JsonObject, key : String, value : Any?, configuredBuilder : GsonBuilder, disableTransient : Boolean = false) : JsonObject
    {
        val gson = configuredBuilder.create()
        if(value == null) {
            jsonObject.add(key, JsonNull.INSTANCE)
            return jsonObject
        }
        when(value) {
            // Those types can use the default method.
            // There's no need specific process working.
            is Number  -> jsonObject.addProperty(key, value)
            is Char    -> jsonObject.addProperty(key, value)
            is String  -> jsonObject.addProperty(key, value)
            is Boolean -> jsonObject.addProperty(key, value)

            // It needs to other method.
            // Most of type could be IObject type or Collection, and Iterable.
            // Otherwise, It can't serialize.
            else -> {
                when(value) {
                    is IObject<*> -> {
                        // It depends the owner value.
                        value.disableTransient(disableTransient)

                        // If it is of type IObject, to deserialize is unreasonable. Note only the unique ID.
                        val owsUri = "${value::class.java.typeName}@${value.getUniqueId()}"
                        jsonObject.addProperty(key, "${Standard.CLZ_LINK}$owsUri")

                        if(value.getIObjectCollector() == null)
                            IObjectCollector.addUnspecifiedObjectType(value)
                        else
                            value.getIObjectCollector()!!.addReferenceObject(owsUri, value)
                    }
                    is Map<*,*> -> {
                        val mapTypeOf = JsonObject()
                        val mapper = ObjectMapper()
                        jsonObject.add(key, mapTypeOf)
                        jsonObject.addProperty(key, "Not supported Map type yet")
                    }
                    else -> try {
                        val result = gson.toJson(value)
                        val element = JsonParser.parseString(result)
                        jsonObject.add(key, element)
                    } catch(e : Exception) {
                        val ex = java.lang.RuntimeException("You need to configure the adapter this entity type: ${value::class.java.name}", e)
                        ex.printStackTrace()
                        jsonObject.addProperty(key, "FAILED_SERIALIZED_OBJECT")
                    }
                }
            }
        }
        return jsonObject
    }



    fun applyField(field : Field, target : Any?, victim : Any?) : Boolean
    {
        return try {
            field.isAccessible = true
            field.set(victim, field.get(target))
            true }
        catch(e : Exception) { false }
    }

    fun deleteDirectory(dir : File) : Boolean
    {
        if(dir.isDirectory) {
            val children = dir.listFiles() ?: return dir.delete()
            for(ch in children) if(!deleteDirectory(ch)) return false
        }
        return dir.delete()
    }
}