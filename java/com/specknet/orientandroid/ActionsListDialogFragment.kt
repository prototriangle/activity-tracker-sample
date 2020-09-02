package com.specknet.orientandroid

import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_actions_list_dialog.*
import kotlinx.android.synthetic.main.fragment_actions_list_dialog_item.view.*

class ActionsListDialogFragment : BottomSheetDialogFragment() {
    private var mListener: Listener? = null
    private var title: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_actions_list_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = ActionsAdapter(positionToAction, actionNames)
        if (title != null) {
            dialogWrapperTitle?.text = title
        } else {
            dialogWrapperTitle?.visibility = View.GONE
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (mListener == null) {
            val parent = parentFragment
            mListener =
                    if (parent != null) {
                        parent as Listener
                    } else {
                        context as Listener
                    }
        }
    }

    override fun onDetach() {
        mListener = null
        super.onDetach()
    }

    enum class Action {
        Delete,
        DeleteAll,
        None
    }

    interface Listener {
        fun onActionsClicked(action: Action)
    }

    private inner class ViewHolder internal constructor(inflater: LayoutInflater, parent: ViewGroup, positionToAction: List<Action>)
        : RecyclerView.ViewHolder(inflater.inflate(R.layout.fragment_actions_list_dialog_item, parent, false)) {

        internal val text: TextView = itemView.text

        init {
            text.setOnClickListener {
                mListener?.let { l ->
                    l.onActionsClicked(positionToAction[adapterPosition])
                    dismiss()
                }
            }
        }
    }

    private inner class ActionsAdapter
    internal constructor(
            private val positionToAction: List<Action>,
            private val actionNames: Map<Action, String>)
        : RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context), parent, positionToAction)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.text.text = actionNames[positionToAction[position]]
        }

        override fun getItemCount(): Int {
            return positionToAction.size
        }
    }

    companion object {


        val actionNames: Map<Action, String> = mapOf(Action.Delete to "Delete record for this day", Action.DeleteAll to "Delete all records")
        val positionToAction = listOf(Action.Delete, Action.DeleteAll)
        // TODO: Customize parameters
        fun newInstance(listener: Listener? = null, title: String): ActionsListDialogFragment =
                ActionsListDialogFragment().apply {
                    mListener = listener
                    this.title = title
                }

    }
}
