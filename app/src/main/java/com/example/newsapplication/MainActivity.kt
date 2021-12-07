package com.example.newsapplication

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity() {

    private val debug = true
    private val io_scope = /*lifecycleScope */
        CoroutineScope(Dispatchers.IO + CoroutineName("io_scope"))
    private lateinit var feed_list_view: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        feed_list_view = findViewById(R.id.feed_list)
        /*
        val source_url = "https://feeds.bbci.co.uk/news/rss.xml"
        val converter_url = "https://api.rss2json.com/v1/api.json?rss_url="
        val target_url = converter_url + source_url
         */
        val api_key    = "pub_2633950f76868b0c471f9101a8343a615c7d"
        val target_url = "https://newsdata.io/api/1/news?apikey=$api_key&q=russia"

        io_scope.launch {
            val feed_list = async {
                if (debug == false) loadRSS(target_url)
                else loadJSON()
            }.await()
            if (feed_list != null) {
                withContext(Dispatchers.Main) {
                    displayFeed(feed_list)
                }
            }
        }

        val refresh_btn = findViewById<Button>(R.id.refresh_button)

        refresh_btn.setOnClickListener {
            io_scope.launch {
                val feed_list = async {
                    if (debug == false) loadRSS(target_url)
                    else loadJSON()
                }.await()
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
        for (feed_item in feed_list.results) {
            val feed_item_view = inflater.inflate(R.layout.feed_item, feed_list_view, false)
            val title = feed_item_view.findViewById<TextView>(R.id.feed_item_title)
            title.text = feed_item.title
            feed_list_view.addView(feed_item_view)
        }
    }

    private fun displayFeed(feed_list: FeedList){
        feed_list_view.adapter = FeedListAdapter(feed_list)
        feed_list_view.layoutManager = GridLayoutManager(this, 1)
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

    private suspend fun loadJSON(): FeedList? {
        val feed_list = Gson().fromJson(json, FeedList::class.java)
        return feed_list
    }
}

class FeedList(val results: ArrayList<FeedItem>)

class FeedItem(
    val title: String?,
    val description: String?,
    val image_url: String?,
    val link: String?,
)

class FeedListAdapter__(val feed_list: FeedList) : BaseAdapter() {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val inflater = LayoutInflater.from(parent!!.context)

        val feed_item_view = convertView ?: inflater.inflate(R.layout.feed_item, parent, false)
        val title = feed_item_view.findViewById<TextView>(R.id.feed_item_title)
        val feed_item = getItem(position) as FeedItem
        title.text = feed_item.title
        return feed_item_view
    }

    override fun getItem(position: Int): Any {
        return feed_list.results[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return feed_list.results.size
    }
}


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
        val thumbnail       = feed_item_view.findViewById<ImageView>(R.id.feed_item_thumbnail)
        val context         = title.context
        title.text          = feed_item.title ?: ""
        description.text    = feed_item.description ?: ""
        if (feed_item.image_url != null && feed_item.image_url != "") {
            Picasso.with(context).load(feed_item.image_url).into(thumbnail)
        }

        feed_item_view.setOnClickListener {
            if (feed_item.link != null) {
                val intent = Intent(ACTION_VIEW)
                intent.data = Uri.parse(feed_item.link)
                context.startActivity(intent)
            }
        }
    }
}