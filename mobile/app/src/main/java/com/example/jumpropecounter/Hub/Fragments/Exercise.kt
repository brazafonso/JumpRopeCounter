package com.example.jumpropecounter.Hub.Fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.jumpropecounter.Exercise.JumpRope
import com.example.jumpropecounter.R


class Exercise : Fragment() {
    val TAG = "Exercise Fragment"

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val jumpRopeButton = requireActivity().findViewById<Button>(R.id.jumping_rope)
        jumpRopeButton.setOnClickListener {
            Log.d(TAG,"Going to Jump Rope activity")
            val intent = Intent(activity,JumpRope::class.java)
            intent.putExtra("MODE",0)
            startActivity(intent)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_exercise, container, false)
    }

    companion object {

        @JvmStatic
        fun newInstance() = Exercise()
    }
}