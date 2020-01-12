package com.newmcs.filive.concurrent

import com.newmcs.filive.handler.Handle

abstract class EventPapyrus<T> : Handle<T>
{
    private var handler : T? = null
    override fun setEnable(enable: Boolean)
    {
        if(!enable) this.handler = null
    }

    override fun setEnable(handleInstance: T?)
    {
        this.handler = handleInstance
        return this.setEnable(this.handler != null)
    }

    abstract fun callEvent(handleInstance: T)

    override fun isEnabled(): Boolean = this.handler != null
}