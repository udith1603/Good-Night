package com.yourname.gnight

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Schedule(
    val id: String,
    val phone: String,
    val message: String,
    val hour: Int,
    val minute: Int,
    val repeat: Boolean,
    val enabled: Boolean
)

object ScheduleStore {
    private const val PREFS = "gnight_prefs"
    private const val KEY = "schedules"

    fun load(context: Context): List<Schedule> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<Schedule>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                Schedule(
                    id = o.getString("id"),
                    phone = o.getString("phone"),
                    message = o.getString("message"),
                    hour = o.getInt("hour"),
                    minute = o.getInt("minute"),
                    repeat = o.optBoolean("repeat", false),
                    enabled = o.optBoolean("enabled", true)
                )
            )
        }
        return list
    }

    fun save(context: Context, list: List<Schedule>) {
        val arr = JSONArray()
        list.forEach { s ->
            val o = JSONObject()
            o.put("id", s.id)
            o.put("phone", s.phone)
            o.put("message", s.message)
            o.put("hour", s.hour)
            o.put("minute", s.minute)
            o.put("repeat", s.repeat)
            o.put("enabled", s.enabled)
            arr.put(o)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, arr.toString())
            .apply()
    }
}
