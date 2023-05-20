package com.example.jumpropecounter.Hub.Fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.jumpropecounter.Camera.Preview
import com.example.jumpropecounter.Exercise.JumpRope
import com.example.jumpropecounter.JUMP_TYPE_ACTIVITY
import com.example.jumpropecounter.R
import com.example.jumpropecounter.User.User
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.properties.Delegates

class Home:Fragment() {
    var TAG = "Home"
    private lateinit var activity: FragmentActivity
    private lateinit var user:User
    private lateinit var logout_btn:Button
    private lateinit var n_jumps:TextView
    private lateinit var day_streak:TextView
    private lateinit var jumps_day_chart:BarChart


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
        activity = requireActivity()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)


        logout_btn = activity.findViewById(R.id.logout_btn)
        n_jumps = activity.findViewById(R.id.njumps)
        jumps_day_chart = activity.findViewById(R.id.jumps_day_chart)
        day_streak = activity.findViewById(R.id.nstreakdays)

        logout_btn.setOnClickListener { _ ->
            Log.d(TAG,"Logout button")
            user.sign_out()
            requireActivity().finish()
        }

        update_stats()
    }

    /**
     * Func that gathers information about the users sessions and updates view accordingly
     */
    fun update_stats(){
        CoroutineScope(Dispatchers.IO).launch {
            val sessions = user.get_sessions()
            activity.runOnUiThread {
                val stats = user.get_stats(sessions, JUMP_TYPE_ACTIVITY)
                n_jumps.text = stats["total_reps"].toString()
                day_streak.text = stats["streak"].toString()

                // Feed daily jumps graph
                val daily_reps = stats["daily_reps"] as LinkedHashMap<LocalDate,Int>
                val sorted_daily_reps = daily_reps.toSortedMap()
                val list:ArrayList<BarEntry> = ArrayList()
                var x = 0F
                for(date in sorted_daily_reps){
                    list.add(BarEntry(x,date.value.toFloat()))
                    x += 1
                }

                val barDataSet = BarDataSet(list,"list")

                barDataSet.setColors(ColorTemplate.MATERIAL_COLORS,255)
                barDataSet.valueTextColor= Color.BLACK

                val barData = BarData(barDataSet)

                jumps_day_chart.data = barData

                jumps_day_chart.description.text = "bar chart"
                // refresh chart
                jumps_day_chart.notifyDataSetChanged()
                jumps_day_chart.invalidate()

                Log.d(TAG,"Done with stats")


            }
        }
    }

}