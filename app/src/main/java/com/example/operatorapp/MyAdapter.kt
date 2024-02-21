package com.example.operatorapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class MyAdapter(
    private val activity: Context,
    private val mList: MutableList<Model>,
    private val itemClickListener: OnItemClickListener
) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val v: View = LayoutInflater
            .from(activity)
            .inflate(R.layout.item, parent, false)

        return MyViewHolder(v, itemClickListener, mList)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = mList[position]
        holder.tvName.text = currentItem.name
        holder.tvLatLong.text = "Latitude:${currentItem.lat}\nLongitude:${currentItem.long}"
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    class MyViewHolder(
        itemView: View,
        private val itemClickListener: OnItemClickListener,
        private val mList: MutableList<Model>
    ) : RecyclerView.ViewHolder(itemView) {
        var tvName: TextView
        var tvLatLong: TextView


        init {
            tvName = itemView.findViewById(R.id.tvStopName)
            tvLatLong = itemView.findViewById(R.id.tvLatLong)

            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    itemClickListener.onItemClick(mList[position])
                }
            }
        }
    }


    interface OnItemClickListener {
        fun onItemClick(model: Model)
    }



}