package com.specknet.orientandroid.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.specknet.orientandroid.ActiveDayListFragmentDirections
import com.specknet.orientandroid.data.ActiveDay
import com.specknet.orientandroid.data.Converters
import com.specknet.orientandroid.databinding.ListItemActiveDayBinding


class ActiveDayAdapter(private val longClickListener: (String) -> View.OnLongClickListener) : ListAdapter<ActiveDay, ActiveDayAdapter.ViewHolder>(ActiveDayDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val activeDay = getItem(position)
        holder.apply {
            bind(createOnClickListener(Converters.fromDateToString(activeDay.date)), activeDay)
            bind(createOnLongClickListener(Converters.fromDateToString(activeDay.date)), activeDay)
            itemView.tag = activeDay
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
                ListItemActiveDayBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                )
        )
    }

    private fun createOnClickListener(date: String): View.OnClickListener {
        return View.OnClickListener { v ->
            val direction = ActiveDayListFragmentDirections.ActionActiveDayListFragmentToActiveDayDetailFragment(date)
            v.findNavController().navigate(direction)
        }
    }

    private fun createOnLongClickListener(date: String): View.OnLongClickListener {
        return longClickListener(date)
    }

    class ViewHolder(
            private val binding: ListItemActiveDayBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: View.OnClickListener, item: ActiveDay) {
            binding.apply {
                clickListener = listener
                day = item
                executePendingBindings()
            }
        }

        fun bind(listener: View.OnLongClickListener, item: ActiveDay) {
            binding.apply {
                longClickListener = listener
                day = item
                executePendingBindings()
            }
        }
    }
}