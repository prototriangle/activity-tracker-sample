package com.specknet.orientandroid.adapters

import androidx.recyclerview.widget.DiffUtil
import com.specknet.orientandroid.data.ActiveDay

class ActiveDayDiffCallback : DiffUtil.ItemCallback<ActiveDay>() {

    override fun areItemsTheSame(oldItem: ActiveDay, newItem: ActiveDay): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ActiveDay, newItem: ActiveDay): Boolean {
        return oldItem == newItem
    }
}