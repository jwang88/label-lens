package com.android.labellens

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.android.labellens.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)

        val navController = this.findNavController(R.id.navHostFragment)

        val frag = MainMenu()

        val manager = fragmentManager

        val frag_transaction = manager.beginTransaction()

        frag_transaction.replace(R.id.navHostFragment,frag)
        frag_transaction.commit()


    }

}
