package io.github.sdsstudios.nvidiagpumonitor

import android.content.Context
import android.database.Cursor
import android.support.v4.widget.CursorAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.sonelli.juicessh.pluginlibrary.PluginContract
import java.util.*

/**
 * Created by Seth on 04/03/18.
 */

class ConnectionListAdapter(ctx: Context) : CursorAdapter(ctx, null, false) {

    private val mInflater = ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        return mInflater.inflate(R.layout.spinner_item, parent, false)
    }

    override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
        val nameColumn = cursor!!.getColumnIndex(PluginContract.Connections.COLUMN_NAME)

        if (nameColumn > -1) {
            val name = cursor.getString(nameColumn)
            view!!.findViewById<TextView>(R.id.textView).text = name
        }
    }

    fun getConnectionId(position: Int): UUID? {
        var id: UUID? = null

        if (cursor != null) {
            if (cursor.moveToPosition(position)) {
                val idIndex = cursor.getColumnIndex(PluginContract.Connections.COLUMN_ID)
                if (idIndex > -1) {
                    id = UUID.fromString(cursor.getString(idIndex))
                }
            }
        }

        return id
    }
}