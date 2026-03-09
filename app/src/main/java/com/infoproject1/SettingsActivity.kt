package com.infoproject1

import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.infoproject1.app.R
import com.google.android.material.snackbar.Snackbar

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: AppPreferences

    private lateinit var txtDrivers: TextView
    private lateinit var txtLocations: TextView
    private lateinit var txtInspectors: TextView

    private val pickEmployeeFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                takePersistable(it)
                prefs.saveEmployeeFile(it.toString())
                txtDrivers.text = "Munkavállalók fájl: $it"
            }
        }

    private val pickLocationFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                takePersistable(it)
                prefs.saveLocationFile(it.toString())
                txtLocations.text = "Helyszínek fájl: $it"
            }
        }

    private val pickAuditorFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                takePersistable(it)
                prefs.saveAuditorFile(it.toString())
                txtInspectors.text = "Auditorok fájl: $it"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = AppPreferences(this)

        val btnPickDrivers = findViewById<Button>(R.id.btnPickDrivers)
        val btnPickLocations = findViewById<Button>(R.id.btnPickLocations)
        val btnPickInspectors = findViewById<Button>(R.id.btnPickInspectors)
        val btnUpload = findViewById<Button>(R.id.btnUpload)

        txtDrivers = findViewById(R.id.txtDrivers)
        txtLocations = findViewById(R.id.txtLocations)
        txtInspectors = findViewById(R.id.txtInspectors)

        btnPickDrivers.setOnClickListener {
            pickEmployeeFile.launch(arrayOf("text/plain"))
        }

        btnPickLocations.setOnClickListener {
            pickLocationFile.launch(arrayOf("text/plain"))
        }

        btnPickInspectors.setOnClickListener {
            pickAuditorFile.launch(arrayOf("text/plain"))
        }

        btnUpload.setOnClickListener {
            val employees = mutableListOf<Employee>()
            val unitLocations = mutableListOf<UnitLocation>()
            val auditors = mutableListOf<Auditor>()

            prefs.getEmployeeFile()?.let { uriString ->
                val lines = FileUtils.readTextFile(this, Uri.parse(uriString))
                for (line in lines) {
                    val parts = line.split(",")
                    if (parts.size >= 2) {
                        val name = parts[0].trim()
                        val employeeId = parts.subList(1, parts.size).joinToString(",").trim()
                        employees.add(Employee(name, employeeId))
                    }
                }
            }

            prefs.getLocationFile()?.let { uriString ->
                val lines = FileUtils.readTextFile(this, Uri.parse(uriString))
                for (line in lines) {
                    val parts = line.split(",")
                    if (parts.size >= 2) {
                        val unit = parts[0].trim()
                        val location = parts.subList(1, parts.size).joinToString(",").trim()
                        unitLocations.add(UnitLocation(unit, location))
                    }
                }
            }

            prefs.getAuditorFile()?.let { uriString ->
                val lines = FileUtils.readTextFile(this, Uri.parse(uriString))
                for (line in lines) {
                    val parts = line.split(",")
                    if (parts.size >= 2) {
                        val name = parts[0].trim()
                        val auditorId = parts[1].trim()
                        auditors.add(Auditor(name, auditorId))
                    }
                }
            }

            val storage = DataStorage(this)
            storage.saveEmployees(employees)
            storage.saveUnitLocations(unitLocations)
            storage.saveAuditors(auditors)

            Snackbar.make(
                findViewById(android.R.id.content),
                "Feldolgozva: ${employees.size} munkavállaló, ${unitLocations.size} helyszín, ${auditors.size} auditor",
                Snackbar.LENGTH_SHORT
            ).show()

            if (employees.isNotEmpty() && unitLocations.isNotEmpty() && auditors.isNotEmpty()) {
                startActivity(Intent(this, FormActivity::class.java))
            }
        }

        txtDrivers.text = prefs.getEmployeeFile() ?: "Nincs kiválasztva"
        txtLocations.text = prefs.getLocationFile() ?: "Nincs kiválasztva"
        txtInspectors.text = prefs.getAuditorFile() ?: "Nincs kiválasztva"
    }

    private fun takePersistable(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
    }
}
