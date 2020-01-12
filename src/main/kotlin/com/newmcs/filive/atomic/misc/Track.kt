package com.newmcs.filive.atomic.misc

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Track(val reference : String = "com.wboxonline.filive.atomic.event.example.AtomicDefaultTracker",
                       val target : String)
