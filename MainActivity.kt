package com.example.shtrih2

//import android.R
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import com.google.zxing.integration.android.IntentIntegrator
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import com.example.shtrih2.R
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<Swag>()
    private val database by lazy {viewModel.database}
    private var tvResult: TextView? = null

    var filePicker: ActivityResultLauncher<String?>? = null
    private val scaner=registerForActivityResult(ScanContract()){
        if (it.getContents() != null) {
            val code = it.getContents()
            val product = database.get(code)

            tvResult!!.setText(code + " → " + product)
//            tvResult!!.setText("Excel загружен: " + database.size + " записей")
        } else {
            tvResult!!.setText("Сканирование отменено")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnLoad = findViewById<Button?>(R.id.btnLoad)
        val btnScan = findViewById<Button?>(R.id.btnScan)
        tvResult = findViewById<TextView?>(R.id.tvResult)

        // Выбор Excel файла
        filePicker = registerForActivityResult<String?, Uri?>(
            GetContent(),
            ActivityResultCallback { uri: Uri? ->
                if (uri != null) {
                    loadExcel(uri)
                }
            }
        )

        btnLoad.setOnClickListener(View.OnClickListener { v: View? ->
            filePicker!!.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        })

        btnScan.setOnClickListener(View.OnClickListener { v: View? -> startScan() })
    }

    private fun loadExcel(uri: Uri) {
        try {
            val inputStream = getContentResolver().openInputStream(uri)
            val workbook: Workbook = XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(0)

            database.clear()

            for (row in sheet) {
                val barcodeCell = row.getCell(2)
                val nameCell = row.getCell(0)
                try {
                    if (barcodeCell != null && nameCell != null) {
                        val barcode = barcodeCell.stringCellValue.trim()
                        val name = nameCell.numericCellValue.toString()
                        Log.e("System.err","${barcode} ${name}")
                        database.put(barcode, name)
                    }

                }catch (e: Exception){}

//                if (barcodeCell != null && nameCell != null) {
//                    val barcode = barcodeCell.richStringCellValue.string.trim()
//                    val name = nameCell.stringCellValue
//                    database.put(barcode, name)
                }


            tvResult!!.setText("Excel загружен: " + database.size + " записей")
        } catch (e: Exception) {
            e.printStackTrace()
            tvResult!!.setText("Ошибка загрузки Excel")
        }
    }

    private fun startScan() {
        scaner.launch(ScanOptions())
//        val integrator = IntentIntegrator(this)
//        integrator.setDesiredBarcodeFormats(ScanOptions.CODE_128)
//        integrator.setPrompt("Сканируйте штрих-код")
//        integrator.setBeepEnabled(true)
//        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result != null) {
            if (result.getContents() != null) {
                val code = result.getContents()
                val product = database.getOrDefault(code, "Не найдено")

                tvResult!!.setText(code + " → " + product)
            } else {
                tvResult!!.setText("Сканирование отменено")
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
class Swag: ViewModel(){
    val database = HashMap<String?, String?>()
