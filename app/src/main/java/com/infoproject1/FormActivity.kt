package com.infoproject1

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.infoproject1.app.databinding.ActivityFormBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFormBinding

    private var employees: List<Employee> = emptyList()
    private var auditors: List<Auditor> = emptyList()
    private var unitLocations: List<UnitLocation> = emptyList()

    private var allUnits: List<String> = emptyList()
    private var allLocations: List<String> = emptyList()

    private var suppressDropdownReopen = false

    private val positiveOptions = listOf(
        "Pozitív észrevétel 1",
        "Pozitív észrevétel 2",
        "Pozitív észrevétel 3",
        "Pozitív észrevétel 4",
        "Pozitív észrevétel 5",
        "Pozitív észrevétel 6",
        "Pozitív észrevétel 7",
        "Pozitív észrevétel 8",
        "Pozitív észrevétel 9",
        "Pozitív észrevétel 10"
    )

    private val negativeOptions = listOf(
        "Negatív észrevétel 1",
        "Negatív észrevétel 2",
        "Negatív észrevétel 3",
        "Negatív észrevétel 4",
        "Negatív észrevétel 5",
        "Negatív észrevétel 6",
        "Negatív észrevétel 7",
        "Negatív észrevétel 8",
        "Negatív észrevétel 9",
        "Negatív észrevétel 10"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val storage = DataStorage(this)
        employees = storage.loadEmployees()
        auditors = storage.loadAuditors()
        unitLocations = storage.loadUnitLocations()

        allUnits = unitLocations.map { it.unit }.distinct()
        allLocations = unitLocations.map { it.location }.distinct()

        setupAlwaysShowBehavior(binding.autoDriverName)
        setupAlwaysShowBehavior(binding.autoInspector)
        setupAlwaysShowBehavior(binding.autoLine)
        setupAlwaysShowBehavior(binding.autoStartLocation)
        setupAlwaysShowBehavior(binding.autoEndLocation)

        bindItems(binding.autoDriverName, employees.map { it.name })
        bindItems(binding.autoInspector, auditors.map { buildAuditorDisplay(it) })
        bindItems(binding.autoLine, allUnits)
        bindItems(binding.autoStartLocation, allLocations)
        bindItems(binding.autoEndLocation, allLocations)

        binding.autoDriverName.setOnItemClickListener { parent, _, position, _ ->
            suppressDropdownReopen = true
            val selectedName = parent.getItemAtPosition(position)?.toString().orEmpty()
            binding.autoDriverName.setText(selectedName, false)
            fillEmployeeCodeByName(selectedName)
            binding.autoDriverName.dismissDropDown()
            binding.autoDriverName.post { suppressDropdownReopen = false }
        }

        binding.autoDriverName.doAfterTextChanged { editable ->
            val typedName = editable?.toString().orEmpty()
            fillEmployeeCodeByName(typedName)

            if (binding.autoDriverName.hasFocus() && !suppressDropdownReopen) {
                binding.autoDriverName.post {
                    if (!suppressDropdownReopen) binding.autoDriverName.showDropDown()
                }
            }
        }

        binding.autoInspector.setOnItemClickListener { parent, _, position, _ ->
            suppressDropdownReopen = true
            val selectedDisplay = parent.getItemAtPosition(position)?.toString().orEmpty()
            binding.autoInspector.setText(selectedDisplay, false)
            binding.autoInspector.dismissDropDown()
            binding.autoInspector.post { suppressDropdownReopen = false }
        }

        binding.autoInspector.doAfterTextChanged { editable ->
            val typed = editable?.toString()?.trim().orEmpty()
            val exactAuditor = auditors.firstOrNull {
                it.name.equals(typed, ignoreCase = true) ||
                        buildAuditorDisplay(it).equals(typed, ignoreCase = true)
            }

            if (exactAuditor != null) {
                val formatted = buildAuditorDisplay(exactAuditor)
                if (typed != formatted) {
                    suppressDropdownReopen = true
                    binding.autoInspector.setText(formatted, false)
                    binding.autoInspector.setSelection(formatted.length)
                    binding.autoInspector.post { suppressDropdownReopen = false }
                }
            }

            if (binding.autoInspector.hasFocus() && !suppressDropdownReopen) {
                binding.autoInspector.post {
                    if (!suppressDropdownReopen) binding.autoInspector.showDropDown()
                }
            }
        }

        binding.autoLine.setOnItemClickListener { parent, _, position, _ ->
            suppressDropdownReopen = true
            val unit = parent.getItemAtPosition(position)?.toString()
            binding.autoLine.setText(unit.orEmpty(), false)
            updateLocationsForUnit(unit)
            binding.autoLine.dismissDropDown()
            binding.autoLine.post { suppressDropdownReopen = false }
        }

        binding.autoLine.doAfterTextChanged { editable ->
            val typed = editable?.toString()?.trim().orEmpty()
            val exactUnit = allUnits.firstOrNull { it.equals(typed, ignoreCase = true) }
            updateLocationsForUnit(exactUnit)

            if (binding.autoLine.hasFocus() && !suppressDropdownReopen) {
                binding.autoLine.post {
                    if (!suppressDropdownReopen) binding.autoLine.showDropDown()
                }
            }
        }

        binding.autoStartLocation.setOnItemClickListener { parent, _, position, _ ->
            suppressDropdownReopen = true
            val selected = parent.getItemAtPosition(position)?.toString().orEmpty()
            binding.autoStartLocation.setText(selected, false)
            binding.autoStartLocation.dismissDropDown()
            binding.autoStartLocation.post { suppressDropdownReopen = false }
        }

        binding.autoEndLocation.setOnItemClickListener { parent, _, position, _ ->
            suppressDropdownReopen = true
            val selected = parent.getItemAtPosition(position)?.toString().orEmpty()
            binding.autoEndLocation.setText(selected, false)
            binding.autoEndLocation.dismissDropDown()
            binding.autoEndLocation.post { suppressDropdownReopen = false }
        }

        setupObservationField(binding.txtPositive, "Pozitív észrevételek", positiveOptions)
        setupObservationField(binding.txtNegative, "Negatív észrevételek", negativeOptions)

        setupTimeField(binding.editStartTime)
        setupTimeField(binding.editEndTime)

        binding.btnGeneratePdf.setOnClickListener {
            generatePdf()
        }
    }

    private fun setupAlwaysShowBehavior(view: AutoCompleteTextView) {
        view.threshold = 0

        view.setOnClickListener {
            if (!suppressDropdownReopen) {
                view.showDropDown()
            }
        }

        view.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !suppressDropdownReopen) {
                view.post {
                    if (!suppressDropdownReopen) view.showDropDown()
                }
            }
        }

        view.doAfterTextChanged {
            if (view.hasFocus() && !suppressDropdownReopen) {
                view.post {
                    if (!suppressDropdownReopen) view.showDropDown()
                }
            }
        }
    }

    private fun bindItems(view: AutoCompleteTextView, items: List<String>) {
        view.setAdapter(AlwaysShowArrayAdapter(this, items.distinct()))
    }

    private fun updateLocationsForUnit(unit: String?) {
        val locations =
            if (unit.isNullOrBlank()) {
                allLocations
            } else {
                unitLocations
                    .filter { it.unit.equals(unit, ignoreCase = true) }
                    .map { it.location }
                    .distinct()
                    .ifEmpty { allLocations }
            }

        bindItems(binding.autoStartLocation, locations)
        bindItems(binding.autoEndLocation, locations)
    }

    private fun fillEmployeeCodeByName(name: String) {
        val cleanName = name.trim()

        val match = employees.firstOrNull {
            it.name.equals(cleanName, ignoreCase = true)
        }

        binding.autoDriverCode.setText(match?.employeeId.orEmpty())
    }

    private fun buildAuditorDisplay(auditor: Auditor): String {
        return "${auditor.name} (${auditor.auditorId})"
    }

    private fun extractAuditorData(display: String): Pair<String, String> {
        val trimmed = display.trim()

        val exactDisplayMatch = auditors.firstOrNull {
            buildAuditorDisplay(it).equals(trimmed, ignoreCase = true)
        }
        if (exactDisplayMatch != null) {
            return exactDisplayMatch.name to exactDisplayMatch.auditorId
        }

        val exactNameMatch = auditors.firstOrNull {
            it.name.equals(trimmed, ignoreCase = true)
        }
        if (exactNameMatch != null) {
            return exactNameMatch.name to exactNameMatch.auditorId
        }

        return trimmed to ""
    }

    private fun setupObservationField(
        view: TextView,
        title: String,
        options: List<String>
    ) {
        view.isFocusable = false
        view.isFocusableInTouchMode = false
        view.isClickable = true
        view.isLongClickable = false

        view.setOnClickListener {
            showObservationDialog(view, title, options)
        }
    }

    private fun showObservationDialog(
        targetView: TextView,
        title: String,
        options: List<String>
    ) {
        val currentSelections = targetView.text.toString()
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

        val checkedItems = BooleanArray(options.size) { index ->
            options[index] in currentSelections
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMultiChoiceItems(options.toTypedArray(), checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("OK") { dialog, _ ->
                val selected = options.filterIndexed { index, _ -> checkedItems[index] }
                targetView.text = selected.joinToString("\n")
                dialog.dismiss()
            }
            .setNegativeButton("Mégse") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Törlés") { dialog, _ ->
                targetView.text = ""
                dialog.dismiss()
            }
            .show()
    }

    private fun setupTimeField(view: TextView) {
        view.isFocusable = false
        view.isFocusableInTouchMode = false
        view.isClickable = true
        view.isLongClickable = false

        view.setOnClickListener {
            showTimePicker(view)
        }
    }

    private fun showTimePicker(targetView: TextView) {
        val calendar = Calendar.getInstance()

        val existing = targetView.text.toString().trim()
        if (isValidTime(existing)) {
            val parts = existing.split(":")
            calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
            calendar.set(Calendar.MINUTE, parts[1].toInt())
        }

        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                targetView.text = String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    hourOfDay,
                    minute
                )
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun isValidTime(value: String): Boolean {
        val regex = Regex("^\\d{2}:\\d{2}$")
        if (!regex.matches(value)) return false

        val parts = value.split(":")
        val hour = parts[0].toIntOrNull() ?: return false
        val minute = parts[1].toIntOrNull() ?: return false

        return hour in 0..23 && minute in 0..59
    }

    private fun parseSelectedLines(text: String): List<String> {
        return text.split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun generatePdf() {
        val auditorDisplay = binding.autoInspector.text.toString().trim()
        val (auditorName, auditorCode) = extractAuditorData(auditorDisplay)

        val employee = binding.autoDriverName.text.toString().trim()
        val employeeId = binding.autoDriverCode.text.toString().trim()

        val unit = binding.autoLine.text.toString().trim()
        val startLocation = binding.autoStartLocation.text.toString().trim()
        val endLocation = binding.autoEndLocation.text.toString().trim()
        val startTime = binding.editStartTime.text.toString().trim()
        val endTime = binding.editEndTime.text.toString().trim()
        val notes = binding.editNotes.text.toString().trim()

        val positives = parseSelectedLines(binding.txtPositive.text.toString())
        val negatives = parseSelectedLines(binding.txtNegative.text.toString())

        if (auditorName.isBlank()) {
            toast("Az ellenőrző személy mező kitöltése kötelező.")
            return
        }

        if (employee.isBlank()) {
            toast("Az ellenőrzött személy mező kitöltése kötelező.")
            return
        }

        if (employeeId.isBlank()) {
            toast("Az ellenőrzött személy azonosítója nincs kitöltve.")
            return
        }

        if (unit.isBlank()) {
            toast("A szervezeti egység mező kitöltése kötelező.")
            return
        }

        if (startLocation.isBlank() || endLocation.isBlank()) {
            toast("Az ellenőrzés helyét ki kell választani.")
            return
        }

        if (!isValidTime(startTime) || !isValidTime(endTime)) {
            toast("Csak érvényes időpont adható meg (HH:mm).")
            return
        }

        val today = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())

        val reportData = ReportData(
            headerTitle = "Ellenőrzési jelentés",
            inspectorName = auditorName,
            inspectorCode = auditorCode,
            driverName = employee,
            driverCode = employeeId,
            line = unit,
            startLoc = startLocation,
            startTime = startTime,
            endLoc = endLocation,
            endTime = endTime,
            dateStr = today,
            positives = positives,
            negatives = negatives,
            notes = notes
        )

        val file = PdfLayout.render(this, reportData)
        FileUtils.openPdf(this, file)
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}