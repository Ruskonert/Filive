package com.newmcs.filive.concurrent

import com.newmcs.filive.atomic.AtomicAbstractObject
import com.newmcs.filive.atomic.misc.AtomicExceptField
import com.newmcs.filive.atomic.misc.Unique
import kotlinx.coroutines.Runnable
import org.apache.commons.lang3.mutable.MutableBoolean

/**
 * PeriodicObject executes some task that can be sustainably repeat.
 */
abstract class PeriodicObject<HandlerType>(uniqueId: String) : AtomicAbstractObject<HandlerType>(), Unique
{
    /**
     * A identity unit for judging specific object among the various objects.
     */
    @AtomicExceptField(IsExpected = true)
    private val uid : String = uniqueId

    /**
     * Gets the unique id.
    * @return the identity unit
    */
    final override fun getUniqueId(): String = this.uid

    /**
     *
     */
    @AtomicExceptField
    protected var sustainTask0 : Runnable? = null; private set

    /**
     *
     */
    fun getSustainTask() : Runnable? = this.sustainTask0

    /**
     *
     */
    fun setSustainTask(r : Runnable) { this.sustainTask0 = r }


    /**
     *
     */
    @AtomicExceptField
    private var sustainTaskIsRunning = MutableBoolean(true)

    /**
     *
     */
    fun isRunning() : Boolean {
        this.getSustainTask() ?: return false
        return this.sustainTaskIsRunning.booleanValue()
    }

    /**
     *
     */
    @Throws(RuntimeException::class)
    override fun setEnable(enable: Boolean)
    {
        if(enable) {
            val handler = this.getHandler() ?: throw RuntimeException("You need to set value of handler type before enable the object")
            val thread = Thread(Runnable { while(sustainTaskIsRunning.isTrue) { this.sustainImpl(handler) } })
            thread.start()
            this.sustainTask0 = thread
        }
        else {
            this.sustainTaskIsRunning.setValue(false)
            this.sustainTask0 = null
        }
    }
}
