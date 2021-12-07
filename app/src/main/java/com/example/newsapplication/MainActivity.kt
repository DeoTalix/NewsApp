package com.example.newsapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.lifecycleScope
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import com.google.gson.Gson
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity() {

    private val io_scope = /*lifecycleScope */
        CoroutineScope(Dispatchers.IO + CoroutineName("io_scope"))
    private lateinit var feed_list_view: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        feed_list_view = findViewById(R.id.feed_list)

        val source_url = "https://feeds.bbci.co.uk/news/rss.xml"
        val converter_url = "https://api.rss2json.com/v1/api.json?rss_url="
        val target_url = converter_url + source_url

        io_scope.launch {
            val feed_list = async { loadRSS(target_url) }.await()
            if (feed_list != null) {
                withContext(Dispatchers.Main) {
                    displayFeed(feed_list)
                }
            }
        }

        val refresh_btn = findViewById<Button>(R.id.refresh_button)

        refresh_btn.setOnClickListener {
            io_scope.launch {
                val feed_list = async { loadRSS(target_url) }.await()
                if (feed_list != null) {
                    withContext(Dispatchers.Main) {
                        displayFeed(feed_list)
                    }
                }
            }
        }
    }

    private fun showLinearLayout(feed_list: FeedList) {
        val inflater = this.layoutInflater
        for (feed_item in feed_list.items) {
            val feed_item_view = inflater.inflate(R.layout.feed_item, feed_list_view, false)
            val title = feed_item_view.findViewById<TextView>(R.id.feed_item_title)
            title.text = feed_item.title
            feed_list_view.addView(feed_item_view)
        }
    }

    private fun displayFeed(feed_list: FeedList){
        feed_list_view.adapter = FeedListAdapter(feed_list)
    }

    private suspend fun loadRSS(url: String): FeedList? {
        var error_msg = "Request failed:"
        val url_connection = URL(url).openConnection() as HttpsURLConnection?
        if (url_connection != null) {
            try {
                url_connection.connect()
                if (url_connection.responseCode != HttpsURLConnection.HTTP_OK) {
                    throw RuntimeException(url_connection.responseMessage)
                } else {
                    val str = url_connection.inputStream.bufferedReader().readText()
                    //Log.v("request", "response: $str")
                    val feed_list = Gson().fromJson(str, FeedList::class.java)
                    return feed_list
                }
            } catch (e: Exception) {
                Log.e("request", "$error_msg ${e.stackTraceToString()}", e)
                Toast.makeText(this, "$error_msg bad server response", Toast.LENGTH_SHORT)
                    .show()
            } finally {
                url_connection.disconnect()
            }
        } else {
            error_msg = "$error_msg Failed to open url connection."
            Log.v("request", error_msg)
            Toast.makeText(this, error_msg, Toast.LENGTH_SHORT).show()
        }
        return null
    }
}

class FeedList(val items: ArrayList<FeedItem>)

class FeedItem(
    val title: String,
    val link: String,
)

class FeedListAdapter(val feed_list: FeedList) : BaseAdapter() {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val inflater = LayoutInflater.from(parent!!.context)

        val feed_item_view = convertView ?: inflater.inflate(R.layout.feed_item, parent, false)
        val title = feed_item_view.findViewById<TextView>(R.id.feed_item_title)
        val feed_item = getItem(position) as FeedItem
        title.text = feed_item.title
        return feed_item_view
    }

    override fun getItem(position: Int): Any {
        return feed_list.items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return feed_list.items.size
    }
}

