package com.newmcs.filive.atomic

import com.newmcs.filive.atomic.event.AtomicMonitor
import com.newmcs.filive.atomic.event.AtomicMonitorEventImpl
import com.newmcs.filive.atomic.misc.AtomicExceptField
import com.newmcs.filive.atomic.misc.Track
import com.newmcs.filive.handler.IObjectHandler
import com.newmcs.filive.handler.ISustainable
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

abstract class AtomicAbstractObject<HandlerType> protected constructor() : ISustainable<HandlerType>, IObjectHandler<HandlerType?>
{
    init {
        this.onCreate()
    }

    protected open fun onCreate() : Boolean { return true }

    protected fun getDeclareValidFields() : List<Field>
    {
        val targetClasses = this::class.superclasses
        val declaredFieldList = ArrayList<Field>()
        val kExtractFieldFunction = fun(clazz : KClass<*>) {
            for(field in clazz.java.declaredFields) {
                field.isAccessible = true
                if(!Modifier.isStatic(field.modifiers)) {
                    declaredFieldList.add(field)
                }
            }
        }

        for(targetClazz in targetClasses) kExtractFieldFunction(targetClazz)
        kExtractFieldFunction(this::class)
        return declaredFieldList
    }

    protected open fun sustainOnPreLoad()
    {
        val declaredFieldList = this.getDeclareValidFields()
        for(field in declaredFieldList) {
            val initialValue : Any? = try {
                field.get(this)
            } catch(e : NullPointerException) {
                null
            }
            fieldList0.add(field)
            fieldReflectionList0.putIfAbsent(field, initialValue)
        }
    }

    private var isSustainLoaded : Boolean = false
    fun checkIsSustainLoaded() : Boolean = this.isSustainLoaded

    protected fun sustainImpl(handleInstance: Any?)
    {
        if(! this.isSustainLoaded) {
            this.isSustainLoaded = true
            this.sustainOnPreLoad()
        }
        this.sustain(handleInstance)
    }

    override fun sustain(handleInstance: Any?)
    {
        for(fieldEntry in fieldList0) {
            val reflectionValue = fieldReflectionList0.getValue(fieldEntry)
            val watchedValue = fieldEntry.get(this)

            if(reflectionValue != watchedValue) {
                this.changedAllListener(fieldEntry, reflectionValue, watchedValue, this)
                if(!isAnnotationField<AtomicExceptField>(fieldEntry)) {
                    if(this.getHandler() != null) {
                        val atomicEvent = AtomicMonitorEventImpl<HandlerType>(this, reflectionValue, watchedValue)
                        atomicEvent.callEvent(this.objectHandler!!)

                        // Monitor event can be deactivated externally. If canceled, the code below will not be called.
                        if(atomicEvent.isEnabled()) {
                            this.changedListener(fieldEntry, reflectionValue, atomicEvent.newValue, this)
                            fieldReflectionList0[fieldEntry] = atomicEvent.newValue
                        }
                        else {
                            fieldEntry.set(this, reflectionValue)
                        }
                    }
                }
            }
            else {
                if(isAnnotationField<Track>(fieldEntry)) {
                    val anno = fieldEntry.getAnnotation(Track::class.java)
                    val clazz = Class.forName(anno.reference)
                    val referenceMethod = clazz.getMethod(anno.target, fieldEntry.type, fieldEntry.type)
                    val result = referenceMethod.invoke(clazz.constructors[0].newInstance(), reflectionValue, watchedValue) as Boolean
                    if(result) {
                        val atomicEvent = AtomicMonitorEventImpl<HandlerType>(this, reflectionValue, watchedValue)
                        atomicEvent.callEvent(this.objectHandler!!)
                        this.changedListener(fieldEntry, reflectionValue, atomicEvent.newValue, this)

                        // Monitor event can be deactivated externally. If canceled, the code below will not be called.
                        if(atomicEvent.isEnabled()) {
                            this.changedListener(fieldEntry, reflectionValue, atomicEvent.newValue, this)
                            fieldReflectionList0[fieldEntry] = atomicEvent.newValue
                        }
                        else {
                            fieldEntry.set(this, reflectionValue)
                        }
                    }
                }
            }
        }
    }

    @Override
    override fun isEnabled(): Boolean
    {
        return this.objectHandler != null
    }

    override fun setEnable(handleInstance: HandlerType?)
    {
        this.objectHandler = handleInstance
        this.setEnable(this.objectHandler != null)
    }

    /**
     * There is nothing in that method, only check nullable type.
     * In addition, calling this method is more stable than calling
     * it with a Boolean type.
     */
    override fun setEnable(enable: Boolean)
    {

    }

    @AtomicExceptField
    private var objectHandler : HandlerType? = null
    override fun getHandler(): HandlerType? = this.objectHandler

    @AtomicExceptField
    private val fieldList0 = ArrayList<Field>()

    @AtomicExceptField
    private val fieldReflectionList0 = HashMap<Field, Any?>()

    companion object
    {
        /**
         * Determines the Field is non-atomic value.
         * This is considered when the annotation is referenced or it is not null.
         *
         * @param f The field that you want to check that is internal value
         * @return returns true if the field is internal value, otherwise false
         */
        private inline fun <reified A : Annotation> isAnnotationField(f: Field): Boolean
        {
            if(f.isAnnotationPresent(A::class.java)) {
                return f.getAnnotation(A::class.java) != null
            }
            return false
        }

        private val handlerList = ConcurrentHashMap<Class<out AtomicAbstractObject<*>>, ArrayList<AtomicMonitor>>()
        fun getHandlerList() : Map<Class<out AtomicAbstractObject<*>>, List<AtomicMonitor>> = handlerList
        fun registerMonitorHandler(handlerType : Class<out AtomicAbstractObject<*>>, monitor: AtomicMonitor)
        {
            var list = handlerList[handlerType]
            if(list == null) {
                list = ArrayList()
                with(handlerList) { putIfAbsent(handlerType, list) }
            }
            list.add(monitor)
        }
    }

    open fun changedAllListener(field : Field, previous: Any?, after: Any?, handleInstance: Any?) {}
    open fun changedListener(field : Field, previous : Any?, after : Any?, handleInstance: Any?) {}
}