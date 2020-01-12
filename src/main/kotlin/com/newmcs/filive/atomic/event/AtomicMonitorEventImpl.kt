package com.newmcs.filive.atomic.event

import com.newmcs.filive.atomic.AtomicAbstractObject
import com.newmcs.filive.atomic.misc.AtomicHandler
import com.newmcs.filive.concurrent.EventPapyrus

data class AtomicMonitorEventImpl<T>(val target : AtomicAbstractObject<*>, val previousValue : Any?, var newValue : Any?) : EventPapyrus<T>()
{
    override fun callEvent(handleInstance: T)
    {
        this.setEnable(handleInstance)
        val handlerList = AtomicAbstractObject.getHandlerList()
        val handlerMonitors = handlerList[target::class.java] ?: return
        for(monitor in handlerMonitors) {
            val methods = monitor::class.java.declaredMethods
            for(method in methods) {
                if(method.isAnnotationPresent(AtomicHandler::class.java)) {
                    method.invoke(monitor, this, ArrayList<String>())
                }
            }
        }
    }
}