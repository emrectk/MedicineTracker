package com.example.medicinetracker

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        setContent {
            MedicineApp()
        }
    }

    private fun createNotificationChannel() {
        val name = "Medicine Reminder"
        val descriptionText = "Channel for medicine reminders"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("med_channel", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

@Composable
fun MedicineApp(viewModel: MedicineViewModel = viewModel()) {
    var name by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("İlaç Takip Uygulaması") })
        },
        content = {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("İlaç Adı") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Saat (HH:mm)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    viewModel.addMedicine(name, time, it.context)
                    name = ""
                    time = ""
                }) {
                    Text("İlaç Ekle")
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(viewModel.medicines.size) { index ->
                        val medicine = viewModel.medicines[index]
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = medicine.name, style = MaterialTheme.typography.titleMedium)
                                    Text(text = "Saat: ${medicine.time}")
                                }
                                Checkbox(
                                    checked = medicine.taken,
                                    onCheckedChange = { viewModel.toggleTaken(index) }
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

class MedicineViewModel : ViewModel() {
    var medicines = mutableStateListOf<Medicine>()
        private set

    fun addMedicine(name: String, time: String, context: Context) {
        val med = Medicine(name, time)
        medicines.add(med)
        scheduleNotification(context, med, medicines.lastIndex)
    }

    fun toggleTaken(index: Int) {
        val med = medicines[index].copy(taken = !medicines[index].taken)
        medicines[index] = med
    }

    private fun scheduleNotification(context: Context, med: Medicine, id: Int) {
        val parts = med.time.split(":")
        if (parts.size != 2) return

        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_MONTH, 1)
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("med_name", med.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
}

data class Medicine(
    val name: String,
    val time: String,
    val taken: Boolean = false
)

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medName = intent.getStringExtra("med_name") ?: return

        val notification = Notification.Builder(context, "med_channel")
            .setContentTitle("İlaç Zamanı")
            .setContentText("$medName ilacını alma zamanı!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(Random().nextInt(), notification)
    }
}
