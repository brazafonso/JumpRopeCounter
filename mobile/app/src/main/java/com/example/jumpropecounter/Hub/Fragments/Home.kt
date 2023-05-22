package com.example.jumpropecounter.Hub.Fragments

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.jumpropecounter.JUMP_TYPE_ACTIVITY
import com.example.jumpropecounter.R
import com.example.jumpropecounter.User.User
import com.firebase.ui.auth.AuthUI
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class Home:Fragment() {
    var TAG = "Home"
    private lateinit var activity: FragmentActivity
    private lateinit var user:User
    private lateinit var logout_btn:Button
    private lateinit var n_jumps:TextView
    private lateinit var day_streak:TextView
    private lateinit var jumps_day_chart:LineChart
    private lateinit var time_day_chart:LineChart
    private lateinit var calories_day_chart:LineChart


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
        Log.d(TAG,"On home fragment")

        logout_btn = activity.findViewById(R.id.logout_btn)
        n_jumps = activity.findViewById(R.id.njumps)
        jumps_day_chart = activity.findViewById(R.id.jumps_day_chart)
        time_day_chart = activity.findViewById(R.id.time_day_chart)
        calories_day_chart = activity.findViewById(R.id.calories_day_chart)
        day_streak = activity.findViewById(R.id.nstreakdays)


        update_stats()

        logout_btn.setOnClickListener { _ ->
            Log.d(TAG,"Logout button")
            CoroutineScope(Dispatchers.IO).launch {
                user.sign_out(requireContext())
                activity.finish()
            }
        }

    }




    /**
     * Func that gathers information about the users sessions and updates view accordingly
     */
    private fun update_stats(){
        CoroutineScope(Dispatchers.IO).launch {
            val sessions = user.get_sessions()
            val stats = user.get_stats(sessions, JUMP_TYPE_ACTIVITY)
            val MET = 8.8F

            // Feed daily jumps graph
            val daily_reps_count = stats["daily_reps_count"] as LinkedHashMap<String,Int>
            val daily_reps_time = stats["daily_reps_time"] as LinkedHashMap<String,Map<String,Any>>

            if (daily_reps_time.isNotEmpty()) {
                val sorted_daily_reps_count = daily_reps_count.toSortedMap()
                val list_jumps: ArrayList<Entry> = ArrayList()
                val list_calories: ArrayList<Entry> = ArrayList()
                val list_time: ArrayList<Entry> = ArrayList()
                var x = 0F

                // Create plot points
                for (date in sorted_daily_reps_count) {
                    list_jumps.add(Entry(x, date.value.toFloat(),date.key))
                    if (daily_reps_time.contains(date.key)) {
                        val values = daily_reps_time[date.key]
                        if (values != null) {
                            val duration = values["duration"] as Long
                            val weight = values["weight"] as Float
                            val height = values["height"] as Float
                            val burned = calculate_burned_calories(duration, weight, height, MET)
                            list_calories.add(Entry(x, burned,date.key))
                            //Log.d(TAG,"${date.key} $duration")
                            list_time.add(Entry(x, duration.toFloat(),date.key))
                        }
                    } else {
                        list_calories.add(Entry(x, 0f))
                        list_time.add(Entry(x, 0f))
                    }
                    x += 1
                }
                // Graph settings
                val lineDataSet1 = LineDataSet(list_jumps.toList(),"")
                lineDataSet1.setColors(Color.WHITE, Color.WHITE)
                lineDataSet1.valueTextColor = Color.WHITE
                lineDataSet1.setDrawCircles(false)
                lineDataSet1.setDrawCircleHole(false)
                lineDataSet1.valueTextSize = 10f
                val lineData1 = LineData(lineDataSet1)
                jumps_day_chart.data = lineData1
                jumps_day_chart.setVisibleXRangeMaximum(7F)
                jumps_day_chart.description.text = ""
                jumps_day_chart.legend.isEnabled = false
                jumps_day_chart.xAxis.setValueFormatter { value, axis ->
                    val entry = lineDataSet1.getEntryForIndex(value.toInt())
                    val date = LocalDate.parse(entry.data.toString())
                    if(value !=  lineDataSet1.xMax)
                        date.dayOfWeek.toString()
                    else
                        date.toString()
                }
                jumps_day_chart.xAxis.textSize = 5f
                jumps_day_chart.axisRight.isEnabled = false
                jumps_day_chart.moveViewTo(x,0f,jumps_day_chart.axisLeft.axisDependency)
                jumps_day_chart.setDrawGridBackground(true)
                jumps_day_chart.setGridBackgroundColor(Color.BLACK)
                jumps_day_chart.setBackgroundColor(Color.parseColor("#1f1f1f"))
                jumps_day_chart.axisLeft.textColor = Color.WHITE
                jumps_day_chart.xAxis.textColor = Color.WHITE

                val lineDataSet2 = LineDataSet(list_calories.toList(),"")
                lineDataSet2.setColors(Color.WHITE, Color.WHITE)
                lineDataSet2.valueTextColor = Color.WHITE
                lineDataSet2.setDrawCircles(false)
                lineDataSet2.setDrawCircleHole(false)
                lineDataSet2.valueTextSize = 10f
                val lineData2 = LineData(lineDataSet2)
                calories_day_chart.data = lineData2
                calories_day_chart.setVisibleXRangeMaximum(7F)
                calories_day_chart.description.text = ""
                calories_day_chart.legend.isEnabled = false
                calories_day_chart.xAxis.setValueFormatter { value, axis ->
                    val entry = lineDataSet1.getEntryForIndex(value.toInt())
                    val date = LocalDate.parse(entry.data.toString())
                    if(value !=  lineDataSet2.xMax)
                        date.dayOfWeek.toString()
                    else
                        date.toString()
                }
                calories_day_chart.xAxis.textSize = 5f
                calories_day_chart.axisRight.isEnabled = false
                calories_day_chart.moveViewTo(x,0f,calories_day_chart.axisLeft.axisDependency)
                calories_day_chart.setDrawGridBackground(true)
                calories_day_chart.setGridBackgroundColor(Color.BLACK)
                calories_day_chart.setBackgroundColor(Color.parseColor("#1f1f1f"))
                calories_day_chart.axisLeft.textColor = Color.WHITE
                calories_day_chart.xAxis.textColor = Color.WHITE

                val lineDataSet3 = LineDataSet(list_time.toList(),"")
                lineDataSet3.setColors(Color.WHITE, Color.WHITE)
                lineDataSet3.valueTextColor = Color.WHITE
                lineDataSet3.setDrawCircles(false)
                lineDataSet3.setDrawCircleHole(false)
                lineDataSet3.valueTextSize = 10f
                val lineData3 = LineData(lineDataSet3)
                time_day_chart.data = lineData3
                time_day_chart.setVisibleXRangeMaximum(7F)
                time_day_chart.description.text = ""
                time_day_chart.legend.isEnabled = false
                time_day_chart.xAxis.setValueFormatter { value, axis ->
                    val entry = lineDataSet1.getEntryForIndex(value.toInt())
                    val date = LocalDate.parse(entry.data.toString())
                    if(value !=  lineDataSet3.xMax)
                        date.dayOfWeek.toString()
                    else
                        date.toString()
                }
                time_day_chart.xAxis.textSize = 5f
                time_day_chart.axisRight.isEnabled = false
                time_day_chart.moveViewTo(x,0f,time_day_chart.axisLeft.axisDependency)
                time_day_chart.setDrawGridBackground(true)
                time_day_chart.setGridBackgroundColor(Color.BLACK)
                time_day_chart.setBackgroundColor(Color.parseColor("#1f1f1f"))
                time_day_chart.axisLeft.textColor = Color.WHITE
                time_day_chart.xAxis.textColor = Color.WHITE
            }

            // Modify view
            activity.runOnUiThread {
                n_jumps.text = stats["total_reps"].toString()
                day_streak.text = stats["streak"].toString()

                // refresh chart
                jumps_day_chart.notifyDataSetChanged()
                jumps_day_chart.invalidate()

                calories_day_chart.notifyDataSetChanged()
                calories_day_chart.invalidate()

                time_day_chart.notifyDataSetChanged()
                time_day_chart.invalidate()

                Log.d(TAG,"Done with stats")


            }
        }
    }

    /**
     * Calculates burned calories, acording to exercise and height and weight of the user
     */
    private fun calculate_burned_calories(seconds:Long,weight:Float,height:Float,met:Float): Float {
        val burned = (((met * weight * 3.5) / 200) / 60) * seconds
        Log.d(TAG,"Burned $burned in $seconds seconds")
        return burned.toFloat()
    }


}