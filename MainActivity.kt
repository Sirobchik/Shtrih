package com.example.shtrih2

//Импорт подключаемых библиотек
//import android.R
import android.content.Intent
import android.net.Uri//Работа с файлами
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook//Чтение эксель файла
import com.example.shtrih2.R
import com.journeyapps.barcodescanner.ScanContract//Сканирование
import com.journeyapps.barcodescanner.ScanOptions

//Главный экран приложения
class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<Swag>()
    private val database by lazy {viewModel.database}
    private var tvResult: TextView? = null//Вывод результата

    var filePicker: ActivityResultLauncher<String?>? = null//Запуск работы файла
    //Запуск сканера
    private val scaner=registerForActivityResult(ScanContract()){
        //Поиск штрих-кода в базе
        if (it.getContents() != null) {
            val code = it.getContents()
            val product = database.get(code)

            tvResult!!.setText(code + " → " + product)//Запись результата
//            tvResult!!.setText("Excel загружен: " + database.size + " записей")
        } else {
            tvResult!!.setText("Сканирование отменено")
        }
    }
    //Запуск и работа программы и кнопок
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
            filePicker!!.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")//Позволяет вручную выбрать файл
        })

        btnScan.setOnClickListener(View.OnClickListener { v: View? -> startScan() })//Запуск сканера
    }

    private fun loadExcel(uri: Uri) {//Загрузка эксель файла
        try {
            val inputStream = getContentResolver().openInputStream(uri)//Открытие файла
            val workbook: Workbook = XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(0)//Просмотр листа

            database.clear()

            for (row in sheet) {//Цикл просмотра вхес столбцов и строк
                val barcodeCell = row.getCell(2)//запись третьего столбца
                val nameCell = row.getCell(0)//запись первого столбца
                try {
                    if (barcodeCell != null && nameCell != null) {
                        val barcode = barcodeCell.stringCellValue.trim()
                        val name = nameCell.numericCellValue.toString()
                        Log.e("System.err","${barcode} ${name}")
                        database.put(barcode, name)//Сохранение данных
                    }

                }catch (e: Exception){}

//                if (barcodeCell != null && nameCell != null) {
//                    val barcode = barcodeCell.richStringCellValue.string.trim()
//                    val name = nameCell.stringCellValue
//                    database.put(barcode, name)
                }


            tvResult!!.setText("Excel загружен: " + database.size + " записей")//Вывод результата
        } catch (e: Exception) {
            e.printStackTrace()
            tvResult!!.setText("Ошибка загрузки Excel")
        }
    }
    //Запуск сканера
    private fun startScan() {
        scanLauncher.launch(ScanOptions())
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
}
