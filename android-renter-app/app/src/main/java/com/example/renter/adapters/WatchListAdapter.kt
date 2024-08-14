package com.example.renter.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.renter.R
import com.example.renter.models.Property

class WatchListAdapter (
    var yourListData:List<Property>,
    var functionOnClick: (Int) ->Unit,
    var removeFunction: (String)->Unit
)
    : RecyclerView.Adapter<WatchListAdapter.PropertyViewHolder>(){

    inner class PropertyViewHolder(itemView: View) : RecyclerView.ViewHolder (itemView) {
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.watch_list_row_layout, parent, false)

        return PropertyViewHolder(view)
    }
    override fun getItemCount(): Int {
        return yourListData.size
    }
    override fun onBindViewHolder(holder: PropertyViewHolder, position: Int) {
        val property: Property = yourListData.get(position)

        val tvAddress = holder.itemView.findViewById<TextView>(R.id.tvAddress)
        tvAddress.text = property.address

        val onClick = holder.itemView.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(
            R.id.btnToPropertyDetails)
        val removeClick = holder.itemView.findViewById<Button>(
            R.id.btnRemove)
        onClick.setOnClickListener{
            functionOnClick(position)
        }
        removeClick.setOnClickListener{
            removeFunction(property.id)
        }

    }

}