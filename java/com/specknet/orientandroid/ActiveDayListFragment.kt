package com.specknet.orientandroid

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.specknet.orientandroid.adapters.ActiveDayAdapter
import com.specknet.orientandroid.data.Converters
import com.specknet.orientandroid.databinding.FragmentActiveDayListBinding
import com.specknet.orientandroid.utilities.InjectorUtils
import com.specknet.orientandroid.viewmodels.ActiveDayListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class ActiveDayListFragment : Fragment(), ActionsListDialogFragment.Listener {

    private lateinit var viewModel: ActiveDayListViewModel
    private var selectedDate: String? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentActiveDayListBinding.inflate(inflater, container, false)
        val context = context ?: return binding.root

        val factory = InjectorUtils.provideActiveDayListViewModelFactory(context)
        viewModel = ViewModelProviders.of(this, factory).get(ActiveDayListViewModel::class.java)

        val adapter = ActiveDayAdapter { dateString ->
            View.OnLongClickListener {
                selectedDate = dateString
                showBottomSheetDialog(dateString)
            }
        }
        binding.activeDayList.adapter = adapter
        subscribeUi(adapter)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuResource = arguments?.getInt("menuResource")
        Log.i("menuResource", menuResource.toString())
        if (menuResource != null) {
            (activity as? MainStub)?.setMenu(menuResource)
        }
    }

    private fun showBottomSheetDialog(title: String): Boolean {
        if (activity == null) return false
        val dialog = ActionsListDialogFragment.newInstance(this, title)
        dialog.show(activity!!.supportFragmentManager, "DeleteDialog")
        return dialog.showsDialog
    }

    override fun onActionsClicked(action: ActionsListDialogFragment.Action) {
        Log.i("onActionsClicked", "onActionsClicked(${ActionsListDialogFragment.actionNames[action]})")
        GlobalScope.launch(Dispatchers.IO) {
            when (action) {
                ActionsListDialogFragment.Action.Delete -> {
                    val date = Converters.fromStringToDate(selectedDate)
                    viewModel.clearDay(date)
                    viewModel.removeDay(date)
                }
                ActionsListDialogFragment.Action.DeleteAll -> {
                    viewModel.removeAllDays()
                }
                else -> {
                }
            }
        }
    }

    private fun subscribeUi(adapter: ActiveDayAdapter) {
        viewModel.getActiveDays().observe(viewLifecycleOwner, Observer { activeDays ->
            if (activeDays != null) {
                adapter.submitList(activeDays)
            }
        })
    }
}
