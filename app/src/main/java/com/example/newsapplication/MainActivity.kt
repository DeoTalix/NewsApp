package com.example.newsapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers


class MainActivity : AppCompatActivity() {

    private val debug           = false
    private val io_scope        = CoroutineScope(Dispatchers.IO + CoroutineName("io_scope"))
    private val api_key         = "pub_2633950f76868b0c471f9101a8343a615c7d"
    private val default_query   = "Russia"

    private lateinit var feed_list_view:    RecyclerView
    private lateinit var query_input:       EditText
    private lateinit var refresh_btn:       Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init_views()

        val url = constructUrl(default_query)
        load_and_display_feed(url)

        refresh_btn.setOnClickListener {
            val url = constructUrl(query_input.text.toString())
            load_and_display_feed(url)
        }
    }

    private fun init_views() {
        feed_list_view   = findViewById(R.id.feed_list)
        query_input      = findViewById(R.id.query_input)
        refresh_btn      = findViewById(R.id.refresh_button)
    }

    private fun constructUrl(query: String, api_key: String = this.api_key) = "https://newsdata.io/api/1/news?apikey=$api_key&q=$query"

    private fun load_and_display_feed(url: String){
        io_scope.launch {
            val json_string = async { loadRSS(url) }.await()
            val feed_list: FeedList? = if (json_string != null) {
                convert_json_to_feed_list(json_string) } else { null }

            if (feed_list != null) {
                withContext(Dispatchers.Main) {
                    Log.v("request", url)
                    displayFeed(feed_list)
                }
            }
        }
    }

    private fun displayFeed(feed_list: FeedList){
        feed_list_view.adapter       = FeedListAdapter(feed_list)
        feed_list_view.layoutManager = LinearLayoutManager(this)
    }

    private fun convert_json_to_feed_list(json_string: String): FeedList? {
        return Gson().fromJson(json_string, FeedList::class.java)
    }

    private fun loadRSS(url: String): String? {
        if (debug) {
            return default_json
        }

        var error_msg = "Request failed:"
        val url_connection = URL(url).openConnection() as HttpsURLConnection?
        if (url_connection != null) {
            try {
                url_connection.connect()
                if (url_connection.responseCode != HttpsURLConnection.HTTP_OK) {
                    throw RuntimeException(url_connection.responseMessage)
                } else {
                    val json_string = url_connection.inputStream.bufferedReader().readText()
                    //Log.v("request", "response: $str")
                    return json_string
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

class FeedList(val results: ArrayList<FeedItem>)

class FeedItem(
    val title: String?,
    val description: String?,
    val full_description: String?,
    val content: String?,
    val image_url: String?,
    val link: String?,
    val pubDate: String?,
    val source_id: String?,
)

class FeedListAdapter(val feed_list: FeedList) : RecyclerView.Adapter<RVHolder>() {

    private lateinit var inflater: LayoutInflater

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RVHolder {
        inflater = LayoutInflater.from(parent!!.context)
        val feed_item_view = inflater.inflate(R.layout.feed_item, parent, false)
        return RVHolder(feed_item_view)
    }

    override fun getItemCount(): Int {
        return feed_list.results.size
    }

    override fun onBindViewHolder(holder: RVHolder, position: Int) {
        val feed_item = feed_list.results[position]
        holder?.bind(feed_item)
    }
}

class RVHolder(val feed_item_view: View): RecyclerView.ViewHolder(feed_item_view) {
    fun bind(feed_item: FeedItem) {
        val title           = feed_item_view.findViewById<TextView>(R.id.feed_item_title)
        val description     = feed_item_view.findViewById<TextView>(R.id.feed_item_description)
        val context         = title.context
        title.text          = feed_item.title ?: ""
        description.text    = feed_item.description ?: ""

        feed_item_view.setOnClickListener {
            if (feed_item.link != null) {
                val item_activity_intent = Intent(context, ItemActivity::class.java)

                item_activity_intent.putExtra("feed_item:title",            feed_item.title)
                item_activity_intent.putExtra("feed_item:description",      feed_item.description)
                item_activity_intent.putExtra("feed_item:full_description", feed_item.full_description)
                item_activity_intent.putExtra("feed_item:content",          feed_item.content)
                item_activity_intent.putExtra("feed_item:image_url",        feed_item.image_url)
                item_activity_intent.putExtra("feed_item:link",             feed_item.link)
                item_activity_intent.putExtra("feed_item:pubDate",          feed_item.pubDate)
                item_activity_intent.putExtra("feed_item:source_id",        feed_item.source_id)

                startActivity(context, item_activity_intent, null)
            }
        }
    }
}