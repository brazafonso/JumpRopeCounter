package com.example.jumpropecounter.Hub.Fragments

import android.R.attr.bitmap
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.jumpropecounter.R
import com.example.jumpropecounter.User.User
import com.example.jumpropecounter.Utils.General
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.net.URL


class Settings : Fragment() {
    private var TAG = "Settings"
    private lateinit var activity: FragmentActivity
    private lateinit var user: User
    private lateinit var username: TextView
    private lateinit var profile_pic: ImageView
    private lateinit var change_username: TextInputEditText
    private lateinit var confirm_change_username:ImageButton
    private lateinit var change_age: TextInputEditText
    private lateinit var confirm_change_age:ImageButton
    private lateinit var change_weight: TextInputEditText
    private lateinit var confirm_change_weight:ImageButton
    private lateinit var change_height: TextInputEditText
    private lateinit var confirm_change_height:ImageButton
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
        Log.d(TAG,"In settings fragment")

        username = activity.findViewById(R.id.settings_username)
        profile_pic = activity.findViewById(R.id.profileimage)

        change_username = activity.findViewById(R.id.change_u)
        confirm_change_username = activity.findViewById(R.id.change_u_confirm)

        change_age = activity.findViewById(R.id.change_a)
        confirm_change_age = activity.findViewById(R.id.change_a_confirm)

        change_weight = activity.findViewById(R.id.change_w)
        confirm_change_weight = activity.findViewById(R.id.change_w_confirm)

        change_height = activity.findViewById(R.id.change_h)
        confirm_change_height = activity.findViewById(R.id.change_h_confirm)

        reset_data = activity.findViewById(R.id.reset_data)

        update_page()

        confirm_change_username.setOnClickListener {
            val new_username = change_username.text.toString()
            Log.d(TAG,"New username: $new_username")
            if( new_username == user.username){
                Toast.makeText(requireContext(),"Input a new username.",Toast.LENGTH_SHORT).show()
            }else{
                user.change_username(new_username)
                Toast.makeText(requireContext(),"Updated",Toast.LENGTH_SHORT).show()
                update_page()
            }
        }

        confirm_change_age.setOnClickListener {
            val new_age = change_age.text.toString().toInt()
            Log.d(TAG,"New age: $new_age")
            if( new_age == user.age){
                Toast.makeText(requireContext(),"Input a new age.",Toast.LENGTH_SHORT).show()
            }else{
                user.change_age(new_age)
                Toast.makeText(requireContext(),"Updated",Toast.LENGTH_SHORT).show()
                update_page()
            }
        }

        confirm_change_weight.setOnClickListener {
            val new_weight = change_weight.text.toString().toFloat()
            Log.d(TAG,"New weight: $new_weight")
            if( new_weight == user.weight){
                Toast.makeText(requireContext(),"Input a new weight.",Toast.LENGTH_SHORT).show()
            }else{
                user.change_weight(new_weight)
                Toast.makeText(requireContext(),"Updated",Toast.LENGTH_SHORT).show()
                update_page()
            }
        }

        confirm_change_height.setOnClickListener {
            val new_height = change_height.text.toString().toFloat()
            Log.d(TAG,"New height: $new_height")
            if( new_height == user.height){
                Toast.makeText(requireContext(),"Input a new height.",Toast.LENGTH_SHORT).show()
            }else{
                user.change_height(new_height)
                Toast.makeText(requireContext(),"Updated",Toast.LENGTH_SHORT).show()
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
        Log.d(TAG,"User ${user.username} ${user.email} ${user.age} ${user.height} ${user.weight}")
        username.text = user.username
        change_username.hint = user.username
        change_age.hint = user.age.toString()
        change_height.hint = user.height.toString()
        change_weight.hint = user.weight.toString()
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
