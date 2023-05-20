package com.example.jumpropecounter.Hub.Fragments

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.jumpropecounter.R
import com.example.jumpropecounter.User.User
import com.google.android.material.textfield.TextInputEditText


class Settings : Fragment() {
    private var TAG = "Settings"
    private lateinit var activity: FragmentActivity
    private lateinit var user: User
    private lateinit var username: TextView
    private lateinit var change_username: TextInputEditText
    private lateinit var confirm_change_username:ImageButton
    private lateinit var reset_data:Button


    companion object {
        fun newInstance(u: User): Settings {
            val fragment = Settings()
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
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        user = requireArguments().getParcelable("user")!!
        activity = requireActivity()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        username = activity.findViewById(R.id.settings_username)
        change_username = activity.findViewById(R.id.change_u)
        confirm_change_username = activity.findViewById(R.id.change_u_confirm)
        reset_data = activity.findViewById(R.id.reset_data)

        update_page()

        confirm_change_username.setOnClickListener {
            val new_username = change_username.text.toString()
            Log.d(TAG,"New username: $new_username")
            if( new_username == user.username){
                Toast.makeText(requireContext(),"Input a new username.",Toast.LENGTH_SHORT).show()
            }else{
                user.change_username(new_username)
                update_page()
            }
        }

        reset_data.setOnClickListener {
            reset_confirmation()
        }


    }

    /**
     * Updates the page with users data
     */
    private fun update_page(){
        username.text = user.username

        change_username.hint = user.username
    }


    private fun reset_confirmation(){
        val builder= AlertDialog.Builder(requireContext())
        builder.setCancelable(true)
        builder.setTitle("Reset Data")
        builder.setMessage("Are you sure you want to reset all your data?")
        builder.setPositiveButton("Confirm"
        )
        { dialog, which ->
            Log.d(TAG, "Confirmed")
            user.reset_sessions()
        }
        builder.setNegativeButton(android.R.string.cancel
        )
        { dialog, which ->
            Log.d(TAG, "Canceled")
        }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

}
