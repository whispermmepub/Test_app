package com.whisper.wowlauncher

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        setContentView(LauncherView())
    }

    inner class LauncherView : View(this@MainActivity) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val apps = mutableListOf<ApplicationInfo>()
        private val labels = mutableListOf<String>()
        private val launchables = mutableListOf<Intent>()
        private var page = 0
        private var downX = 0f
        private var downY = 0f
        private val date = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        private val time = SimpleDateFormat("HH:mm", Locale.getDefault())

        init { loadApps() }

        private fun loadApps() {
            val pm = packageManager
            val base = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val list = pm.queryIntentActivities(base, 0).sortedBy { it.loadLabel(pm).toString().lowercase() }
            apps.clear(); labels.clear(); launchables.clear()
            for (r in list) {
                if (r.activityInfo.packageName == packageName) continue
                apps += r.activityInfo.applicationInfo
                labels += r.loadLabel(pm).toString()
                launchables += Intent(base).setClassName(r.activityInfo.packageName, r.activityInfo.name)
            }
        }

        override fun onDraw(c: Canvas) {
            val w = width.toFloat(); val h = height.toFloat(); val d = resources.displayMetrics.density
            paint.shader = LinearGradient(0f, 0f, w, h, Color.rgb(70,130,220), Color.rgb(185,90,170), Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, w, h, paint); paint.shader = null

            paint.color = Color.WHITE; paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create("sans", Typeface.NORMAL)
            paint.textSize = 14f*d; c.drawText(date.format(Date()), w/2, 52f*d, paint)
            paint.typeface = Typeface.create("sans", Typeface.BOLD)
            paint.textSize = 42f*d; c.drawText(time.format(Date()), w/2, 102f*d, paint)

            val cellW = w/4f; val top = 140f*d; val size = 56f*d
            val start = page*20; val end = minOf(start+20, apps.size)
            for (i in start until end) {
                val n=i-start; val col=n%4; val row=n/4
                drawApp(c, apps[i], labels[i], cellW*col+cellW/2, top+row*82f*d, size)
            }

            val dockY=h-92f*d
            paint.color=Color.argb(92,255,255,255)
            c.drawRoundRect(12f*d,dockY,w-12f*d,h-10f*d,28f*d,28f*d,paint)
            val symbols=arrayOf("☎","✉","●","⚙")
            for(i in 0..3) drawDock(c,cellW*i+cellW/2,dockY+29f*d,size,symbols[i])
        }

        private fun drawApp(c:Canvas, info:ApplicationInfo, label:String, cx:Float, cy:Float, size:Float) {
            val d=info.loadIcon(packageManager)
            d.setBounds((cx-size/2).toInt(),(cy-size/2).toInt(),(cx+size/2).toInt(),(cy+size/2).toInt()); d.draw(c)
            paint.color=Color.WHITE; paint.textAlign=Paint.Align.CENTER; paint.textSize=12f*resources.displayMetrics.density
            c.drawText(label.take(13),cx,cy+size/2+17f*resources.displayMetrics.density,paint)
        }

        private fun drawDock(c:Canvas,cx:Float,cy:Float,size:Float,symbol:String) {
            paint.color=Color.WHITE; c.drawCircle(cx,cy,size/2,paint)
            paint.color=Color.DKGRAY; paint.textAlign=Paint.Align.CENTER; paint.textSize=22f*resources.displayMetrics.scaledDensity
            c.drawText(symbol,cx,cy+8f*resources.displayMetrics.density,paint)
        }

        override fun onTouchEvent(e:MotionEvent):Boolean {
            val d=resources.displayMetrics.density
            if(e.action==MotionEvent.ACTION_DOWN){downX=e.x;downY=e.y;return true}
            if(e.action!=MotionEvent.ACTION_UP)return true
            val dx=e.x-downX
            if(kotlin.math.abs(dx)>100f*d){
                page=if(dx<0)page+1 else maxOf(0,page-1)
                page=minOf(page,maxOf(0,(apps.size-1)/20));invalidate();return true
            }
            if(e.y>=height-92f*d){
                val col=(e.x/(width/4f)).toInt().coerceIn(0,3)
                Toast.makeText(this@MainActivity, arrayOf("Phone","Messages","Camera","Settings")[col], Toast.LENGTH_SHORT).show()
                return true
            }
            val top=140f*d
            if(e.y>=top){
                val col=(e.x/(width/4f)).toInt()
                val row=((e.y-top)/(82f*d)).toInt()
                val idx=page*20+row*4+col
                if(col in 0..3 && row in 0..4 && idx<launchables.size) {
                    try { startActivity(launchables[idx]) } catch(_:Exception) {}
                }
            }
            return true
        }
    }
}
