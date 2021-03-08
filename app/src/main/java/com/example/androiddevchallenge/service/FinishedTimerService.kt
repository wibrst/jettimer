/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androiddevchallenge.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.androiddevchallenge.R
import com.example.androiddevchallenge.manager.BeepManager
import com.example.androiddevchallenge.util.NotificationReceiver
import com.example.androiddevchallenge.util.ONE_THOUSAND_INT
import com.example.androiddevchallenge.util.TIMER_FINISH_RUNNING_ID
import com.example.androiddevchallenge.util.TimerState
import com.example.androiddevchallenge.util.ZERO_LONG
import com.example.androiddevchallenge.util.ZERO_STRING
import com.example.androiddevchallenge.util.getOpenTimerTabIntent
import com.example.androiddevchallenge.util.isOreoPlus
import com.example.androiddevchallenge.util.toHhMmSs
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

@AndroidEntryPoint
class FinishedTimerService : Service() {

    @Inject lateinit var beepManager: BeepManager
    private val bus = EventBus.getDefault()

    override fun onCreate() {
        super.onCreate()
        bus.register(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(TIMER_FINISH_RUNNING_ID, notification(ZERO_STRING))
        beepManager.playNotificationSound()
        Log.d("Hola", " Holi")
        return START_NOT_STICKY
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(state: TimerState.Running) {
        if (state.tick % ONE_THOUSAND_INT == ZERO_LONG) {
            val notification: Notification = notification(state.tick.toHhMmSs())
            val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            mNotificationManager.notify(TIMER_FINISH_RUNNING_ID, notification)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: FinishedTimerStopService) {
        stopService()
    }

    private fun stopService() {
        if (isOreoPlus()) {
            stopForeground(true)
        } else {
            stopSelf()
        }
        beepManager.stopNotificationSound()
    }

    override fun onDestroy() {
        super.onDestroy()
        bus.unregister(this)
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun notification(formattedTick: String): Notification {
        val channelId = "finished_alarm_timer"
        val label = getString(R.string.app_name)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (isOreoPlus()) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            NotificationChannel(channelId, label, importance).apply {
                setSound(null, null)
                notificationManager.createNotificationChannel(this)
            }
        }
        val broadcastIntent = Intent(application, NotificationReceiver::class.java)
        val broadcastPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(application, 0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(formattedTick)
            .setContentText(getString(R.string.label_time_up))
            .setSmallIcon(R.drawable.ic_timer)
            .setContentIntent(this.getOpenTimerTabIntent())
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(null)
            .setOngoing(true)
            .setAutoCancel(true)
            .setChannelId(channelId)
            .addAction(R.drawable.ic_timer, getString(R.string.label_stop), broadcastPendingIntent)
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        return builder.build()
    }
}

fun startFinishedTimerService(context: Context) {
    if (isOreoPlus()) {
        context.startForegroundService(Intent(context, FinishedTimerService::class.java))
    } else {
        context.startService(Intent(context, FinishedTimerService::class.java))
    }
}

fun stopFinishedTimerService(context: Context) {
    context.stopService(Intent(context, FinishedTimerService::class.java))
}

object FinishedTimerStopService
