package com.engage.consumer.sample

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.engage.consumer.sdk.Engage
import com.engage.consumer.sdk.EngageConfig
import com.engage.consumer.sdk.EngageEvent
import com.engage.consumer.sdk.EngageUser
import com.squareup.picasso.Picasso
import io.bloco.faker.Faker

import kotlinx.android.synthetic.main.activity_sample.*
import kotlinx.android.synthetic.main.content_sample.*
import java.util.*
import kotlin.collections.HashMap

class SampleActivity : AppCompatActivity() {

    private val inputMap: HashMap<Int, View> = HashMap()
    private lateinit var user: EngageUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)
        setSupportActionBar(toolbar)


        // Init engage
        Engage.getInstance(applicationContext)
            .init(EngageConfig("YOUR USER NAME", "YOUR SECRET (OPTIONAL)"))


        //generate random user
        generateUser.setOnClickListener {
            generateRandomUser()
        }

        //refresh user if needed
        refreshUserBtn.setOnClickListener {
            generateRandomUser()
        }

        //Identify User
        identifyBtn.setOnClickListener {
            Toast.makeText(this, "Identifying User", Toast.LENGTH_SHORT).show()
            Engage.getInstance(applicationContext).setUser(user)
        }

        // Track Event
        trackEventBtn.setOnClickListener {
            openDialog(1)
        }

        // Update User Properties
        updatePropertiesBtn.setOnClickListener {
            openDialog(2)
        }
    }


    private fun generateRandomUser(): EngageUser {
        val _user = EngageUser(System.currentTimeMillis().toString())
        val faker = Faker()
        _user.firstName = faker.name.firstName()
        _user.lastName = faker.name.lastName()
        _user.emailAddress = "${_user.firstName}.${_user.lastName}${_user.id.subSequence(0, 5)}@example.com"
        _user.phoneNumber = faker.phoneNumber.phoneNumber().replace(Regex("[^0-9]"),"")
        if(_user.phoneNumber!!.length >= 15) {
           _user.phoneNumber = _user.phoneNumber!!.substring(0, 13)
        }

        userNameTV.text = "${_user.firstName} ${_user.lastName}"
        userEmailTV.text = _user.emailAddress
        userPhoneTV.text = _user.phoneNumber
        Picasso.get().load(faker.avatar.image()).into(userImage)

        if(!this::user.isInitialized) {
            userProfileLayout.visibility = View.VISIBLE
            generateUser.visibility = View.GONE
            identifyBtn.isEnabled = true
            trackEventBtn.isEnabled = true
            updatePropertiesBtn.isEnabled = true
        }

        user = _user

        return _user
    }

    // type: 1 = Event, 2 = User prop
    private fun openDialog(type: Int) {
        val builder:AlertDialog.Builder = AlertDialog.Builder(this)

        val root: View = View.inflate(this, R.layout.layout_input_holder, null)
        val inputHolder: LinearLayout = root.findViewById(R.id.inputHolder)

        val addBtn: Button = root.findViewById(R.id.addNewBtn)

        addBtn.setOnClickListener {
            // inflate new input collector and add to parent
            val inputCollector: View = View.inflate(this, R.layout.layout_input_collector, null)
            inputCollector.id = inputHolder.childCount + 1
            val clearBtn: ImageButton = inputCollector.findViewById(R.id.clear)
            clearBtn.setOnClickListener {
                inputHolder.removeView(inputCollector)
            }
            inputHolder.addView(inputCollector)
            inputMap[inputCollector.id] = inputCollector
        }


        if(type == 1) {
            // if type is event; simulate click and hide add button
            addBtn.callOnClick()
            addBtn.visibility = View.GONE
        }

        builder.setPositiveButton("Submit") { d: DialogInterface, i: Int ->
            val map: HashMap<String, Any> = HashMap()
            // loop through all input fields and collect values
            inputMap.values.forEach {
                val name:String? = it.findViewById<EditText>(R.id.nameET)?.text.toString()
                val rawValue:String? = it.findViewById<EditText>(R.id.valueET)?.text.toString()

                if(name != null && rawValue != null) {
                    val key:String = if (type == 1) "event" else name
                    val value: Any = if(rawValue == "" ) true else rawValue
                    map[key] = value
                }

                inputHolder.removeView(it)
                inputMap.remove(it.id)
            }

            //submit values
            if(type == 1) {
                // type is event
                val event = EngageEvent(map.keys.elementAt(0), map.values.elementAt(0))
                Engage.getInstance(applicationContext).trackEvent(event)
                Toast.makeText(this, "Sending Event", Toast.LENGTH_SHORT).show()
            }else{
                // type is user prop
                Engage.getInstance(applicationContext).setUserProperties(map)
                Toast.makeText(this, "Updating user properties", Toast.LENGTH_SHORT).show()
            }
            map.clear()
            inputMap.clear()
            d.dismiss()
        }

        builder.setTitle(if (type == 1) "Add Event" else "Update user property")

        builder.setNegativeButton("Cancel") { d: DialogInterface, i: Int ->
            d.dismiss()
        }

        builder.setView(root)

        // show dialog
        builder.show()
    }



}
