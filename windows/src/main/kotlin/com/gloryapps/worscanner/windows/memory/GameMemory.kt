package com.gloryapps.worscanner.windows.memory

import com.gloryapps.worscanner.scanner.senses.Memory
import com.gloryapps.worscanner.scanner.senses.Span
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.BaseTSD.SIZE_T
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinError
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.ptr.IntByReference
import com.sun.jna.Memory as Buffer

/** The memory of the game's process, opened only to be read; closed once read. */
internal class GameMemory private constructor(private val process: WinNT.HANDLE) : Memory, AutoCloseable {
    override fun spans(): List<Span> = regions(WRITABLE)

    override fun readOnly(): List<Span> = regions(READ_ONLY)

    /** Every committed region whose protection is one of `protections`, up to [MAX_SPAN] each. */
    private fun regions(protections: Set<Int>): List<Span> = buildList {
        val info = WinNT.MEMORY_BASIC_INFORMATION()
        var at = 0L
        while (at < TOP && Kernel32.INSTANCE.VirtualQueryEx(process, Pointer(at), info, SIZE_T(info.size().toLong())).toLong() != 0L) {
            val start = Pointer.nativeValue(info.baseAddress)
            val size = info.regionSize.toLong()
            if (info.state.toInt() == WinNT.MEM_COMMIT && info.protect.toInt() in protections && size <= MAX_SPAN) add(Span(start, size))
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

    /** Windows would not open the game's process: `code` is its error. */
    class Refused(val code: Int) : Exception("Windows would not open the game's memory, error $code")

    /** Windows keeps the game's process from the scanner because the game runs as administrator and the scanner does not. */
    class Elevated : Exception("The game runs as administrator and the scanner does not")

    companion object {
        /** The memory of the process drawing the window `window`. */
        fun of(window: Long): Result<GameMemory> {
            val id = IntByReference()
            User32.INSTANCE.GetWindowThreadProcessId(HWND(Pointer(window)), id)

            return ofProcess(id.value)
        }

        fun ofProcess(id: Int): Result<GameMemory> {
            val process = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_INFORMATION or WinNT.PROCESS_VM_READ, false, id)
                ?: return Result.failure(refusal(Kernel32.INSTANCE.GetLastError()))

            return Result.success(GameMemory(process))
        }

        private fun refusal(code: Int): Exception =
            if (code == WinError.ERROR_ACCESS_DENIED && !Advapi32Util.isCurrentProcessElevated()) Elevated() else Refused(code)

        private const val TOP = 0x7FFF_FFFF_FFFFL
        private const val MAX_SPAN = 1L shl 30
        private val WRITABLE = setOf(WinNT.PAGE_READWRITE, WinNT.PAGE_EXECUTE_READWRITE)
        private val READ_ONLY = setOf(WinNT.PAGE_READONLY, WinNT.PAGE_WRITECOPY)
    }
}
