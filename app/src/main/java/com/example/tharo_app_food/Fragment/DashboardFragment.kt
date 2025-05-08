import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.tharo_app_food.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.util.*

class DashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        setupLineChart(view)
        setupPieChart(view)

        return view
    }

    private fun setupLineChart(view: View) {
        val lineChart = view.findViewById<LineChart>(R.id.lineChart)

        // Sample data
        val entries = ArrayList<Entry>()
        entries.add(Entry(0f, 100f))
        entries.add(Entry(1f, 200f))
        entries.add(Entry(2f, 150f))
        entries.add(Entry(3f, 300f))
        entries.add(Entry(4f, 280f))
        entries.add(Entry(5f, 400f))
        entries.add(Entry(6f, 350f))

        val lineDataSet = LineDataSet(entries, "User Activity")
        lineDataSet.color = Color.parseColor("#6200EE")
        lineDataSet.valueTextColor = Color.BLACK
        lineDataSet.lineWidth = 2f
        lineDataSet.setCircleColor(Color.parseColor("#6200EE"))
        lineDataSet.circleRadius = 5f
        lineDataSet.setDrawCircleHole(false)
        lineDataSet.valueTextSize = 10f

        val lineData = LineData(lineDataSet)
        lineChart.data = lineData

        // Customize chart
        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.axisRight.isEnabled = false

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = IndexAxisValueFormatter(getWeekDays())

        val yAxis = lineChart.axisLeft
        yAxis.setDrawGridLines(false)

        lineChart.animateXY(1000, 1000)
    }

    private fun setupPieChart(view: View) {
        val pieChart = view.findViewById<PieChart>(R.id.pieChart)

        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(40f, "Android"))
        entries.add(PieEntry(30f, "iOS"))
        entries.add(PieEntry(20f, "Web"))
        entries.add(PieEntry(10f, "Other"))

        val dataSet = PieDataSet(entries, "Platform Distribution")
        dataSet.colors = listOf(
            Color.parseColor("#6200EE"),
            Color.parseColor("#03DAC5"),
            Color.parseColor("#FFC107"),
            Color.parseColor("#FF5722")
        )
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f

        val pieData = PieData(dataSet)
        pieChart.data = pieData

        // Customize chart
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setEntryLabelTextSize(12f)
        pieChart.legend.isEnabled = false
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.setTransparentCircleAlpha(0)
        pieChart.animateY(1000)
    }

    private fun getWeekDays(): Array<String> {
        val calendar = Calendar.getInstance()
        val days = Array(7) { "" }

        val dateFormat = android.text.format.DateFormat.getDateFormat(context)
        for (i in 0 until 7) {
            calendar.add(Calendar.DAY_OF_WEEK, -6 + i)
            days[i] = android.text.format.DateFormat.format("EEE", calendar).toString()
        }

        return days
    }
}