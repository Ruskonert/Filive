package com.newmcs.filive

import com.newmcs.filive.application.ApplicationListener


sealed class Standard
{
    class UnspecifiedHandler private constructor() : ApplicationListener
    {
        override fun getHandler(): ApplicationListener = this
        companion object
        {
            val Instance : UnspecifiedHandler = UnspecifiedHandler()
        }
    }

    companion object
    {
        const val MAIN_FOLDER : String = "data"
        const val CLZ_LINK : String = "clz://"
        const val OBJ_FOLDER : String = "entities"
        val STD_HANDLER : UnspecifiedHandler = UnspecifiedHandler.Instance
    }
}
