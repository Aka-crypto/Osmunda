package moe.sunjiao.osmundademo.fragment

import android.app.Activity.RESULT_OK
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_home.*
import moe.sunjiao.osmunda.model.ImportOption
import moe.sunjiao.osmunda.reader.OsmReader
import moe.sunjiao.osmunda.reader.OsmosisReader
import moe.sunjiao.osmundademo.R
import java.io.File
import java.util.*


class HomeFragment : Fragment() {
    val reader : OsmReader = OsmosisReader()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val thisView: View = inflater.inflate(R.layout.fragment_home,container,false)
        reader.options.add(ImportOption.INCLUDE_RELATIONS)
        reader.options.add(ImportOption.INCLUDE_WAYS)

        val file = File(context?.filesDir?.absolutePath+ "/nauru-latest.osm.bz2")

        val thread = Thread(Runnable { context?.let { reader.read(file, it, "nauru") } })

        val listener : OnClickListener = OnClickListener {
            val intent = Intent()
            intent.type = "*/*"
            intent.action = Intent.ACTION_GET_CONTENT;
            startActivityForResult(Intent.createChooser(intent, "Select Osm Source File"), 100);
            }
        thisView.findViewById<ImageButton?>(R.id.import_button)?.setOnClickListener(listener)
        return thisView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 100 && resultCode == RESULT_OK){

            val start = System.currentTimeMillis()
            val task: TimerTask = object : TimerTask() {
                override fun run() {
                    val handler = MyHandler()
                    val message = handler.obtainMessage()
                    message.obj = arrayOf(reader.progress, import_progress)
                    message.what = 0
                    handler.sendMessage(message)
                }
            }
            val timer = Timer(true)

            timer.schedule(task, 0, 1000)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private class MyHandler() : Handler() {

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                0-> {
                    val array : Array<Any> = msg.obj as Array<Any>
                    (array[1] as ProgressBar).progress = (array[0] as Double).toInt()

                }
                else -> {
                }
            }
        }
    }


    fun getPath(context: Context, path: String): String? {
        val uri = Uri.parse(path) ?: return null
        var data: String? = null
        if (path.length > 8 && path.substring(0, 8) == "content:") {
            val scheme = uri.scheme
            if (scheme == null) data =
                uri.path else if (ContentResolver.SCHEME_FILE == scheme) {
                data = uri.path
            } else if (ContentResolver.SCHEME_CONTENT == scheme) {
                val cursor = context.contentResolver.query(uri, arrayOf(MediaStore.Images.ImageColumns.DATA), null, null, null)
                if (null != cursor) {
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                        if (index > -1) {
                            data = cursor.getString(index)
                        }
                    }
                    cursor.close()
                }
            }
        } else {
            return path
        }
        return data
    }
}
