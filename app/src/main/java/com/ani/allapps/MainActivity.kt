package com.ani.allapps

import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    data class Item(val label: String, val pkg: String, val cls: String, val ri: ResolveInfo)

    private var all = listOf<Item>()
    private var shown = listOf<Item>()
    private lateinit var adapter: Adapter

    private fun dp(x: Int) = (x * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pm = packageManager
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        all = pm.queryIntentActivities(q, 0)
            .map { Item(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.activityInfo.name, it) }
            .sortedBy { it.label.lowercase() }
        shown = all
        title = "Alle Apps (" + all.size + ")"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = true
        }
        val search = EditText(this).apply {
            hint = "Suchen…"
            setSingleLine()
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val list = ListView(this)
        adapter = Adapter()
        list.adapter = adapter
        root.addView(search, LinearLayout.LayoutParams(-1, -2))
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val t = s.toString().trim().lowercase()
                shown = if (t.isEmpty()) all
                else all.filter { it.label.lowercase().contains(t) || it.pkg.lowercase().contains(t) }
                adapter.notifyDataSetChanged()
            }
        })

        list.setOnItemClickListener { _, _, pos, _ ->
            val it = shown[pos]
            try {
                startActivity(
                    Intent(Intent.ACTION_MAIN)
                        .setClassName(it.pkg, it.cls)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Konnte nicht starten: " + e.message, Toast.LENGTH_SHORT).show()
            }
        }
        list.setOnItemLongClickListener { _, _, pos, _ ->
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + shown[pos].pkg))
            )
            true
        }
    }

    inner class Adapter : BaseAdapter() {
        override fun getCount() = shown.size
        override fun getItem(position: Int) = shown[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val row: LinearLayout
            val icon: ImageView
            val name: TextView
            val pkg: TextView
            if (convertView == null) {
                row = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(16), dp(8), dp(16), dp(8))
                }
                icon = ImageView(this@MainActivity)
                row.addView(icon, LinearLayout.LayoutParams(dp(44), dp(44)))
                val col = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(16), 0, 0, 0)
                }
                name = TextView(this@MainActivity).apply { textSize = 16f }
                pkg = TextView(this@MainActivity).apply { textSize = 12f; alpha = 0.6f }
                col.addView(name)
                col.addView(pkg)
                row.addView(col)
                row.tag = arrayOf(icon, name, pkg)
            } else {
                row = convertView as LinearLayout
            }
            @Suppress("UNCHECKED_CAST")
            val v = row.tag as Array<View>
            val item = shown[position]
            (v[0] as ImageView).setImageDrawable(item.ri.loadIcon(packageManager))
            (v[1] as TextView).text = item.label
            (v[2] as TextView).text = item.pkg
            return row
        }
    }
}
