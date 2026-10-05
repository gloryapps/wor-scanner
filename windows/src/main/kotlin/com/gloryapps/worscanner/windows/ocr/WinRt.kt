package com.gloryapps.worscanner.windows.ocr

import com.sun.jna.Function
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.platform.win32.Guid
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.win32.StdCallLibrary
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/** combase.dll: WinRT's start, its class factories and its strings. */
private interface Combase : StdCallLibrary {
    fun RoInitialize(type: Int): Int

    fun RoGetActivationFactory(classId: Pointer, iid: Guid.REFIID, factory: PointerByReference): Int

    fun WindowsCreateString(source: WString, length: Int, string: PointerByReference): Int

    fun WindowsDeleteString(string: Pointer?): Int

    fun WindowsGetStringRawBuffer(string: Pointer?, length: IntByReference): Pointer?

    companion object {
        val combase: Combase by lazy { Native.load("combase", Combase::class.java) }
    }
}

/** `RO_INIT_MULTITHREADED`: WinRT objects made here may be called from any thread of the apartment. */
private const val MULTITHREADED = 1

/** The thread WinRT is started on and every OCR pass runs on. */
internal val WinRtThread = Executors.newSingleThreadExecutor { work ->
    Thread({ Combase.combase.RoInitialize(MULTITHREADED).ok("starting WinRT"); work.run() }, "wor-scanner-winrt").apply { isDaemon = true }
}.asCoroutineDispatcher()

/**
 * A WinRT object as its ABI exposes it: a pointer to a table of methods, each called with the object
 * first and returning an `HRESULT`. A slot is a method's place in that table, the six of
 * `IInspectable` (`QueryInterface`, `AddRef`, `Release` and three more) coming before an interface's own.
 */
internal class Inspectable(val pointer: Pointer) : AutoCloseable {
    fun call(slot: Int, vararg args: Any?): Int =
        Function.getFunction(pointer.getPointer(0).getPointer(slot.toLong() * Native.POINTER_SIZE)).invokeInt(arrayOf(pointer, *args))

    /** A method whose last argument is the object it hands back. */
    fun get(slot: Int, what: String, vararg args: Any?): Inspectable {
        val out = PointerByReference()
        call(slot, *args, out).ok(what)

        return Inspectable(checkNotNull(out.value) { "$what gave nothing" })
    }

    /** A method that hands back a string. */
    fun text(slot: Int, what: String): String {
        val out = PointerByReference()
        call(slot, out).ok(what)

        return try {
            val length = IntByReference()
            Combase.combase.WindowsGetStringRawBuffer(out.value, length)?.let { String(it.getCharArray(0, length.value)) }.orEmpty()
        } finally {
            Combase.combase.WindowsDeleteString(out.value)
        }
    }

    /** The same object under another of its interfaces. */
    fun query(iid: String, what: String): Inspectable = get(QUERY_INTERFACE, what, Guid.REFIID(Guid.IID(iid)))

    override fun close() {
        call(RELEASE)
    }

    private companion object {
        const val QUERY_INTERFACE = 0
        const val RELEASE = 2
    }
}

/** The factory of a WinRT class, under the interface `iid` names: its statics, or its constructors. */
internal fun factoryOf(className: String, iid: String): Inspectable {
    val name = PointerByReference()
    Combase.combase.WindowsCreateString(WString(className), className.length, name).ok("naming $className")
    try {
        val factory = PointerByReference()
        Combase.combase.RoGetActivationFactory(name.value, Guid.REFIID(Guid.IID(iid)), factory).ok("the factory of $className")

        return Inspectable(factory.value)
    } finally {
        Combase.combase.WindowsDeleteString(name.value)
    }
}

/** What an `IAsyncOperation<T>` comes to, waited for on this thread; one that runs past `WAIT_MS` is cancelled and fails. */
internal fun Inspectable.result(what: String): Inspectable {
    query(ASYNC_INFO, "$what's progress").use { info ->
        val until = System.nanoTime() + WAIT_MS * 1_000_000
        var status: Int
        do {
            val out = IntByReference()
            info.call(ASYNC_INFO_STATUS, out).ok("$what's status")
            status = out.value
            if (status == STARTED) {
                if (System.nanoTime() > until) {
                    info.call(ASYNC_INFO_CANCEL)
                    error("$what took longer than $WAIT_MS ms")
                }
                Thread.sleep(POLL_MS)
            }
        } while (status == STARTED)
        if (status != COMPLETED) {
            val code = IntByReference()
            info.call(ASYNC_INFO_ERROR_CODE, code)
            error("$what ended with status $status, 0x%08X".format(code.value))
        }
    }

    return get(ASYNC_OPERATION_GET_RESULTS, what)
}

/** An `HRESULT` that is not a failure, or the failure named. */
internal fun Int.ok(what: String) = check(this >= 0) { "$what failed: 0x%08X".format(this) }

private const val ASYNC_INFO = "{00000036-0000-0000-C000-000000000046}"
private const val ASYNC_INFO_STATUS = 7
private const val ASYNC_INFO_ERROR_CODE = 8
private const val ASYNC_INFO_CANCEL = 9
private const val ASYNC_OPERATION_GET_RESULTS = 8
private const val STARTED = 0
private const val COMPLETED = 1
private const val POLL_MS = 5L
private const val WAIT_MS = 10_000L
