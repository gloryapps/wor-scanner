package com.gloryapps.worscanner.windows.memory

import com.gloryapps.worscanner.scanner.senses.Memory
import com.gloryapps.worscanner.scanner.senses.Span
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.BaseTSD.SIZE_T
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.ptr.IntByReference
import com.sun.jna.Memory as Buffer

/** The memory of the game's process, opened only to be read; closed once read. */
internal class GameMemory private constructor(private val process: WinNT.HANDLE) : Memory, AutoCloseable {
    override fun spans(): List<Span> = buildList {
        val info = WinNT.MEMORY_BASIC_INFORMATION()
        var at = 0L
        while (at < TOP && Kernel32.INSTANCE.VirtualQueryEx(process, Pointer(at), info, SIZE_T(info.size().toLong())).toLong() != 0L) {
            val start = Pointer.nativeValue(info.baseAddress)
            val size = info.regionSize.toLong()
            if (info.state.toInt() == WinNT.MEM_COMMIT && info.protect.toInt() in WRITABLE && size <= MAX_SPAN) add(Span(start, size))
            if (start + size <= at) break
            at = start + size
        }
    }

    override fun read(address: Long, size: Int): ByteArray? {
        if (size == 0) return ByteArray(0)

        return Buffer(size.toLong()).use { buffer ->
            val read = IntByReference()
            val whole = Kernel32.INSTANCE.ReadProcessMemory(process, Pointer(address), buffer, size, read) && read.value == size
            if (whole) buffer.getByteArray(0, size) else null
        }
    }

    override fun close() {
        Kernel32.INSTANCE.CloseHandle(process)
    }

    /** Windows would not open the game's process: `code` is its error, 5 where the game runs as administrator and the app does not. */
    class Refused(val code: Int) : Exception("Windows would not open the game's memory, error $code")

    companion object {
        /** The memory of the process drawing the window `window`. */
        fun of(window: Long): Result<GameMemory> {
            val id = IntByReference()
            User32.INSTANCE.GetWindowThreadProcessId(HWND(Pointer(window)), id)

            return ofProcess(id.value)
        }

        fun ofProcess(id: Int): Result<GameMemory> {
            val process = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_INFORMATION or WinNT.PROCESS_VM_READ, false, id)

            return if (process == null) Result.failure(Refused(Kernel32.INSTANCE.GetLastError())) else Result.success(GameMemory(process))
        }

        private const val TOP = 0x7FFF_FFFF_FFFFL
        private const val MAX_SPAN = 1L shl 30
        private val WRITABLE = setOf(WinNT.PAGE_READWRITE, WinNT.PAGE_EXECUTE_READWRITE)
    }
}
