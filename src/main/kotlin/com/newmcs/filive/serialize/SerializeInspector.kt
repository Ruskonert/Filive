package com.newmcs.filive.serialize

import com.newmcs.filive.Standard
import com.newmcs.filive.concurrent.PeriodicObject
import com.newmcs.filive.IObject
import org.apache.commons.lang3.mutable.MutableBoolean
import java.io.IOException
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.CompletionHandler
import java.nio.charset.Charset
import java.nio.file.*
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

open class SerializeInspector<T>(private val iObject : IObject<*>) : PeriodicObject<T>(UUID.randomUUID().toString()), CompletionHandler<Int, IObject<*>>
{
    fun getReferenceType() : Class<out IObject<*>> = this.iObject.getReference()
    fun getInspectUid() : String = this.iObject.getUniqueId()

    private var executorService : ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    private var streamChannel : AsynchronousFileChannel? = null

    private lateinit var targetPath : Path
    fun getTargetPath() : Path = this.targetPath

    private val streamLock : MutableBoolean = MutableBoolean(false)
    fun asCompleted() { this.streamLock.setValue(true) }

    @Synchronized
    override fun sustainOnPreLoad()
    {
        try {
            val clazzPath = Paths.get(Standard.MAIN_FOLDER, Standard.OBJ_FOLDER, this.iObject.getReference().canonicalName)
            if (!Files.exists(clazzPath)) Files.createDirectories(clazzPath)

            val fileUri = Paths.get(Standard.MAIN_FOLDER, Standard.OBJ_FOLDER, this.iObject.getReference().canonicalName, this.iObject.getUniqueId() + ".json")
            if (!Files.exists(fileUri)) Files.createFile(fileUri)
            this.targetPath = fileUri
            this.streamChannel = AsynchronousFileChannel.open(fileUri, EnumSet.of(StandardOpenOption.WRITE), this.executorService)
        }
        catch (e : IOException)
        {
            e.printStackTrace()
        }
    }

    override fun sustain(handleInstance: Any?)
    {
        if(this.streamLock.isTrue) {
            synchronized(this) {
                if(this.streamChannel == null) {
                    this.streamLock.setValue(false)
                    return
                }
                else {
                    streamChannel?.write<IObject<*>>(
                            Charset.defaultCharset().encode(this.iObject.stringify(true)),
                            0,
                            this.iObject,
                            this)
                    this.streamLock.setValue(false)
                }
            }
        }
    }

    override fun completed(result: Int, attachment: IObject<*>)
    {
        println("[INFO] ${attachment.getUniqueId()} -> Synchronized file was updated!")
    }

    override fun failed(exc: Throwable, attachment: IObject<*>)
    {
        println("failed! Please check the exception")
        exc.printStackTrace()
    }
}