package com.infoproject1

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.infoproject1.app.R
import com.infoproject1.app.databinding.ActivityFormBinding
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class FormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFormBinding

    private var employees: List<Employee> = emptyList()
    private var auditors: List<Auditor> = emptyList()
    private var unitLocations: List<UnitLocation> = emptyList()

    private lateinit var employeeNameAdapter: ArrayAdapter<String>
    private lateinit var employeeIdAdapter: ArrayAdapter<String>
    private lateinit var auditorNameAdapter: ArrayAdapter<String>
    private lateinit var unitAdapter: ArrayAdapter<String>

    private val selectedPositiveItems = mutableListOf<String>()
    private val selectedNegativeItems = mutableListOf<String>()

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private var suppressEmployeeSync = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val storage = DataStorage(this)
        employees = storage.loadEmployees()
        auditors = storage.loadAuditors()
        unitLocations = storage.loadUnitLocations()

        if (employees.isEmpty() || auditors.isEmpty() || unitLocations.isEmpty()) {
            Toast.makeText(
                this,
                "Hiányoznak a mintafájlokból betöltött adatok. Előbb töltsd be őket a Beállításoknál.",
                Toast.LENGTH_LONG
            ).show()
        }

        setupEmployeeFields()
        setupAuditorField()
        setupUnitAndLocationFields()
        setupTimeFields()
        setupObservationSelectors()

        binding.btnGeneratePdf.setOnClickListener {
            generatePdf()
        }
    }

    private fun setupEmployeeFields() {
        val employeeNames = employees.map { it.name }.distinct().sorted()
        val employeeIds = employees.map { it.employeeId }.distinct().sorted()

        employeeNameAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            employeeNames
        )

        employeeIdAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            employeeIds
        )

        binding.autoDriverName.setAdapter(employeeNameAdapter)
        binding.autoDriverCode.setAdapter(employeeIdAdapter)

        binding.autoDriverName.threshold = 1
        binding.autoDriverCode.threshold = 1

        binding.autoDriverName.setOnItemClickListener { _, _, position, _ ->
            val selectedName = employeeNameAdapter.getItem(position) ?: return@setOnItemClickListener
            val employee = employees.firstOrNull { it.name == selectedName } ?: return@setOnItemClickListener

            suppressEmployeeSync = true
            binding.autoDriverName.setText(employee.name, false)
            binding.autoDriverCode.setText(employee.employeeId, false)
            suppressEmployeeSync = false
        }

        binding.autoDriverCode.setOnItemClickListener { _, _, position, _ ->
            val selectedId = employeeIdAdapter.getItem(position) ?: return@setOnItemClickListener
            val employee = employees.firstOrNull { it.employeeId == selectedId } ?: return@setOnItemClickListener

            suppressEmployeeSync = true
            binding.autoDriverCode.setText(employee.employeeId, false)
            binding.autoDriverName.setText(employee.name, false)
            suppressEmployeeSync = false
        }

        binding.autoDriverName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (suppressEmployeeSync) return
                val text = s?.toString()?.trim().orEmpty()
                val exact = employees.firstOrNull { it.name.equals(text, ignoreCase = true) }
                if (exact != null) {
                    suppressEmployeeSync = true
                    binding.autoDriverCode.setText(exact.employeeId, false)
                    suppressEmployeeSync = false
                } else if (binding.autoDriverCode.text?.isNotBlank() == true) {
                    suppressEmployeeSync = true
                    binding.autoDriverCode.setText("", false)
                    suppressEmployeeSync = false
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        binding.autoDriverCode.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (suppressEmployeeSync) return
                val text = s?.toString()?.trim().orEmpty()
                val exact = employees.firstOrNull { it.employeeId.equals(text, ignoreCase = true) }
                if (exact != null) {
                    suppressEmployeeSync = true
                    binding.autoDriverName.setText(exact.name, false)
                    suppressEmployeeSync = false
                } else if (binding.autoDriverName.text?.isNotBlank() == true) {
                    suppressEmployeeSync = true
                    binding.autoDriverName.setText("", false)
                    suppressEmployeeSync = false
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        binding.autoDriverName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateEmployeeSelection()
        }

        binding.autoDriverCode.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateEmployeeSelection()
        }
    }

    private fun setupAuditorField() {
        val auditorNames = auditors.map { "${it.name} (${it.auditorId})" }.sorted()

        auditorNameAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            auditorNames
        )

        binding.autoInspector.setAdapter(auditorNameAdapter)
        binding.autoInspector.threshold = 1

        binding.autoInspector.setOnItemClickListener { _, _, position, _ ->
            val selected = auditorNameAdapter.getItem(position) ?: return@setOnItemClickListener
            binding.autoInspector.setText(selected, false)
        }

        binding.autoInspector.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = binding.autoInspector.text.toString().trim()
                if (value.isNotEmpty() && auditors.none { "${it.name} (${it.auditorId})" == value }) {
                    binding.autoInspector.error = "Csak a betöltött auditorok közül lehet választani"
                } else {
                    binding.autoInspector.error = null
                }
            }
        }
    }

    private fun setupUnitAndLocationFields() {
        val units = unitLocations.map { it.unit }.distinct().sorted()

        unitAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            units
        )

        binding.autoLine.setAdapter(unitAdapter)
        binding.autoLine.threshold = 1

        binding.autoLine.setOnItemClickListener { _, _, position, _ ->
            val selectedUnit = unitAdapter.getItem(position) ?: return@setOnItemClickListener
            binding.autoLine.setText(selectedUnit, false)
            updateLocationsForUnit(selectedUnit)
        }

        binding.autoLine.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val unit = s?.toString()?.trim().orEmpty()
                if (units.contains(unit)) {
                    updateLocationsForUnit(unit)
                    binding.autoLine.error = null
                } else {
                    binding.autoStartLocation.setText("", false)
                    binding.autoEndLocation.setText("", false)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        binding.autoLine.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val unit = binding.autoLine.text.toString().trim()
                if (unit.isBlank()) {
                    binding.autoLine.error = "Kötelező mező"
                } else {
                    binding.autoLine.error = null
                }
            }
        }

        binding.autoStartLocation.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateLocationField(binding.autoStartLocation.text.toString().trim(), true)
        }

        binding.autoEndLocation.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateLocationField(binding.autoEndLocation.text.toString().trim(), false)
        }
    }

    private fun updateLocationsForUnit(unit: String) {
        val locations = unitLocations
            .filter { it.unit == unit }
            .map { it.location }
            .distinct()
            .sorted()

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            locations
        )

        binding.autoStartLocation.setAdapter(adapter)
        binding.autoEndLocation.setAdapter(adapter)

        binding.autoStartLocation.threshold = 1
        binding.autoEndLocation.threshold = 1

        binding.autoStartLocation.setOnItemClickListener { _, _, position, _ ->
            val selected = adapter.getItem(position) ?: return@setOnItemClickListener
            binding.autoStartLocation.setText(selected, false)
        }

        binding.autoEndLocation.setOnItemClickListener { _, _, position, _ ->
            val selected = adapter.getItem(position) ?: return@setOnItemClickListener
            binding.autoEndLocation.setText(selected, false)
        }
    }

    private fun setupTimeFields() {
        binding.editStartTime.filters = arrayOf(InputFilter.LengthFilter(5))
        binding.editEndTime.filters = arrayOf(InputFilter.LengthFilter(5))

        binding.editStartTime.addTextChangedListener(TimeMaskTextWatcher(binding.editStartTime))
        binding.editEndTime.addTextChangedListener(TimeMaskTextWatcher(binding.editEndTime))

        binding.editStartTime.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateSingleTime(binding.editStartTime.text.toString().trim(), isStart = true)
        }

        binding.editEndTime.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateSingleTime(binding.editEndTime.text.toString().trim(), isStart = false)
        }
    }

    private fun setupObservationSelectors() {
        binding.txtPositive.keyListener = null
        binding.txtNegative.keyListener = null

        binding.txtPositive.isFocusable = false
        binding.txtNegative.isFocusable = false

        binding.txtPositive.setOnClickListener {
            showMultiSelectDialog(
                title = "Pozitív észrevételek",
                items = getPositiveObservationItems(),
                selectedItems = selectedPositiveItems
            ) { selected ->
                selectedPositiveItems.clear()
                selectedPositiveItems.addAll(selected)
                binding.txtPositive.setText(selectedPositiveItems.joinToString("\n"))
            }
        }

        binding.txtNegative.setOnClickListener {
            showMultiSelectDialog(
                title = "Negatív észrevételek",
                items = getNegativeObservationItems(),
                selectedItems = selectedNegativeItems
            ) { selected ->
                selectedNegativeItems.clear()
                selectedNegativeItems.addAll(selected)
                binding.txtNegative.setText(selectedNegativeItems.joinToString("\n"))
            }
        }
    }

    private fun showMultiSelectDialog(
        title: String,
        items: List<String>,
        selectedItems: MutableList<String>,
        onApply: (List<String>) -> Unit
    ) {
        val checked = BooleanArray(items.size) { index ->
            selectedItems.contains(items[index])
        }
        val tempSelected = selectedItems.toMutableList()

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMultiChoiceItems(items.toTypedArray(), checked) { _, which, isChecked ->
                val item = items[which]
                if (isChecked) {
                    if (!tempSelected.contains(item)) tempSelected.add(item)
                } else {
                    tempSelected.remove(item)
                }
            }
            .setPositiveButton("OK") { _, _ ->
                onApply(tempSelected)
            }
            .setNegativeButton("Mégse", null)
            .show()
    }

    private fun getPositiveObservationItems(): List<String> {
        return listOf(
            getString(R.string.pozitiv_eszrevetel1),
            getString(R.string.pozitiv_eszrevetel2),
            getString(R.string.pozitiv_eszrevetel3),
            getString(R.string.pozitiv_eszrevetel4),
            getString(R.string.pozitiv_eszrevetel5),
            getString(R.string.pozitiv_eszrevetel6),
            getString(R.string.pozitiv_eszrevetel7),
            getString(R.string.pozitiv_eszrevetel8),
            getString(R.string.pozitiv_eszrevetel9),
            getString(R.string.pozitiv_eszrevetel10)
        )
    }

    private fun getNegativeObservationItems(): List<String> {
        return listOf(
            getString(R.string.negativ_eszrevetel1),
            getString(R.string.negativ_eszrevetel2),
            getString(R.string.negativ_eszrevetel3),
            getString(R.string.negativ_eszrevetel4),
            getString(R.string.negativ_eszrevetel5),
            getString(R.string.negativ_eszrevetel6),
            getString(R.string.negativ_eszrevetel7),
            getString(R.string.negativ_eszrevetel8),
            getString(R.string.negativ_eszrevetel9),
            getString(R.string.negativ_eszrevetel10)
        )
    }

    private fun validateEmployeeSelection(): Boolean {
        val name = binding.autoDriverName.text.toString().trim()
        val employeeId = binding.autoDriverCode.text.toString().trim()

        val exactByName = employees.firstOrNull { it.name.equals(name, ignoreCase = true) }
        val exactById = employees.firstOrNull { it.employeeId.equals(employeeId, ignoreCase = true) }

        return when {
            name.isBlank() || employeeId.isBlank() -> {
                if (name.isBlank()) binding.autoDriverName.error = "Kötelező mező"
                if (employeeId.isBlank()) binding.autoDriverCode.error = "Kötelező mező"
                false
            }

            exactByName == null || exactById == null -> {
                binding.autoDriverName.error = "Csak a betöltött munkavállalók közül lehet választani"
                binding.autoDriverCode.error = "Csak a betöltött azonosítók közül lehet választani"
                false
            }

            exactByName.employeeId != exactById.employeeId -> {
                binding.autoDriverName.error = "A név és azonosító nem tartozik össze"
                binding.autoDriverCode.error = "A név és azonosító nem tartozik össze"
                false
            }

            else -> {
                binding.autoDriverName.error = null
                binding.autoDriverCode.error = null
                true
            }
        }
    }

    private fun validateLocationField(location: String, isStart: Boolean): Boolean {
        val unit = binding.autoLine.text.toString().trim()
        val validLocations = unitLocations
            .filter { it.unit == unit }
            .map { it.location }

        val valid = validLocations.contains(location)

        if (isStart) {
            binding.autoStartLocation.error =
                if (valid) null else "Csak az adott szervezeti egységhez tartozó helyszín választható"
        } else {
            binding.autoEndLocation.error =
                if (valid) null else "Csak az adott szervezeti egységhez tartozó helyszín választható"
        }

        return valid
    }

    private fun validateSingleTime(value: String, isStart: Boolean): Boolean {
        val valid = parseTimeOrNull(value) != null
        if (isStart) {
            binding.editStartTime.error = if (valid) null else "Érvényes időformátum: HH:mm"
        } else {
            binding.editEndTime.error = if (valid) null else "Érvényes időformátum: HH:mm"
        }
        return valid
    }

    private fun validateTimesTogether(): Boolean {
        val startTime = parseTimeOrNull(binding.editStartTime.text.toString().trim())
        val endTime = parseTimeOrNull(binding.editEndTime.text.toString().trim())

        if (startTime == null || endTime == null) {
            if (startTime == null) binding.editStartTime.error = "Érvényes időformátum: HH:mm"
            if (endTime == null) binding.editEndTime.error = "Érvényes időformátum: HH:mm"
            return false
        }

        binding.editStartTime.error = null
        binding.editEndTime.error = null
        return true
    }

    private fun parseTimeOrNull(value: String): LocalTime? {
        return try {
            LocalTime.parse(value, timeFormatter)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun generatePdf() {
        val auditorText = binding.autoInspector.text.toString().trim()
        val employeeName = binding.autoDriverName.text.toString().trim()
        val employeeId = binding.autoDriverCode.text.toString().trim()
        val unit = binding.autoLine.text.toString().trim()
        val vehicleCode = binding.editVehicleCode.text.toString().trim()
        val startLocation = binding.autoStartLocation.text.toString().trim()
        val endLocation = binding.autoEndLocation.text.toString().trim()
        val startTime = binding.editStartTime.text.toString().trim()
        val endTime = binding.editEndTime.text.toString().trim()
        val notes = binding.editNotes.text.toString().trim()

        val selectedAuditor = auditors.firstOrNull { "${it.name} (${it.auditorId})" == auditorText }

        val isValid = validateEmployeeSelection() &&
                (selectedAuditor != null).also {
                    binding.autoInspector.error =
                        if (it) null else "Csak a betöltött auditorok közül lehet választani"
                } &&
                unit.isNotBlank().also {
                    binding.autoLine.error = if (it) null else "Kötelező mező"
                } &&
                validateLocationField(startLocation, true) &&
                validateLocationField(endLocation, false) &&
                validateTimesTogether()

        if (!isValid) {
            Toast.makeText(this, "Javítsd a hibás mezőket", Toast.LENGTH_SHORT).show()
            return
        }

        val reportData = ReportData(
            headerTitle = "Ellenőrzési jelentés",
            inspectorName = selectedAuditor!!.name,
            inspectorCode = selectedAuditor.auditorId,
            driverName = employeeName,
            driverCode = employeeId,
            line = unit,
            startLoc = startLocation,
            startTime = startTime,
            endLoc = endLocation,
            endTime = endTime,
            dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")),
            positives = selectedPositiveItems.toList(),
            negatives = selectedNegativeItems.toList(),
            notes = buildString {
                if (vehicleCode.isNotBlank()) {
                    append("Eszköz azonosító: ")
                    append(vehicleCode)
                    if (notes.isNotBlank()) append("\n")
                }
                if (notes.isNotBlank()) {
                    append(notes)
                }
            }
        )

        val file = PdfLayout.render(this, reportData)
        FileUtils.openPdf(this, file)
    }

    private class TimeMaskTextWatcher(
        private val target: android.widget.EditText
    ) : TextWatcher {

        private var selfChange = false

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

        override fun afterTextChanged(s: Editable?) {
            if (selfChange) return

            val raw = s?.toString().orEmpty().filter { it.isDigit() }.take(4)
            val formatted = when {
                raw.length <= 2 -> raw
                else -> raw.substring(0, 2) + ":" + raw.substring(2)
            }

            if (formatted != s?.toString().orEmpty()) {
                selfChange = true
                target.setText(formatted)
                target.setSelection(formatted.length)
                selfChange = false
            }
        }
    }
}