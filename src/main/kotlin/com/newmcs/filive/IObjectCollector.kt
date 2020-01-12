package com.newmcs.filive

import com.google.common.collect.ArrayListMultimap
import com.google.gson.*
import com.newmcs.filive.atomic.misc.AtomicExceptField
import com.newmcs.filive.handler.CollectionHandler
import com.newmcs.filive.handler.IObjectHandler
import com.newmcs.filive.handler.ISustainable
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import java.lang.RuntimeException
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList

/**
 * IObjectCollector is the collection the entity object which was registered by IObject$register at
 * generic type, inherited with IObject Class.
 * It cans monitor the change of the class value or field, and built in database form.
 *
 * @since 2.0.0
 * @author ruskonert
 */
@Suppress("UNCHECKED_CAST")
abstract class IObjectCollector<Entity : IObject<Entity>> : ISustainable<IObjectHandler<*>>
{
    override fun sustain(handleInstance: Any?)
    {
        for(entity in this.entityCollection)
        {
            if(entity.isEnabled())
            {
                entity.sustain(this)
            }
        }
    }

    override fun setEnable(enable: Boolean)
    {

    }

    override fun isEnabled(): Boolean
    {
        return true
    }

    override fun setEnable(handleInstance: IObjectHandler<*>?)
    {

    }


    @Suppress("UNCHECKED_CAST")
    @AtomicExceptField
    private val persistentBaseClass : Class<Entity> = (javaClass.genericSuperclass as? ParameterizedType)!!.actualTypeArguments[0] as Class<Entity>
    fun getPersistentBaseClass() : Class<Entity> = this.persistentBaseClass

    /**
     *
     */
    private val uid : String
    fun getUniqueId() : String = this.uid

    /**
     *
     */
    private var entityCollection : MutableList<Entity>
    constructor() : this(UUID.randomUUID().toString())
    protected constructor(uuid : String) : super()
    {
        this.uid = uuid
        this.entityCollection = ArrayList()
    }

    fun create() : IObjectCollector<Entity>
    {
        return this
    }


    /**
     *
     */
    fun create(handler : CollectionHandler) : IObjectCollector<Entity>
    {
        handlerCollections.put(handler, this)
        this.handler = handler
        return this
    }

    /**
     *
     */
    private lateinit var handler : IObjectHandler<*>
    fun getHandler() : IObjectHandler<*> = this.handler

    /**
     * Retrieves all entities that have the class type of the Collection.
     * The entities are those in which disk I/O synchronization is continuously performed by the create function.
     * @return The entities with continuous disk I/O synchronization
     * @see
     */
    fun getEntities() : MutableList<Entity> = this.entityCollection

    /**
     * A collection of field names for identifying entities.
     * It will be use 'uid' to identify the Entity only if it is empty.
     */
    private val identifier : MutableList<String> = arrayOf("uid").toMutableList()

    /**
     * Add a field name to identify the Entity you want to import.
     * The getEntity method can identify and retrieve an Entity through a stored signature.
     * @param signature The field names defined or inferred within the class
     * @see
     */
    fun addIdentity(vararg signature : String) {
        for(sig in signature) {
            if(! this.identifier.contains(sig)) {
                this.identifier.add(sig)
            }
        }
    }
    fun getIdentifier() : List<String> = this.identifier

    private val referenceObject : ConcurrentHashMap<String, IObject<*>> = ConcurrentHashMap()
    fun addReferenceObject(clzUri : String, iObject : IObject<*>) { this.referenceObject[clzUri] = iObject }


    open fun getEntity(objectData: Any?) : Entity?
    {
        if(objectData == null) return null
        return getEntity0(objectData, this.getPersistentBaseClass())
    }

    fun terminate() {
        val obj = this
        val collection = handlerCollections[obj.handler]
        collection.remove(obj)
        for(entity in obj.entityCollection) {
            entity.disconnectCollection()
        }
        obj.entityCollection.clear()
    }

    override fun toString(): String {
        return "${this.persistentBaseClass}::${uid}"
    }


    companion object
    {
        private val handlerCollections : ArrayListMultimap<IObjectHandler<*>, IObjectCollector<*>> = ArrayListMultimap.create()
        fun getEntityCollections() : ArrayListMultimap<IObjectHandler<*>, IObjectCollector<*>> = handlerCollections


        fun <U : IObject<U>> getEntityCollection(ref : Class<U>) : IObjectCollector<U>?
        {
            @Suppress("UNCHECKED_CAST")
            for(k in getEntityCollections().values()) {
                if(ref.isAssignableFrom(k.getPersistentBaseClass()))
                    return k as? IObjectCollector<U>
            }
            return null
        }


        fun getUnspecifiedCollection(ref: Class<*>) : IObjectCollector<*>?
        {
            for(k in getEntityCollections().values()) {
                if(ref.isAssignableFrom(k.getPersistentBaseClass()))
                    return k
            }
            return null
        }


        private fun <V, E> inlineNullCheck(value : V, entity : E, function : (V, E) -> Any?) : Boolean {
            val result : Any? = function(value, entity)
            return result != null
        }


        private fun <E : IObject<E>> getEntity0(objectData: Any, refClazz : Class<E>): E?
        {
            try {
                val collection = getEntityCollection(refClazz)
                        ?: return null
                val registerEntities = collection.getEntities()
                if(registerEntities.isEmpty()) return null

                val checkFunction0 = fun(value : String, target : IObject<*>) : E? {
                    for (field in target.getSerializableEntityFields(specific = collection.getIdentifier())) {
                        field.isAccessible = true
                        if(field.type != String::class.java) continue
                        if((field.get(target) as String) == value) return target as E?
                    }
                    return null
                }

                for(entity in registerEntities) {
                    when (objectData) {
                        is String -> {
                            if (inlineNullCheck(objectData, entity, checkFunction0)) return entity
                        }
                        else -> {
                            throw NotImplementedError("Not implemented this case.")
                        }
                    }
                }
                return null
            }
            catch(e : TypeCastException) {
                return null
            }
        }


        inline fun <reified U : IObject<out U>> deserialize(element : String, constructorIndexOf: Int = 0, vararg constructorParam: Any? = arrayOfNulls(0)) : U?
        {
            return deserialize<U>(JsonParser.parseString(element), constructorIndexOf, *constructorParam)
        }


        inline fun <reified U : IObject<out U>> deserialize(element : JsonElement, constructorIndexOf: Int = 0, vararg constructorParam: Any? = arrayOfNulls(0)) : U?
        {
            val targetObject: U?
            val constructColl = U::class.constructors
            val toJsonObject: JsonObject
            when(element) {
                is JsonNull -> return null
                else -> toJsonObject = element as JsonObject
            }
            targetObject = constructColl.toList()[constructorIndexOf].call(*constructorParam)
            for(field in (targetObject as IObject<*>).getSerializableEntityFields()) {
                val refValue = toJsonObject.get(field.name)
                if(refValue == null) {
                    println("The variable '${field.name}' is invalid value that compare with base class")
                    continue
                }
                when {
                    IObject::class.java.isAssignableFrom(field.type) -> {
                        field.set(targetObject, deserializeFromClass(refValue, U::class.java, constructorIndexOf, *constructorParam))
                    }
                    else -> tryExecuteSerialize(refValue, field, targetObject)
                }
            }
            targetObject.subCompleted()
            return targetObject
        }


        fun tryExecuteSerialize(refValue : JsonElement, field : Field, targetObject : Any)
        {
            val result = tryAvailableSerialize(refValue, field.type, targetObject as IObject<*>)
            if(result != null) {
                if(Modifier.isFinal(field.modifiers)) {
                    val modifiersField = Field::class.java.getDeclaredField("modifiers")
                    modifiersField.isAccessible = true
                    modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
                }
                val resultClazz = result::class.java
                field.set(targetObject, resultClazz.cast(result))
            }
        }

        fun <U : IObject<*>> deserializeFromClass(element : JsonElement, reference: Class<U>, constructorIndexOf: Int = 0, vararg constructorParam: Any? = arrayOfNulls(0)) : U?
        {
            val targetObject: U?
            val constructColl = reference.constructors
            val toJsonObject: JsonObject
            when(element) {
                is JsonPrimitive -> {
                    if(element.isString) {
                        val result = IObjectUtil.validateCLZUri(element.asString)
                                ?: return null
                        val refClazzName = result[1]
                        val uuid = result[2]
                        val collection = getUnspecifiedCollection(Class.forName(refClazzName))
                                ?: return null
                        return collection.getEntity(uuid) as? U ?: collection.referenceObject["$refClazzName@$uuid"] as? U
                    }
                    else {
                        throw RuntimeException("Unsupported type for deserialize value")
                    }
                }
                is JsonNull -> return null
                else -> toJsonObject = element as JsonObject
            }

            targetObject = constructColl[constructorIndexOf].newInstance(*constructorParam) as? U
            if(targetObject == null) return null
            for(field in targetObject.getSerializableEntityFields()) {
                val refValue = toJsonObject.get(field.name)
                if(refValue == null) {
                    println("The variable '${field.name}' is invalid value that compare with base class")
                    continue
                }

                when {
                    IObject::class.java.isAssignableFrom(field.type) -> field.set(targetObject,
                            deserializeFromClass(refValue, targetObject::class.java, constructorIndexOf, *constructorParam))
                    else -> tryExecuteSerialize(refValue, field, targetObject)
                }
            }
            return targetObject
        }


        protected fun tryAvailableSerialize(jsonElement : JsonElement, ref : Class<*>, targetSerializer : IObject<*>) : Any?
        {
            return try {
                val gson = targetSerializer.configureSerializeBuilder().create()
                gson.fromJson(jsonElement, ref)
            } catch(e : Exception) {
                null
            }
        }


        fun findIObjectField(target : Class<*> = this::class.java, fieldName : String) : Field?
        {
            val function0 = fun(c : Class<*>, name : String) : Field? {
                return try {c.getDeclaredField(name)}
                catch(e : NoSuchFieldException) { null }
            }
            if(target == Any::class.java) return null

            when {
                target.superclass != Any::class.java -> {
                    return function0(target, fieldName) ?: return findIObjectField(target.superclass, fieldName)
                }
                target.genericSuperclass != Any::class.java -> {
                    return function0((target.genericSuperclass as ParameterizedTypeImpl).rawType, fieldName) ?:
                    return findIObjectField((target.genericSuperclass as ParameterizedTypeImpl).rawType, fieldName)
                }
                else -> {
                    return null
                }
            }
        }

        fun <E : IObject<E>> setReference(entity: IObject<E>, containable : Boolean = false)
        {
            for(k in getEntityCollections().values()) {
                if(entity::class.java.isAssignableFrom(k.getPersistentBaseClass())) {
                    // Hook the reference collection.
                    var eField = findIObjectField(entity::class.java, "collection")
                            ?: return
                    eField.isAccessible = true
                    eField.set(entity, k)

                    eField = findIObjectField(entity::class.java, "uid")
                            ?: return
                    eField.isAccessible = true

                    // Generate the unique signature if the entity have no id.
                    val uuid = eField.get(entity) as? String
                    if (uuid == null || !IObjectUtil.isUUID(uuid) || uuid == IObject.UNREFERENCED_UNIQUE_ID) eField.set(entity, UUID.randomUUID().toString())
                    val targetRef = k.entityCollection as MutableList<E>
                    if(targetRef.contains(entity))
                        println("[Warning] This object[${entity.getUniqueId()}] already registered, trying to register a duplicate object!")
                    targetRef.add(entity as E)
                    entity.setEnable(k.getHandler())
                    return
                }
            }
            println("Not exist IObjectCollector<${entity::class.java.simpleName}>, It needs to specific entity collection from class.")
        }


        fun handlerFrom(handler: CollectionHandler): List<IObjectCollector<*>>? {
            return handlerCollections[handler]
        }


        private val unspecificObjectRef : ArrayList<IObject<*>> = ArrayList()
        fun addUnspecifiedObjectType(value: IObject<*>) { unspecificObjectRef.add(value) }


        fun getUnspecifiedObject(clzUri : String) : IObject<*>? {
            IObjectUtil.validateCLZUri(clzUri) ?: return null
            for(value in unspecificObjectRef) {
                if(clzUri == "${Standard.CLZ_LINK}${value.getReference().typeName}@${value.getUniqueId()}") return value
            }
            return null
        }
    }
}