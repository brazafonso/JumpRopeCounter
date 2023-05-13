package com.example.jumpropecounter.Hub.Fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.jumpropecounter.R
import com.example.jumpropecounter.User.User
import kotlin.io.path.Path

class Home:Fragment() {
    var TAG = "Home"
    lateinit var user:User
    lateinit var logout_btn:Button

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

        logout_btn.setOnClickListener { _ ->
            Log.d(TAG,"Logout button")
            user.sign_out()
            requireActivity().finish()
        }

    }



}