package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private var player1Points = 0
    private var player2Points = 0
    private var player1Games = 0
    private var player2Games = 0
    private var player1Sets = 0
    private var player2Sets = 0
    private var isDeuce = false
    private var advantage = 0
    private var matchEnded = false
    private var isTieBreak = false

    private lateinit var originalBgP1: Drawable
    private lateinit var originalBgP2: Drawable
    private var player1Name = "Joueur 1"
    private var player2Name = "Joueur 2"

    private val handler = Handler(Looper.getMainLooper())
    private var randomButtonRunnable: Runnable? = null
    private val RANDOM_INTERVAL = 3000L
    private var activeButton = 0

    private var gameStarted = false
    private var gamePaused = false

    private var currentToast: Toast? = null

    private lateinit var tvPlayer1Name: TextView
    private lateinit var tvPlayer2Name: TextView
    private lateinit var tvPlayer1Score: TextView
    private lateinit var tvPlayer2Score: TextView
    private lateinit var tvPlayer1Games: TextView
    private lateinit var tvPlayer2Games: TextView
    private lateinit var tvPlayer1Sets: TextView
    private lateinit var tvPlayer2Sets: TextView
    private lateinit var btnPlayer1Point: Button
    private lateinit var btnPlayer2Point: Button
    private lateinit var btnReset: Button
    private lateinit var btnHistory: Button
    private lateinit var btnPause: Button
    private lateinit var tvWinnerMessage: TextView
    private lateinit var topPlayerContainer: View
    private lateinit var bottomPlayerContainer: View
    private lateinit var buttonsContainer: View
    private lateinit var mainLayout: View

    private val PREFS_NAME = "TennisAppPrefs"
    private val CURRENT_MATCH = "current_match"
    private val MATCH_HISTORY = "match_history"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        loadCurrentMatch()
        setupClickListeners()
        updateScoreDisplay()

        if (!matchEnded && !gameStarted) {
            gameStarted = true
            gamePaused = true
            btnPause.text = "CONTINUER"
            btnPause.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
        } else if (gameStarted && !matchEnded && !gamePaused) {
            startRandomButtonSystem()
        }
    }

    private fun initializeViews() {
        tvPlayer1Name = findViewById(R.id.tvPlayer1Name)
        tvPlayer2Name = findViewById(R.id.tvPlayer2Name)
        tvPlayer1Score = findViewById(R.id.tvPlayer1Score)
        tvPlayer2Score = findViewById(R.id.tvPlayer2Score)
        tvPlayer1Games = findViewById(R.id.tvPlayer1Games)
        tvPlayer2Games = findViewById(R.id.tvPlayer2Games)
        tvPlayer1Sets = findViewById(R.id.tvPlayer1Sets)
        tvPlayer2Sets = findViewById(R.id.tvPlayer2Sets)
        btnPlayer1Point = findViewById(R.id.btnPlayer1Point)
        btnPlayer2Point = findViewById(R.id.btnPlayer2Point)
        btnReset = findViewById(R.id.btnReset)
        btnHistory = findViewById(R.id.btnHistory)
        btnPause = findViewById(R.id.btnPause)
        tvWinnerMessage = findViewById(R.id.tvWinnerMessage)
        topPlayerContainer = findViewById(R.id.topPlayerContainer)
        bottomPlayerContainer = findViewById(R.id.bottomPlayerContainer)
        buttonsContainer = findViewById(R.id.buttonsContainer)
        mainLayout = findViewById(R.id.linearLayout)

        originalBgP1 = btnPlayer1Point.background
        originalBgP2 = btnPlayer2Point.background

        mainLayout.visibility = View.VISIBLE
        buttonsContainer.visibility = View.VISIBLE
        btnReset.visibility = View.VISIBLE
        btnPause.visibility = View.VISIBLE
    }

    private fun setupClickListeners() {
        btnPlayer1Point.setOnClickListener {
            if (!gameStarted || gamePaused || matchEnded) return@setOnClickListener
            if (activeButton == 1) {
                addPointToPlayer1()
                resetButtonColors()
                activeButton = 0
                showQuickToast("Point pour $player1Name!")
            } else {
                showQuickToast("Ce n'est pas le bon bouton!")
            }
        }

        btnPlayer2Point.setOnClickListener {
            if (!gameStarted || gamePaused || matchEnded) return@setOnClickListener
            if (activeButton == 2) {
                addPointToPlayer2()
                resetButtonColors()
                activeButton = 0
                showQuickToast("Point pour $player2Name!")
            } else {
                showQuickToast("Ce n'est pas le bon bouton!")
            }
        }

        btnReset.setOnClickListener { showResetDialog() }
        btnHistory.setOnClickListener { openHistory() }
        btnPause.setOnClickListener { togglePauseFromButton() }
        tvPlayer1Name.setOnClickListener { editPlayerName(1) }
        tvPlayer2Name.setOnClickListener { editPlayerName(2) }
    }

    private fun showQuickToast(message: String) {
        currentToast?.cancel()
        currentToast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        currentToast?.show()

        handler.postDelayed({
            currentToast?.cancel()
        }, 1000L)
    }

    private fun togglePauseFromButton() {
        if (!gameStarted || matchEnded) return

        if (gamePaused) {
            gamePaused = false
            btnPause.text = "PAUSE"
            btnPause.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF9800"))
            showQuickToast("Jeu repris!")
            startRandomButtonSystem()
        } else {
            gamePaused = true
            stopRandomButtonSystem()
            resetButtonColors()
            activeButton = 0
            btnPause.text = "CONTINUER"
            btnPause.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
            showQuickToast("Jeu en pause")
        }
        saveCurrentMatch()
    }

    override fun onPause() {
        super.onPause()
        if (gameStarted && !gamePaused && !matchEnded) {
            gamePaused = true
            stopRandomButtonSystem()
        }
        saveCurrentMatch()
    }

    override fun onResume() {
        super.onResume()
        if (gameStarted && !matchEnded && gamePaused) {
            btnPause.text = "CONTINUER"
            btnPause.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRandomButtonSystem()
        currentToast?.cancel()
    }

    private fun startRandomButtonSystem() {
        stopRandomButtonSystem()
        if (matchEnded) return
        if (!gameStarted || gamePaused) return

        randomButtonRunnable = object : Runnable {
            override fun run() {
                if (!matchEnded && gameStarted && !gamePaused) {
                    activateRandomButton()
                    handler.postDelayed(this, RANDOM_INTERVAL)
                }
            }
        }
        handler.postDelayed(randomButtonRunnable!!, 100)
    }

    private fun stopRandomButtonSystem() {
        randomButtonRunnable?.let { handler.removeCallbacks(it) }
        randomButtonRunnable = null
    }

    private fun activateRandomButton() {
        activeButton = Random.nextInt(1, 3)

        resetButtonColors()

        if (activeButton == 1) {
            btnPlayer1Point.backgroundTintList = ColorStateList.valueOf(Color.GREEN)
            showQuickToast("Cliquez sur $player1Name maintenant!")
        } else {
            btnPlayer2Point.backgroundTintList = ColorStateList.valueOf(Color.GREEN)
            showQuickToast("Cliquez sur $player2Name maintenant!")
        }

        handler.postDelayed({
            if (activeButton != 0 && !gamePaused && !matchEnded) {
                val missedPlayer = if (activeButton == 1) player1Name else player2Name
                showQuickToast("$missedPlayer a raté le bouton! Pas de point")
                resetButtonColors()
                activeButton = 0
            }
        }, RANDOM_INTERVAL)
    }

    private fun resetButtonColors() {
        btnPlayer1Point.backgroundTintList = null
        btnPlayer2Point.backgroundTintList = null
    }

    private fun addPointToPlayer1() {
        if (matchEnded) return

        if (isTieBreak) {
            player1Points++
            checkTieBreakWin()
        } else if (isDeuce) {
            when {
                advantage == 2 -> {
                    advantage = 0
                    isDeuce = true
                }
                advantage == 1 -> {
                    player1Games++
                    resetPoints()
                    checkGameWin()
                }
                else -> advantage = 1
            }
        } else {
            player1Points++
            checkPointWin()
        }
        updateScoreDisplay()
        saveCurrentMatch()
    }

    private fun addPointToPlayer2() {
        if (matchEnded) return

        if (isTieBreak) {
            player2Points++
            checkTieBreakWin()
        } else if (isDeuce) {
            when {
                advantage == 1 -> {
                    advantage = 0
                    isDeuce = true
                }
                advantage == 2 -> {
                    player2Games++
                    resetPoints()
                    checkGameWin()
                }
                else -> advantage = 2
            }
        } else {
            player2Points++
            checkPointWin()
        }
        updateScoreDisplay()
        saveCurrentMatch()
    }

    private fun checkTieBreakWin() {
        if (player1Points >= 7 && player1Points - player2Points >= 2) {
            player1Sets++
            Toast.makeText(this, "🏆 $player1Name gagne le tie-break!", Toast.LENGTH_LONG).show()
            resetPoints()
            isTieBreak = false
            resetGames()
            checkSetWin()
        } else if (player2Points >= 7 && player2Points - player1Points >= 2) {
            player2Sets++
            Toast.makeText(this, "🏆 $player2Name gagne le tie-break!", Toast.LENGTH_LONG).show()
            resetPoints()
            isTieBreak = false
            resetGames()
            checkSetWin()
        }
    }

    private fun checkPointWin() {
        if (player1Points >= 3 && player2Points >= 3) {
            isDeuce = true
            advantage = when {
                player1Points == player2Points -> 0
                player1Points > player2Points -> 1
                else -> 2
            }
            return
        }

        when {
            player1Points >= 4 -> {
                player1Games++
                resetPoints()
                checkGameWin()
            }
            player2Points >= 4 -> {
                player2Games++
                resetPoints()
                checkGameWin()
            }
        }
    }

    private fun checkGameWin() {
        when {
            player1Games == 6 && player2Games == 6 -> {
                isTieBreak = true
                Toast.makeText(this, "🎾 TIE-BREAK!", Toast.LENGTH_LONG).show()
                updateButtonText()
            }
            player1Games >= 6 && player1Games - player2Games >= 2 -> {
                player1Sets++
                resetGames()
                checkSetWin()
            }
            player2Games >= 6 && player2Games - player1Games >= 2 -> {
                player2Sets++
                resetGames()
                checkSetWin()
            }
            player1Games == 7 && player2Games == 6 -> {
                player1Sets++
                resetGames()
                checkSetWin()
            }
            player2Games == 7 && player1Games == 6 -> {
                player2Sets++
                resetGames()
                checkSetWin()
            }
        }
    }

    private fun checkSetWin() {
        when {
            player1Sets >= 2 -> {
                showWinner(player1Name)
                saveMatchToHistory(player1Name)
            }
            player2Sets >= 2 -> {
                showWinner(player2Name)
                saveMatchToHistory(player2Name)
            }
        }
    }

    private fun showWinner(winnerName: String) {
        matchEnded = true
        gameStarted = false
        gamePaused = false
        stopRandomButtonSystem()
        activeButton = 0
        resetButtonColors()
        topPlayerContainer.visibility = View.GONE
        bottomPlayerContainer.visibility = View.GONE
        btnPlayer1Point.visibility = View.GONE
        btnPlayer2Point.visibility = View.GONE
        btnPause.visibility = View.GONE
        tvWinnerMessage.text = "$winnerName Gagne!"
        findViewById<View>(R.id.winnerContainer).visibility = View.VISIBLE
        saveCurrentMatch()
    }

    private fun showResetDialog() {
        val wasRunning = gameStarted && !gamePaused && !matchEnded
        if (wasRunning) {
            gamePaused = true
            stopRandomButtonSystem()
            resetButtonColors()
            activeButton = 0
        }

        AlertDialog.Builder(this)
            .setTitle("Réinitialiser")
            .setMessage("Voulez-vous réinitialiser le match?")
            .setPositiveButton("Oui") { _, _ -> resetMatch() }
            .setNegativeButton("Non") { _, _ ->
                if (wasRunning) {
                    gamePaused = false
                    startRandomButtonSystem()
                }
            }
            .setOnCancelListener {
                if (wasRunning) {
                    gamePaused = false
                    startRandomButtonSystem()
                }
            }
            .show()
    }

    private fun resetMatch() {
        player1Points = 0
        player2Points = 0
        player1Games = 0
        player2Games = 0
        player1Sets = 0
        player2Sets = 0
        isDeuce = false
        advantage = 0
        matchEnded = false
        isTieBreak = false
        activeButton = 0
        gamePaused = true
        gameStarted = true

        topPlayerContainer.visibility = View.VISIBLE
        bottomPlayerContainer.visibility = View.VISIBLE
        btnPlayer1Point.visibility = View.VISIBLE
        btnPlayer2Point.visibility = View.VISIBLE
        btnPause.visibility = View.VISIBLE
        btnPause.text = "CONTINUER"
        btnPause.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
        findViewById<View>(R.id.winnerContainer).visibility = View.GONE

        resetButtonColors()
        updateScoreDisplay()
        saveCurrentMatch()
    }

    private fun resetPoints() {
        player1Points = 0
        player2Points = 0
        isDeuce = false
        advantage = 0
    }

    private fun resetGames() {
        player1Games = 0
        player2Games = 0
    }

    private fun updateScoreDisplay() {
        tvPlayer1Name.text = player1Name
        tvPlayer2Name.text = player2Name

        if (isTieBreak) {
            tvPlayer1Score.text = player1Points.toString()
            tvPlayer2Score.text = player2Points.toString()
        } else {
            tvPlayer1Score.text = getPointsDisplay(player1Points, 1)
            tvPlayer2Score.text = getPointsDisplay(player2Points, 2)
        }

        tvPlayer1Games.text = player1Games.toString()
        tvPlayer2Games.text = player2Games.toString()
        tvPlayer1Sets.text = player1Sets.toString()
        tvPlayer2Sets.text = player2Sets.toString()
        updateButtonText()
    }

    private fun getPointsDisplay(points: Int, player: Int): String {
        if (isDeuce) {
            return when {
                advantage == 0 -> "40"
                advantage == player -> "AD"
                else -> "40"
            }
        }

        return when (points) {
            0 -> "0"
            1 -> "15"
            2 -> "30"
            3 -> "40"
            else -> "G"
        }
    }

    private fun updateButtonText() {
        if (isTieBreak) {
            btnPlayer1Point.text = "${player1Name.uppercase()}\nTIE-BREAK"
            btnPlayer2Point.text = "${player2Name.uppercase()}\nTIE-BREAK"
        } else {
            btnPlayer1Point.text = "${player1Name.uppercase()}\nSCORES"
            btnPlayer2Point.text = "${player2Name.uppercase()}\nSCORES"
        }
    }

    private fun saveCurrentMatch() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val matchData = MatchData(
            player1Name, player2Name,
            player1Points, player2Points,
            player1Games, player2Games,
            player1Sets, player2Sets,
            isDeuce, advantage, matchEnded, isTieBreak, gameStarted, gamePaused
        )
        val json = Gson().toJson(matchData)
        prefs.edit().putString(CURRENT_MATCH, json).apply()
    }

    private fun loadCurrentMatch() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(CURRENT_MATCH, null) ?: return

        try {
            val matchData = Gson().fromJson(json, MatchData::class.java)
            player1Name = matchData.player1Name
            player2Name = matchData.player2Name
            player1Points = matchData.player1Points
            player2Points = matchData.player2Points
            player1Games = matchData.player1Games
            player2Games = matchData.player2Games
            player1Sets = matchData.player1Sets
            player2Sets = matchData.player2Sets
            isDeuce = matchData.isDeuce
            advantage = matchData.advantage
            matchEnded = matchData.matchEnded
            isTieBreak = matchData.isTieBreak
            gameStarted = matchData.gameStarted
            gamePaused = matchData.gamePaused

            updateScoreDisplay()

            if (matchEnded) {
                val winner = if (player1Sets >= 2) player1Name else player2Name
                tvWinnerMessage.text = "$winner Gagne!"
                findViewById<View>(R.id.winnerContainer).visibility = View.VISIBLE
                topPlayerContainer.visibility = View.GONE
                bottomPlayerContainer.visibility = View.GONE
                btnPlayer1Point.visibility = View.GONE
                btnPlayer2Point.visibility = View.GONE
                btnPause.visibility = View.GONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveMatchToHistory(winner: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val historyJson = prefs.getString(MATCH_HISTORY, "[]")
        val type = object : TypeToken<MutableList<MatchHistory>>() {}.type
        val history: MutableList<MatchHistory> = Gson().fromJson(historyJson, type)

        val matchHistory = MatchHistory(
            date = System.currentTimeMillis(),
            player1Name = player1Name,
            player2Name = player2Name,
            player1Sets = player1Sets,
            player2Sets = player2Sets,
            winner = winner
        )

        history.add(0, matchHistory)
        if (history.size > 50) history.removeAt(history.size - 1)

        val newJson = Gson().toJson(history)
        prefs.edit().putString(MATCH_HISTORY, newJson).apply()
    }

    private fun openHistory() {
        val intent = Intent(this, HistoryActivity::class.java)
        startActivity(intent)
    }

    private fun editPlayerName(playerNumber: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(if (playerNumber == 1) "Nom du Joueur 1" else "Nom du Joueur 2")

        val input = EditText(this)
        input.hint = "Lettres uniquement"
        input.setText(if (playerNumber == 1) player1Name else player2Name)
        builder.setView(input)

        builder.setPositiveButton("Confirmer") { _, _ ->
            val name = input.text.toString().trim()
            if (isValidName(name)) {
                if (playerNumber == 1) {
                    player1Name = name
                    tvPlayer1Name.text = name
                } else {
                    player2Name = name
                    tvPlayer2Name.text = name
                }
                updateButtonText()
                saveCurrentMatch()
            } else {
                Toast.makeText(this, "Nom invalide! Utilisez uniquement des lettres", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Annuler", null)
        builder.show()
    }

    private fun isValidName(name: String): Boolean {
        if (name.isEmpty()) return false
        return name.all { it.isLetter() || it.isWhitespace() }
    }

    data class MatchData(
        val player1Name: String,
        val player2Name: String,
        val player1Points: Int,
        val player2Points: Int,
        val player1Games: Int,
        val player2Games: Int,
        val player1Sets: Int,
        val player2Sets: Int,
        val isDeuce: Boolean,
        val advantage: Int,
        val matchEnded: Boolean,
        val isTieBreak: Boolean = false,
        val gameStarted: Boolean = false,
        val gamePaused: Boolean = false
    )

    data class MatchHistory(
        val date: Long,
        val player1Name: String,
        val player2Name: String,
        val player1Sets: Int,
        val player2Sets: Int,
        val winner: String
    )
}