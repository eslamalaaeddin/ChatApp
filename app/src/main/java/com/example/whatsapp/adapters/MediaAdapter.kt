package com.example.whatsapp.adapters

import android.app.ActionBar
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.whatsapp.R
import com.example.whatsapp.models.PrivateMessageModel
import com.example.whatsapp.ui.ui.activities.VideoPlayerActivity
import com.squareup.picasso.Picasso
private const val VIDEO_URL = "video url"

class MediaAdapter (private var list:List<PrivateMessageModel>) : RecyclerView.Adapter<MediaAdapter.MediaHolder>() {
    private lateinit var context: Context
    inner class MediaHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
        private var mediaImageView: ImageView = itemView.findViewById(R.id.media_image_view)
        private var videoInfoLayout: LinearLayout = itemView.findViewById(R.id.video_info_layout)
        private var mediaDurationTextView: TextView = itemView.findViewById(R.id.media_duration_text_view)

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
            context = itemView.context
        }


        fun bind (mediaMessage : PrivateMessageModel) {
            //media is images
            if (mediaMessage.type == "image" || mediaMessage.type == "captured image"){
                Picasso.get().load(mediaMessage.message).into(mediaImageView)
                videoInfoLayout.visibility = View.INVISIBLE
            }

            else if (mediaMessage.type == "video") {
                videoInfoLayout.visibility = View.VISIBLE

                val interval: Long = 1* 1000
                val options: RequestOptions = RequestOptions().frame(interval)

                Glide.with(itemView.context)
                    .asBitmap().load(mediaMessage.message).apply(options).into(mediaImageView)
            }
        }

        override fun onClick(item: View?) {
            val currentMedia = list[adapterPosition]
            val messageType = list[adapterPosition].type

            if (messageType== "image" ||messageType == "captured image"){
                showSentImage(currentMedia.message)
            }

            else if (messageType== "video") {
                val videoIntent = Intent(
                    itemView.context,
                    VideoPlayerActivity::class.java
                )
                videoIntent.putExtra(VIDEO_URL, currentMedia.message)
                itemView.context.startActivity(videoIntent)
            }
        }

        override fun onLongClick(item: View?): Boolean {
            return true
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.media_item_layout,parent,false )

        return MediaHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: MediaHolder, position: Int) {
        val media = list[holder.adapterPosition]
        holder.bind(media)
    }

    private fun showSentImage(imageUrl: String) {
        val builder = Dialog(context)
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE)
        builder.window?.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )
        builder.setOnDismissListener {
            //nothing;
        }
        val imageView = ImageView(context)

        Picasso.get()
            .load(imageUrl)
            .into(imageView)


        builder.addContentView(
            imageView, ActionBar.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        builder.show()
    }
}