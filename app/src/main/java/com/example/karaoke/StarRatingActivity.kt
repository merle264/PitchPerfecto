package com.example.karaoke

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class StarRatingActivity : AppCompatActivity() {

    private lateinit var starLayout: LinearLayout
    private lateinit var starStatus: TextView
    private lateinit var returnButton: Button
    private var similarity: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_star_rating)

        // Retrieve the similarity value passed from the MainActivity
        similarity = intent.getDoubleExtra("similarity", 0.0)

        starLayout = findViewById(R.id.starLayout)
        starStatus = findViewById(R.id.starRatingStatus)
        returnButton = findViewById(R.id.btnReturnToMain)

        // Display similarity percentage
        starStatus.text = "Score Similarity: %.2f%%".format(similarity)

        // Fill the stars based on the similarity value
        fillStars(similarity)

        // Button to return to the main screen
        returnButton.setOnClickListener {
            finish()  // Close this activity and return to the previous screen
        }
    }

    private fun fillStars(similarity: Double) {
        val stars = arrayOf(
            findViewById<ImageView>(R.id.star1),
            findViewById<ImageView>(R.id.star2),
            findViewById<ImageView>(R.id.star3),
            findViewById<ImageView>(R.id.star4),
            findViewById<ImageView>(R.id.star5)
        )

        // Calculate the number of stars to fill based on similarity (0 to 100%)
        val filledStars = (similarity / 20).toInt()  // 20% per star

        for (i in 0 until stars.size) {
            if (i < filledStars) {
                stars[i].setImageResource(R.drawable.star_filled)  // Filled star
            } else {
                stars[i].setImageResource(R.drawable.star_empty)  // Empty star
            }
        }
    }
}
