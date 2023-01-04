package com.simplemobiletools.calendar.pro.helpers

import android.content.ContentValues.TAG
import android.os.Build
import android.provider.CalendarContract.Events
import android.util.Log
import androidx.annotation.RequiresApi
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.AES
import com.simplemobiletools.calendar.pro.extensions.calDAVHelper
import com.simplemobiletools.calendar.pro.extensions.eventTypesDB
import com.simplemobiletools.calendar.pro.helpers.IcsExporter.ExportResult.EXPORT_FAIL
import com.simplemobiletools.calendar.pro.helpers.IcsExporter.ExportResult.EXPORT_OK
import com.simplemobiletools.calendar.pro.helpers.IcsExporter.ExportResult.EXPORT_PARTIAL
import com.simplemobiletools.calendar.pro.models.CalDAVCalendar
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.writeLn
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter

class IcsExporter {
    enum class ExportResult {
        EXPORT_FAIL, EXPORT_OK, EXPORT_PARTIAL
    }

    private val MAX_LINE_LENGTH = 75
    private var eventsExported = 0
    private var eventsFailed = 0
    private var calendars = ArrayList<CalDAVCalendar>()
    private val outputContent = mutableListOf<String>()

    @RequiresApi(Build.VERSION_CODES.O)
    fun exportEvents(
        activity: BaseSimpleActivity,
        outputStream: OutputStream?,
        events: ArrayList<Event>,
        showExportingToast: Boolean,
        callback: (result: ExportResult) -> Unit
    ) {
        if (outputStream == null) {
            callback(EXPORT_FAIL)
            return
        }

        ensureBackgroundThread {
            val reminderLabel = activity.getString(R.string.reminder)
            val exportTime = Formatter.getExportedTime(System.currentTimeMillis())

            calendars = activity.calDAVHelper.getCalDAVCalendars("", false)
            if (showExportingToast) {
                activity.toast(R.string.exporting)
            }


            object : BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8)) {
                val lineSeparator = "\r\n"

                /**
                 * Writes a line separator. The line separator string is defined by RFC 5545 in 3.1. Content Lines:
                 * Content Lines are delimited by a line break, which is a CRLF sequence (CR character followed by LF character).
                 *
                 * @see <a href="https://icalendar.org/iCalendar-RFC-5545/3-1-content-lines.html">RFC 5545 - 3.1. Content Lines</a>
                 */
                override fun newLine() {
                    write(lineSeparator)
                }
            }.use { out ->
                outputContent.add(BEGIN_CALENDAR)
                outputContent.add(CALENDAR_PRODID)
                outputContent.add(CALENDAR_VERSION)
                for (event in events) {
                    outputContent.add(BEGIN_EVENT)
                    event.title.replace("\n", "\\n").let { if (it.isNotEmpty()) outputContent.add("$SUMMARY:$it") }
                    event.importId.let { if (it.isNotEmpty()) outputContent.add("$UID$it") }
                    event.eventType.let { outputContent.add("$CATEGORY_COLOR${activity.eventTypesDB.getEventTypeWithId(it)?.color}") }
                    event.eventType.let { outputContent.add("$CATEGORIES${activity.eventTypesDB.getEventTypeWithId(it)?.title}") }
                    event.lastUpdated.let { outputContent.add("$LAST_MODIFIED:${Formatter.getExportedTime(it)}") }
                    event.location.let { if (it.isNotEmpty()) outputContent.add("$LOCATION:$it") }
                    event.availability.let { outputContent.add("$TRANSP${if (it == Events.AVAILABILITY_FREE) TRANSPARENT else OPAQUE}") }

                    if (event.getIsAllDay()) {
                        outputContent.add("$DTSTART;$VALUE=$DATE:${Formatter.getDayCodeFromTS(event.startTS)}")
                        outputContent.add("$DTEND;$VALUE=$DATE:${Formatter.getDayCodeFromTS(event.endTS + TWELVE_HOURS)}")
                    } else {
                        event.startTS.let { outputContent.add("$DTSTART:${Formatter.getExportedTime(it * 1000L)}") }
                        event.endTS.let { outputContent.add("$DTEND:${Formatter.getExportedTime(it * 1000L)}") }
                    }
                    event.hasMissingYear().let { outputContent.add("$MISSING_YEAR${if (it) 1 else 0}") }

                    outputContent.add("$DTSTAMP$exportTime")
                    outputContent.add("$STATUS$CONFIRMED")
                    Parser().getRepeatCode(event).let { if (it.isNotEmpty()) outputContent.add("$RRULE$it") }

                    fillDescription(event.description.replace("\n", "\\n"), out)
                    fillReminders(event, out, reminderLabel)
                    fillIgnoredOccurrences(event, out)

                    eventsExported++
                    outputContent.add(END_EVENT)
                }
                outputContent.add(END_CALENDAR)
//                val outPut = Base64.encodeToString(outputContent.joinToString("\n").toByteArray(), Base64.DEFAULT)
                val outPut = AES.encrypt(outputContent.joinToString("\n"))
                Log.d(TAG, "exportEvents: $outPut")
                out.write(outPut)
            }

            callback(
                when {
                    eventsExported == 0 -> EXPORT_FAIL
                    eventsFailed > 0 -> EXPORT_PARTIAL
                    else -> EXPORT_OK
                }
            )
        }
    }

    private fun fillReminders(event: Event, out: BufferedWriter, reminderLabel: String) {
        event.getReminders().forEach {
            val reminder = it
            outputContent.apply {
                add(BEGIN_ALARM)
                add("$DESCRIPTION$reminderLabel")
                if (reminder.type == REMINDER_NOTIFICATION) {
                    add("$ACTION$DISPLAY")
                } else {
                    add("$ACTION$EMAIL")
                    val attendee = calendars.firstOrNull { it.id == event.getCalDAVCalendarId() }?.accountName
                    if (attendee != null) {
                        add("$ATTENDEE$MAILTO$attendee")
                    }
                }

                val sign = if (reminder.minutes < -1) "" else "-"
                add("$TRIGGER:$sign${Parser().getDurationCode(Math.abs(reminder.minutes.toLong()))}")
                add(END_ALARM)
            }
        }
    }

    private fun fillIgnoredOccurrences(event: Event, out: BufferedWriter) {
        event.repetitionExceptions.forEach {
            out.writeLn("$EXDATE:$it")
        }
    }

    private fun fillDescription(description: String, out: BufferedWriter) {
        var index = 0
        var isFirstLine = true

        while (index < description.length) {
            val substring = description.substring(index, Math.min(index + MAX_LINE_LENGTH, description.length))
            if (isFirstLine) {
                outputContent.add("$DESCRIPTION$substring")
            } else {
                outputContent.add("\t$substring")
            }

            isFirstLine = false
            index += MAX_LINE_LENGTH
        }
    }
}
