package com.example.jumpropecounter.Utils

import android.graphics.Bitmap


/**
 * Frame class
 * @param frame: Bitmap of the frame
 * @param seqNum: Sequence number of the frame
 * @param isStart: Boolean indicating if the frame is the start of the recording
 * @param isEnd: Boolean indicating if the frame is the end of the recording
 *
 * The start and end frame have to be indicated!
 */
class Frame (val frame : Bitmap, val seqNum : Int, val isStart : Boolean = false, val isEnd : Boolean = false) {
    private val _frame : Bitmap = frame
    private val _seqNum : Int = seqNum

    private val START : Boolean = isStart
    private val END : Boolean = isEnd

    fun getFrame() : Bitmap {
        return _frame
    }

    fun getSeqNum() : Int {
        return _seqNum
    }

    fun isStart() : Boolean {
        return START
    }

    fun isEnd() : Boolean {
        return END
    }
}