package com.specknet.orientandroid

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.specknet.orientandroid.data.Converters
import com.specknet.orientandroid.databinding.FragmentActiveDayDetailBinding
import com.specknet.orientandroid.utilities.InjectorUtils
import com.specknet.orientandroid.viewmodels.ActiveDayDetailViewModel

class ActiveDayDetailFragment : Fragment() {

    private lateinit var shareText: String
    lateinit var binding: FragmentActiveDayDetailBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val date = if (arguments != null) {
            arguments!!.getString("date")
        } else {
            "null"
        }

        Log.i("ActiveDayDetailFragment", arguments.toString())

        val factory = InjectorUtils.provideActiveDayDetailViewModelFactory(requireActivity(), Converters.fromStringToDate(date))
        val activeDayDetailViewModel =
                ViewModelProviders.of(this, factory).get(ActiveDayDetailViewModel::class.java)

        binding = DataBindingUtil.inflate<FragmentActiveDayDetailBinding>(
                inflater, R.layout.fragment_active_day_detail, container, false).apply {
            viewModel = activeDayDetailViewModel
            setLifecycleOwner(this@ActiveDayDetailFragment)
        }


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
}
