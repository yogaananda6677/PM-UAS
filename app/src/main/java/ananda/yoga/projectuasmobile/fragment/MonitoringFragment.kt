package ananda.yoga.projectuasmobile.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ListView
import android.widget.Spinner
import androidx.fragment.app.Fragment
import ananda.yoga.projectuasmobile.R

class MonitoringFragment : Fragment() {

    private lateinit var actCariPs: AutoCompleteTextView
    private lateinit var spStatus: Spinner
    private lateinit var lvMonitoring: ListView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view =
            inflater.inflate(
                R.layout.fragment_monitoring,
                container,
                false
            )

        actCariPs =
            view.findViewById(R.id.actCariPs)

        spStatus =
            view.findViewById(R.id.spStatus)

        lvMonitoring =
            view.findViewById(R.id.lvMonitoring)

        setupSpinner()

        setupAutoComplete()

        setupDummyData()

        return view
    }

    private fun setupSpinner() {

        val data = arrayOf(
            "Semua",
            "Tersedia",
            "Digunakan",
            "Maintenance"
        )

        val adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                data
            )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spStatus.adapter = adapter
    }

    private fun setupAutoComplete() {

        val data = arrayOf(
            "PS 1",
            "PS 2",
            "PS 3",
            "PS 4",
            "PS 5"
        )

        val adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                data
            )

        actCariPs.setAdapter(adapter)
    }

    private fun setupDummyData() {

        val data = arrayOf(
            "PS 1 | PS5 | Tersedia",
            "PS 2 | PS4 | Digunakan",
            "PS 3 | PS5 | Maintenance"
        )

        val adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                data
            )

        lvMonitoring.adapter = adapter
    }
}