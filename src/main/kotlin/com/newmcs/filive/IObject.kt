package com.newmcs.filive

import com.google.gson.*
import com.newmcs.filive.atomic.misc.AtomicExceptField
import com.newmcs.filive.concurrent.PeriodicObject
import com.newmcs.filive.handler.IObjectHandler
import com.newmcs.filive.serialize.DefaultSerializer
import com.newmcs.filive.serialize.SerializeAdapter
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.ArrayList
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/**
 * IObject is an automatic serializable class that can detect the values that need serialization
 * for defined fields and class types, and build the adapter. This saves development time by
 * eliminating the developer writing an adapter and registering it. it is not an abstract class,
 * but it is not intended to be used by itself; it inherits from child classes and recognizes
 * the values of child fields.
 *
 * @param Entity The class type of child class only, which is inherited
 * @since 2.0.0
 * @author ruskonert
 */
@Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")
open class IObject<Entity : IObject<Entity>> : PeriodicObject<IObjectHandler<*>>(UNREFERENCED_UNIQUE_ID)
{
    // The value of class of referenced type at the child.
    @AtomicExceptField
    private var reference : Type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]

    fun getReference() : Class<Entity>
    {
        return this.reference as Class<Entity>
    }

    /**
     * Updates to the entity collection, which sames the type of this entity.
     * This will be have the unique id, It cans distinguishing the object compare with others.
     * If the task of hooked collection is enabled, the collection can detect it that about
     * changed this entity.
     *
     * @param containable Determines the collection of this entity's base can be built to the database form
     * @return Return this entity that was completed setting the reference to the collection
     */
    open fun create(containable : Boolean = true) : Entity
    {
        val obj = this
        IObjectCollector.setReference(obj, containable)
        return this as Entity
    }

    /**
     * Creates the entity without hooking to the collection. It is recommended when
     * it just uses for singleton-based. it has no unique id & needs to add the
     * identifier field name that can be specified.
     *
     */
    open fun createIndependent(isUnreferenced: Boolean = true) : Entity
    {
        val obj = this
            if(!isUnreferenced) IObjectCollector.setReference(obj)
            var clz : Class<out IObject<*>>? = obj::class.java.superclass as Class<out IObject<*>>
            while(clz != null)  {
                for(f in clz.declaredFields) {
                    f.isAccessible = true
                    if(f.name == "uid" && f.declaringClass == IObject::class.java) {
                        IObjectUtil.changeAnnotationValue(f.getDeclaredAnnotation(AtomicExceptField::class.java), "IsExpected", false)
                        f.set(obj, UUID.randomUUID().toString())
                        break
                    }
                }
                clz = clz::class.java.superclass as? Class<out IObject<*>>
                if(clz == Any::class.java) {
                    break
                }
            }
        this.setEnable(Standard.STD_HANDLER)
        return this as Entity
    }
    

    /**
     *
     */
    @AtomicExceptField
    private var collection : IObjectCollector<Entity>? = null
    fun getIObjectCollector() : IObjectCollector<Entity>? = this.collection
    fun disconnectCollection() { this.collection = null }

    /**
     * Get entity from entity collection through the specific value.
     * To get the value, in Collection, It needs to register the field name in
     * identifier so you can find the Entity.
     * @param referenceObject Any values that can be referred to as clues or information
     *                        to get the Entity
     * @return Returns the applicable entity
     */
    open fun getEntity(referenceObject: Any?) : Entity?
    {
        val ref = this.collection ?: return null
        return ref.getEntity(referenceObject)
    }

    /**
     * Get the string that was serialized of this entity.
     */
    fun stringify(isPretty : Boolean = false) : String
    {
        val element = this.getSerializeElements()
        val builder = this.configureSerializeBuilder()
        if(isPretty) builder.setPrettyPrinting()
        return builder.create().toJson(element)
    }

    /**
     * Get the element of json object that was serialized of this entity.
     */
    fun getSerializeElements() : JsonElement
    {
        val jsonObject = JsonObject()
        for(field in this.getSerializableEntityFields()) {
            this.addPropertyOfField(jsonObject, field, this)
        }
        return jsonObject
    }


    private fun addPropertyOfField(jsonObject : JsonObject, field : Field, target : Any)
    {
        field.isAccessible = true
        val fieldName : String = field.name
        val value: Any? = try {
            field.get(target)
        }
        catch(_ : IllegalArgumentException){ null }
        catch(_ : IllegalAccessException)  { null }
        if(value == null) {
            jsonObject.add(fieldName, JsonNull.INSTANCE)
            return
        }
        IObjectUtil.setProperty(jsonObject, fieldName, value, this.configureSerializeBuilder(), this.ignoreTransient)
    }


    fun getSerializableEntityFields(specific : List<String>? = null) : Iterable<Field>
    {
        val fieldList = this.getValuableFields(this.ignoreTransient)
        return if(specific == null) fieldList
        else fieldList.filter { field: Field -> specific.contains(field.name)  }
    }


    private fun getValuableFields(ignoreTransient: Boolean) : Iterable<Field>
    {
        val fList = ArrayList<Field>()
        val fields = this.getDeclareValidFields().toMutableList()

        val uidField = IObjectCollector.findIObjectField(this::class.java, "uid")
                ?: throw RuntimeException("Not found 'uid' field! Doesn't something wrong?")
        val atomicExceptField = (uidField.annotations[0] as? AtomicExceptField) ?: throw RuntimeException("Oh no, something was changed by one")
        if(atomicExceptField.IsExpected) fields.add(uidField)
        for(f in fields)
        {
            if(ignoreTransient) {
                // The field that was annotated it is no need to serialize because they just use for internal.
                // The named 'Companion' is default object class of Kotlin, which collected the static fields & methods.
                // Therefore no need to serialize this field.
                if (!(f.type.name.endsWith("\$Companion") || IObjectUtil.isInternalField(f))) fList.add(f)
            }
            else {
                try {
                    f.isAccessible = true
                    if(f.type.name.endsWith("\$Companion")) continue
                    // It can't be ignored the transient annotation, Because 'ignoreTransient' is false.
                    if(!Modifier.isTransient(f.modifiers) && !IObjectUtil.isInternalField(f)) fList.add(f)
                }
                catch(e : NoSuchFieldException) {
                    e.printStackTrace()
                }
            }
        }
        return fList
    }

    /**
     * The string of child's class name.
     */
    private var referencedFor : String

    @AtomicExceptField
    private val serializeAdapters : MutableList<SerializeAdapter<*>>
    fun getSerializeAdapters() : List<SerializeAdapter<*>> = this.serializeAdapters

    private fun getDefaultAdapterInit() : Array<out SerializeAdapter<*>>
    {
        return try {
            getDefaultAdapter()
        }
        catch(e: NotImplementedError) {
            getDefaultAdapter()
        }
    }

    init
    {
        this.serializeAdapters = this.getDefaultAdapterInit().toMutableList()
        this.serializeAdapters.add(DefaultSerializer.createWithSelection(this.getReference()))
        this.referencedFor = this.reference.typeName
    }

    /**
     * Gets & Configure the gson builder for IObject Type.
     */
    fun configureSerializeBuilder(targetBuilder : GsonBuilder = GsonBuilder()) : GsonBuilder
    {
        for(adapter in this.getSerializeAdapters()) {
            targetBuilder.registerTypeAdapter(adapter.getReference(), adapter)

            // DefaultSerializer's target is IObject class, but scope is Any.
            // It needs to define the class for accurate serializing.
            if(adapter is DefaultSerializer) {
                targetBuilder.registerTypeAdapter(this.getReference(), adapter)
            }
        }
        return targetBuilder.serializeNulls()
    }

    fun registerSerializeAdapter(vararg adapter : SerializeAdapter<*>)
    {
        this.serializeAdapters.addAll(adapter)
    }

    fun registerSerializeAdapter(vararg adapters : KClass<out SerializeAdapter<*>>)
    {
        for(kClazz in adapters) {
            val adapterConstructor = kClazz.primaryConstructor
            if(adapterConstructor != null) {
                // The constructor of adapter must be have the empty parameter.
                if(adapterConstructor.parameters.isEmpty()) {
                    this.serializeAdapters.add(adapterConstructor.call())
                }
            }
        }
    }

    fun registerSerializeAdapter(vararg adapters : Class<out SerializeAdapter<*>>)
    {
        for(kClazz in adapters) {
            val adapterConstructor = kClazz.constructors[0]
            if(adapterConstructor != null) {
                // The constructor of adapter must be have the empty parameter.
                if(adapterConstructor.parameters.isEmpty()) {
                    this.serializeAdapters.add(adapterConstructor.newInstance() as SerializeAdapter<*>)
                }
            }
        }
    }

    @AtomicExceptField
    private var ignoreTransient : Boolean = false
    fun disableTransient(status : Boolean) { this.ignoreTransient = status }

    fun applyFromBaseElement(serialize : String)
    {
        val fields = JsonParser.parseString(serialize)
        return this.applyFromBaseElement(fields)
    }

    fun applyFromBaseElement(fields : JsonElement)
    {
        val instance = IObjectCollector.deserializeFromClass(fields, this::class.java)
                ?: throw RuntimeException("Cannot create new instance from deserializeFromClass Class<${this::class.java.name}> function")
        return this.applyFromBaseElement(instance)
    }

    fun applyFromBaseElement(victim : IObject<Entity>)
    {
        if(victim::class.java == this::class.java) {
            for (k in victim.getSerializableEntityFields()) {
                IObjectUtil.applyField(k, victim, this)
            }
        }
    }

    override fun hashCode(): Int
    {
        var result = reference.hashCode()
        result = 31 * result + this.getUniqueId().hashCode()
        result = 31 * result + (collection?.hashCode() ?: 0)
        result = 31 * result + serializeAdapters.hashCode()
        result = 31 * result + ignoreTransient.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean
    {
        if(other is IObject<*>) {
           return this.stringify() == other.stringify()
        }
        return false
    }

    override fun toString(): String
    {
        return this.stringify()
    }

    open fun subCompleted(handleInstance : Any? = null) : Boolean
    {
        return true
    }

    companion object
    {
        /**
         * Gets the entity from handled collection.
         * When you import an Entity, use 'previous' to get the best matching object.
         * Returns null if nothing else is available.
         * @param previous
         * @param body
         * @return The entity which is selected by collector
         */
        fun <U : IObject<U>> getEntity(previous: Any?, body: Class<U>) : U?
        {
            val collector = IObjectCollector.getEntityCollection(body)
            return collector?.getEntity(previous)
        }

        /**
         * Every object that has a IObject type has a unique ID.
         * This is a temporary value that is used when the object has
         * not yet been registered in collection.
         */
        @AtomicExceptField
        const val UNREFERENCED_UNIQUE_ID = "Please_call_the_method_#create_if_you_want_to_identity"

        /**
         * Gets adapters as default for IObject type.
         * It needs to register for that constructing the GsonBuilder or specifically
         * de/serialization. It also uses to initialize for the serializeAdapter field
         * if the customize adapter is not implemented.
         */
        fun getDefaultAdapter() : Array<out SerializeAdapter<*>>
        {
            // There's no adapter. It will be added in the future.
            return arrayOf()
        }

        /**
         * Gets adapters as default for specific IObject type.
         * It needs to register for that constructing the GsonBuilder or specifically
         * de/serialization.
         */
        fun getDefaultAdapter(clazzType : Class<out IObject<*>>) : List<SerializeAdapter<*>>
        {
            return (clazzType.constructors[0].newInstance() as IObject<*>).serializeAdapters
        }

        /**
         * Gets default gsonBuilder from IObject type.
         */
        fun getBuilderWithAdapter(clazzType: Class<out IObject<*>>) : GsonBuilder
        {
            return (clazzType.constructors[0].newInstance() as IObject<*>).configureSerializeBuilder()
        }
    }
}