package com.example.jumpropecounter.Hub.Fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.jumpropecounter.Exercise.JumpRope
import com.example.jumpropecounter.R
import com.example.jumpropecounter.User.User


class Exercise : Fragment() {
    private val TAG = "Exercise Fragment"
    private lateinit var admin_btn:Button
    private lateinit var jumpRopeButton:Button
    private lateinit var user:User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        user = requireArguments().getParcelable("user")!!
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        jumpRopeButton = requireActivity().findViewById<Button>(R.id.jumping_rope)
        admin_btn = requireActivity().findViewById<Button>(R.id.admin_capture_btn)

        // Enable admin button for admins
        if(user.permission_level == 1)
            admin_btn.visibility = View.VISIBLE

        jumpRopeButton.setOnClickListener {
            Log.d(TAG,"Going to Jump Rope activity")
            val intent = Intent(activity,JumpRope::class.java)
            intent.putExtra("MODE",0)
            startActivity(intent)
        }

        admin_btn.setOnClickListener {
            Log.d(TAG,"Going to Jump Rope activity")
            val intent = Intent(activity,JumpRope::class.java)
            intent.putExtra("MODE",1)
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
        fun newInstance(u: User): Exercise {
            val fragment = Exercise()
            val args = Bundle()
            args.putParcelable("user", u)
            fragment.arguments = args
            return fragment
        }
    }
}