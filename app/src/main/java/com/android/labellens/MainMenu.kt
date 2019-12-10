package com.android.labellens

import android.os.Bundle
import android.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import kotlin.system.exitProcess


/**
 * A simple [Fragment] subclass.
 */
class MainMenu : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val returning =  inflater.inflate(R.layout.fragment_main_menu, container, false)

        val goToCameraButton = returning.findViewById<Button>(R.id.CameraButton)
        goToCameraButton.setOnClickListener() {
            val frag2 = MainMenu()

            val manager = fragmentManager

            val frag_transaction = manager.beginTransaction()

            frag_transaction.replace(R.id.navHostFragment, frag2)
            frag_transaction.commit()
        }

        val exitButton = returning.findViewById<Button>(R.id.ExitButton)
        exitButton.setOnClickListener() {
            exitProcess(1)
        }

        return returning
    }


}
