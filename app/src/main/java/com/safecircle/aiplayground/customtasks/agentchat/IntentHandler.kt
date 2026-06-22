package com.safecircle.aiplayground.customtasks.agentchat

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Instances
import android.util.Log
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.net.toUri
import com.safecircle.aiplayground.notifications.NotificationScheduleManagerEntryPoint
import com.safecircle.aiplayground.proto.ScheduledNotification
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import dagger.hilt.android.EntryPointAccessors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@JsonClass(generateAdapter = true)
data class SendEmailParams(
  val extra_email: String,
  val extra_subject: String,
  val extra_text: String,
)

@JsonClass(generateAdapter = true)
data class SendSmsParams(val phone_number: String, val sms_body: String)

@JsonClass(generateAdapter = true)
data class CreateCalendarEventParams(
  val title: String,
  val description: String,
  val begin_time: String,
  val end_time: String,
)

@JsonClass(generateAdapter = true) data class ReadCalendarEventsParams(val date: String)

@JsonClass(generateAdapter = true)
data class CalendarEventDto(
  val title: String,
  val description: String,
  val begin_time: String,
  val end_time: String,
)

@JsonClass(generateAdapter = true)
data class ReadCalendarEventsResponse(val events: List<CalendarEventDto>)

enum class IntentAction(val action: String) {
  SEND_EMAIL("send_email"),
  SEND_SMS("send_sms"),
  CREATE_CALENDAR_EVENT("create_calendar_event"),
  READ_CALENDAR_EVENTS("read_calendar_events"),
  GET_CURRENT_DATE_AND_TIME("get_current_date_and_time"),
  SCHEDULE_NOTIFICATION("schedule_notification");

  companion object {
    fun from(action: String): IntentAction? = entries.find { it.action == action }
  }
}

@JsonClass(generateAdapter = true)
data class ScheduleNotificationParams(
  val title: String,
  val message: String,
  val hour: Int,
  val minute: Int,
  val deeplink: String? = null,
  val task_id: String? = null,
  val model_name: String? = null,
  val year: Int? = null,
  val month: Int? = null,
  val day: Int? = null,
  val repeat_daily: Boolean? = null,
)

object IntentHandler {
  private const val TAG = "AGIntentHandler"

  suspend fun handleAction(
    context: Context,
    action: String,
    parameters: String,
    requestPermission: suspend (String) -> Boolean,
  ): String {
    return when (IntentAction.from(action)) {
      IntentAction.SEND_EMAIL -> {
        try {
          val moshi = Moshi.Builder().build()
          val jsonAdapter = moshi.adapter(SendEmailParams::class.java)
          val params = jsonAdapter.fromJson(parameters)
          if (params != null) {
            val intent =
              Intent(Intent.ACTION_SEND).apply {
                data = "mailto:".toUri()
                type = "text/plain"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(params.extra_email))
                putExtra(Intent.EXTRA_SUBJECT, params.extra_subject)
                putExtra(Intent.EXTRA_TEXT, params.extra_text)
              }
            context.startActivity(intent)
            "succeeded"
          } else {
            Log.e(TAG, "Failed to parse send_email parameters: $parameters")
            "failed"
          }
        } catch (e: Exception) {
          Log.e(TAG, "Failed to parse send_email parameters: $parameters", e)
          "failed"
        }
      }
      IntentAction.SEND_SMS -> {
        try {
          val moshi = Moshi.Builder().build()
          val jsonAdapter = moshi.adapter(SendSmsParams::class.java)
          val params = jsonAdapter.fromJson(parameters)
          if (params != null) {
            val uri = "smsto:${params.phone_number}".toUri()
            val intent = Intent(Intent.ACTION_SENDTO, uri)
            intent.putExtra("sms_body", params.sms_body)
            context.startActivity(intent)
            "succeeded"
          } else {
            Log.e(TAG, "Failed to parse send_sms parameters: $parameters")
            "failed"
          }
        } catch (e: Exception) {
          Log.e(TAG, "Failed to parse send_sms parameters: $parameters", e)
          "failed"
        }
      }
      IntentAction.CREATE_CALENDAR_EVENT -> {
        try {
          val moshi = Moshi.Builder().build()
          val jsonAdapter = moshi.adapter(CreateCalendarEventParams::class.java)
          val params = jsonAdapter.fromJson(parameters)
          if (params != null) {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val beginTimeMillis = format.parse(params.begin_time)?.time ?: 0L
            val endTimeMillis = format.parse(params.end_time)?.time ?: 0L
            val intent =
              Intent(Intent.ACTION_INSERT).apply {
                data = Events.CONTENT_URI
                putExtra(Events.TITLE, params.title)
                putExtra(Events.DESCRIPTION, params.description)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTimeMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTimeMillis)
              }
            context.startActivity(intent)
            "succeeded"
          } else {
            Log.e(TAG, "Failed to parse create_calendar_event parameters: $parameters")
            "failed"
          }
        } catch (e: Exception) {
          Log.e(TAG, "Failed to parse create_calendar_event parameters: $parameters", e)
          "failed"
        }
      }
      IntentAction.READ_CALENDAR_EVENTS -> {
        readCalendarEvents(context, parameters, requestPermission)
      }
      IntentAction.GET_CURRENT_DATE_AND_TIME -> {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss EEEE", Locale.getDefault())
        sdf.format(Date())
      }
      IntentAction.SCHEDULE_NOTIFICATION -> {
        scheduleNotification(context, parameters)
      }
      null -> "failed"
    }
  }

  private suspend fun readCalendarEvents(
    context: Context,
    parameters: String,
    requestPermission: suspend (String) -> Boolean,
  ): String {
    if (
      checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) !=
        android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {
      val granted = requestPermission(android.Manifest.permission.READ_CALENDAR)
      if (!granted) return "failed: READ_CALENDAR permission denied by user"
    }

    try {
      val moshi = Moshi.Builder().build()
      val jsonAdapter = moshi.adapter(ReadCalendarEventsParams::class.java)
      val params = jsonAdapter.fromJson(parameters) ?: return "failed"
      val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
      val dateObj = format.parse(params.date) ?: return "failed"

      val cal = Calendar.getInstance().apply {
        timeInMillis = dateObj.time
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
      val startOfDayMillis = cal.timeInMillis
      cal.apply {
        add(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MILLISECOND, -1)
      }
      val endOfDayMillis = cal.timeInMillis

      val projection = arrayOf(Instances.TITLE, Instances.DESCRIPTION, Instances.BEGIN, Instances.END)
      val builder = Instances.CONTENT_URI.buildUpon()
      android.content.ContentUris.appendId(builder, startOfDayMillis)
      android.content.ContentUris.appendId(builder, endOfDayMillis)

      val cursor = context.contentResolver.query(builder.build(), projection, null, null, "${Instances.BEGIN} ASC")
      val eventsList = mutableListOf<CalendarEventDto>()
      cursor?.use { c ->
        val titleIdx = c.getColumnIndex(Instances.TITLE)
        val descIdx = c.getColumnIndex(Instances.DESCRIPTION)
        val startIdx = c.getColumnIndex(Instances.BEGIN)
        val endIdx = c.getColumnIndex(Instances.END)
        val timeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        while (c.moveToNext()) {
          eventsList.add(
            CalendarEventDto(
              title = if (titleIdx >= 0) c.getString(titleIdx) ?: "" else "",
              description = if (descIdx >= 0) c.getString(descIdx) ?: "" else "",
              begin_time = if (startIdx >= 0) timeFormat.format(Date(c.getLong(startIdx))) else "",
              end_time = if (endIdx >= 0) timeFormat.format(Date(c.getLong(endIdx))) else "",
            )
          )
        }
      }
      return moshi.adapter(ReadCalendarEventsResponse::class.java).toJson(ReadCalendarEventsResponse(eventsList))
    } catch (e: Exception) {
      Log.e(TAG, "Failed to read calendar events: $parameters", e)
      return "failed: ${e.message}"
    }
  }

  private fun scheduleNotification(context: Context, parameters: String): String {
    try {
      val moshi = Moshi.Builder().build()
      val params = moshi.adapter(ScheduleNotificationParams::class.java).fromJson(parameters) ?: return "failed"

      val notificationProto = ScheduledNotification.newBuilder()
        .setId(java.util.UUID.randomUUID().toString())
        .setTitle(params.title)
        .setMessage(params.message)
        .setHour(params.hour)
        .setMinute(params.minute)
        .setChannelId("agent_skill_tasks_channel")
        .setChannelName("Agent Skill Task")

      val deeplink = when {
        params.deeplink != null -> params.deeplink
        params.task_id != null && params.model_name != null ->
          "com.safecircle.aiplayground://model/${params.task_id}/${params.model_name}"
            .toUri().buildUpon().appendQueryParameter("query", params.message).build().toString()
        params.task_id != null ->
          "com.safecircle.aiplayground://${params.task_id}/"
            .toUri().buildUpon().appendQueryParameter("query", params.message).build().toString()
        else ->
          "com.safecircle.aiplayground://llm_agent_chat/"
            .toUri().buildUpon().appendQueryParameter("query", params.message).build().toString()
      }
      notificationProto.setDeeplink(deeplink)

      params.year?.let { notificationProto.setYear(it) }
      params.month?.let { notificationProto.setMonth(it) }
      params.day?.let { notificationProto.setDay(it) }
      params.repeat_daily?.let { notificationProto.setRepeatDaily(it) }

      val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        NotificationScheduleManagerEntryPoint::class.java,
      )
      val success = entryPoint.notificationScheduleManager().scheduleNotification(notificationProto.build())
      return if (success) "succeeded" else "failed"
    } catch (e: Exception) {
      Log.e(TAG, "Failed to schedule notification: $parameters", e)
      return "failed"
    }
  }
}
