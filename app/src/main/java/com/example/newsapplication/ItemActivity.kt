package com.example.newsapplication

import android.app.Activity
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso


class ItemActivity : Activity() {
    private val tag = "ItemActivity"

    private lateinit var feed_item_image_view:          ImageView
    private lateinit var feed_item_title_view:          TextView
    private lateinit var feed_item_description_view:    TextView
    private lateinit var feed_item_link_view:           Button
    private lateinit var feed_item_pubDate_view:        TextView
    private lateinit var feed_item_source_id_view:      TextView
    private lateinit var feed_item_back_view:           Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item)

        init_views()
        map_data_to_views()
    }

    private fun init_views(){
        feed_item_image_view        = findViewById(R.id.act_feed_item_image)
        feed_item_title_view        = findViewById(R.id.act_feed_item_title)
        feed_item_description_view  = findViewById(R.id.act_feed_item_description)
        feed_item_link_view         = findViewById(R.id.act_feed_item_link)
        feed_item_pubDate_view      = findViewById(R.id.act_feed_item_date)
        feed_item_source_id_view    = findViewById(R.id.act_feed_item_source_id)
        feed_item_back_view         = findViewById(R.id.act_feed_item_back)
    }

    private fun map_data_to_views(){
        val feed_item_image_url = intent.getStringExtra("feed_item:image_url")
        if (feed_item_image_url != null) {
            Picasso.with(this)
                .load(feed_item_image_url)
                .into(feed_item_image_view)
        }

        feed_item_title_view.text       = intent.getStringExtra("feed_item:title")
        feed_item_description_view.text = intent.getStringExtra("feed_item:full_description")
            ?: intent.getStringExtra("feed_item:description")

        intent.getStringExtra("feed_item:content")

        feed_item_pubDate_view.text     = intent.getStringExtra("feed_item:pubDate")
        feed_item_source_id_view.text   = intent.getStringExtra("feed_item:source_id")

        feed_item_link_view.setOnClickListener {
            val browse_intent   = Intent(ACTION_VIEW)
            val link            = intent.getStringExtra("feed_item:link")
            browse_intent.data  = Uri.parse(link)
            this.startActivity(browse_intent)
        }

        feed_item_back_view.setOnClickListener {
            finish()
        }
    }
}