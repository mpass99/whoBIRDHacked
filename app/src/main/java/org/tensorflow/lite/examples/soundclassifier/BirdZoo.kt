package org.tensorflow.lite.examples.soundclassifier

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews


private const val ACTION_SIMPLE_APP_WIDGET = "ACTION_SIMPLE_APP_WIDGET"
private const val TAG = "BIRD_ZOO"


/**
 * Implementation of App Widget functionality.
 */
class BirdZoo : AppWidgetProvider() {

    override fun onUpdate(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
    ) { // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int) {
        Log.i(TAG, "Starting updateAppWidget")

        val views = RemoteViews(context.packageName, R.layout.bird_zoo)
        val imageViews = listOf(R.id.imageView1, R.id.imageView2, R.id.imageView3, R.id.imageView4, R.id.imageView5, R.id.imageView6, R.id.imageView7, R.id.imageView8, R.id.imageView9, R.id.imageView10, R.id.imageView11, R.id.imageView12, R.id.imageView13, R.id.imageView14, R.id.imageView15, R.id.imageView16)
        val sounds = listOf(R.raw.bankivahuhn, R.raw.bekassine, R.raw.brauntinamu, R.raw.fettschwalm, R.raw.graugans, R.raw.graureiher, R.raw.helmkasuar, R.raw.klagetagschlaefer, R.raw.maeusebussard, R.raw.malaienschwalm, R.raw.nachtschwalbe, R.raw.nandu, R.raw.nordstreifenkiwi, R.raw.rostkappen_ameisenpitta, R.raw.strauss, R.raw.waldkauz)
        for (i in 0..15) {
            val view = imageViews[i]
            val sound = sounds[i]

            val intent = Intent(context, BirdZoo::class.java)
            intent.action = ACTION_SIMPLE_APP_WIDGET + "_" + sound.toString()
            intent.putExtra("sound", sound)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(view, pendingIntent)
        }
        appWidgetManager.updateAppWidget(appWidgetId, views)
        Log.i(TAG, "Finishing updateAppWidget")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.i(TAG, "Starting onReceive: action=${intent.action} hasExtra=${intent.hasExtra("sound")}")
        if (intent.action!!.startsWith(ACTION_SIMPLE_APP_WIDGET)) {
            val serviceIntent = Intent(context, SoundService::class.java)
            serviceIntent.putExtra("sound", intent.getIntExtra("sound", R.raw.graugans))
            context.startForegroundService(serviceIntent)

            // Log.i(TAG, "After starting Service in onReceive")
            // val views = RemoteViews(context.packageName, R.layout.bird_zoo)
            //val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.bankivahuhn)
            //views.setImageViewBitmap(R.id.imageView1, bitmap)
            //val appWidget = ComponentName(context, BirdZoo::class.java)
            //val appWidgetManager = AppWidgetManager.getInstance(context)
            //appWidgetManager.updateAppWidget(appWidget, views)

            Log.i(TAG, "Finishing onReceive")
        }
    }
}