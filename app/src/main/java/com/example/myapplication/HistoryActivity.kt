package com.example.myapplication


import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoHistory: TextView
    private lateinit var btnDeleteAll: Button
    private lateinit var adapter: HistoryAdapter
    private var historyList = mutableListOf<MainActivity.MatchHistory>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Historique des Matchs"

        recyclerView = findViewById(R.id.recyclerHistory)
        tvNoHistory = findViewById(R.id.tvNoHistory)
        btnDeleteAll = findViewById(R.id.btnDeleteAll)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(historyList) { position ->
            showDeleteDialog(position)
        }
        recyclerView.adapter = adapter

        btnDeleteAll.setOnClickListener { showDeleteAllDialog() }

        loadHistory()
    }

    private fun loadHistory() {
        val prefs = getSharedPreferences("TennisAppPrefs", Context.MODE_PRIVATE)
        val json = prefs.getString("match_history", "[]")
        val type = object : TypeToken<List<MainActivity.MatchHistory>>() {}.type
        historyList.clear()
        historyList.addAll(Gson().fromJson(json, type))

        if (historyList.isEmpty()) {
            tvNoHistory.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            btnDeleteAll.visibility = View.GONE
        } else {
            tvNoHistory.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            btnDeleteAll.visibility = View.VISIBLE
        }

        adapter.notifyDataSetChanged()
    }

    private fun showDeleteDialog(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Supprimer le Match")
            .setMessage("Voulez-vous supprimer ce match de l'historique?")
            .setPositiveButton("Supprimer") { _, _ ->
                deleteMatch(position)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showDeleteAllDialog() {
        AlertDialog.Builder(this)
            .setTitle("Supprimer Tout l'Historique")
            .setMessage("Voulez-vous supprimer tous les matchs de l'historique?\n(${historyList.size} matchs)")
            .setPositiveButton("Tout Supprimer") { _, _ ->
                deleteAllHistory()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun deleteMatch(position: Int) {
        historyList.removeAt(position)
        val prefs = getSharedPreferences("TennisAppPrefs", Context.MODE_PRIVATE)
        val json = Gson().toJson(historyList)
        prefs.edit().putString("match_history", json).apply()
        adapter.notifyItemRemoved(position)

        if (historyList.isEmpty()) {
            tvNoHistory.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            btnDeleteAll.visibility = View.GONE
        }
    }

    private fun deleteAllHistory() {
        historyList.clear()
        getSharedPreferences("TennisAppPrefs", Context.MODE_PRIVATE)
            .edit().putString("match_history", "[]").apply()

        adapter.notifyDataSetChanged()
        tvNoHistory.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        btnDeleteAll.visibility = View.GONE

        Toast.makeText(this, "Tous les matchs ont été supprimés", Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class HistoryAdapter(
        private val matches: List<MainActivity.MatchHistory>,
        private val onLongClick: (Int) -> Unit
    ) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val tvPlayer1: TextView = view.findViewById(R.id.tvPlayer1)
            val tvPlayer2: TextView = view.findViewById(R.id.tvPlayer2)
            val tvScore: TextView = view.findViewById(R.id.tvScore)
            val tvWinner: TextView = view.findViewById(R.id.tvWinner)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val match = matches[position]
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

            holder.tvDate.text = dateFormat.format(Date(match.date))
            holder.tvPlayer1.text = match.player1Name
            holder.tvPlayer2.text = match.player2Name
            holder.tvScore.text = "${match.player1Sets} - ${match.player2Sets}"
            holder.tvWinner.text = "🏆 ${match.winner}"

            holder.itemView.setOnLongClickListener {
                onLongClick(position)
                true
            }
        }

        override fun getItemCount() = matches.size
    }
}