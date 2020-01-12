package com.newmcs.filive.handler

import com.newmcs.filive.application.ApplicationListener
import java.nio.file.Paths
import java.nio.file.Path

interface CollectionHandler : IObjectHandler<ApplicationListener>
{
    fun getPath() : Path {
        val currentRelativePath = Paths.get("")
        return currentRelativePath.toAbsolutePath()
    }

    fun getHandlerName() : String {
        return "DefaultCollectHandler"
    }
}