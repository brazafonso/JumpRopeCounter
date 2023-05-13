package com.example.jumpropecounter.Hub.Fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.jumpropecounter.R
import com.example.jumpropecounter.User.User
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.utils.ColorTemplate

class Home:Fragment() {
    var TAG = "Home"
    lateinit var user:User
    lateinit var logout_btn:Button
    lateinit var jumps_day_chart:BarChart

    companion object {
        fun newInstance(u: User): Home {
            val fragment = Home()
            val args = Bundle()
            args.putParcelable("user", u)
            fragment.arguments = args
            return fragment
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        user = requireArguments().getParcelable("user")!!
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)


        logout_btn = requireActivity().findViewById(R.id.logout_btn)
        jumps_day_chart = requireActivity().findViewById(R.id.jumps_day_chart)

        logout_btn.setOnClickListener { _ ->
            Log.d(TAG,"Logout button")
            user.sign_out()
            requireActivity().finish()
        }



        val list:ArrayList<BarEntry> = ArrayList()
        list.add(BarEntry(100f,100f,"Monday"))
        list.add(BarEntry(101f,101f,"Tuesday"))
        list.add(BarEntry(102f,102f,"Wednesday"))
        list.add(BarEntry(103f,103f,"Thursday"))
        list.add(BarEntry(104f,104f,"Friday"))

        val barDataSet = BarDataSet(list,"list")

        barDataSet.setColors(ColorTemplate.MATERIAL_COLORS,255)
        barDataSet.valueTextColor= Color.BLACK

        val barData = BarData(barDataSet)

        jumps_day_chart.data = barData

        jumps_day_chart.description.text = "bar chart"


    }



}