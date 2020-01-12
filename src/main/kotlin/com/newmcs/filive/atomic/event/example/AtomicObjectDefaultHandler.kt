package com.newmcs.filive.atomic.event.example

import com.newmcs.filive.application.ApplicationListener
import com.newmcs.filive.atomic.event.AtomicMonitor
import com.newmcs.filive.atomic.event.AtomicMonitorEventImpl
import com.newmcs.filive.atomic.misc.AtomicHandler

open class AtomicObjectDefaultHandler : AtomicMonitor
{
    @AtomicHandler
    open fun onDefaultChanged(event : AtomicMonitorEventImpl<ApplicationListener>, eventArgs : List<String>)
    {
        /**
         * To do your code ...
         */
    }
}