package com.example.karaoke

import android.util.Log
import com.jlibrosa.audio.JLibrosa
import com.jlibrosa.audio.wavFile.WavFile
import kotlin.math.sqrt
import java.io.File

class PitchComparison {

    private val jlibrosa: JLibrosa = JLibrosa()
    private var sampleRate: Int = 0
    private var audioDuration: Long = 0

    // Function to initialize audio parameters (sample rate and duration)
    fun initializeWithFile(file: File) {
        try {
            val wavFile = WavFile.openWavFile(file)
            sampleRate = wavFile.sampleRate.toInt() // Extract sample rate from the WAV file
            audioDuration = wavFile.duration  // Duration in seconds
            println("Initialized audio with sampleRate=$sampleRate and duration=$audioDuration")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error: Failed to initialize audio file.")
        }
    }

    // Function to generate MFCC features
    fun MFCCFeatures(filePath: File): Array<FloatArray> {
        return try {
            println("Loading audio from: ${filePath.absolutePath}")

            val audioFeaturesValues = jlibrosa.loadAndRead(filePath.absolutePath, sampleRate, audioDuration.toInt())
            if (audioFeaturesValues.isEmpty()) {
                println("Error: Loaded audio features are empty!")
                return arrayOf()
            }
            println("Audio features loaded, size: ${audioFeaturesValues.size}")

            val mfccFeatures = jlibrosa.generateMFCCFeatures(audioFeaturesValues, sampleRate, 40)

            // Log MFCC array size
            Log.d("MFCC", "MFCC Array Length: ${mfccFeatures.size}")

            if (mfccFeatures.isEmpty() || mfccFeatures[0].isEmpty()) {
                Log.e("MFCC", "MFCC Array is empty. Check the feature extraction process.")
                println("Error: MFCC features are empty!")
                return arrayOf()
            }

            println("MFCC Features generated: ${mfccFeatures.size} frames, ${mfccFeatures[0].size} coefficients")
            mfccFeatures
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error: Exception in MFCC feature extraction.")
            arrayOf()  // Return an empty array if error occurs
        }
    }


    // Function to normalize MFCC values
    fun normalizeMFCC(mfcc: Array<FloatArray>): Array<FloatArray> {
        for (i in mfcc.indices) {
            val mean = mfcc[i].average().toFloat()
            val stddev = sqrt(mfcc[i].map { (it - mean) * (it - mean) }.average().toDouble()).toFloat()
            mfcc[i] = mfcc[i].map { (it - mean) / (stddev + 1e-8f) }.toFloatArray()
        }
        return mfcc
    }

    // Function to calculate cosine similarity between two arrays
    fun cosineSimilarity(array1: Array<FloatArray>, array2: Array<FloatArray>): Double {
        val flattenedArray1 = array1.flatMap { it.asIterable() }.toFloatArray()
        val flattenedArray2 = array2.flatMap { it.asIterable() }.toFloatArray()

        val dotProduct = flattenedArray1.zip(flattenedArray2) { a, b -> a * b }.sum()

        val magnitude1 = sqrt(flattenedArray1.sumByDouble { (it * it).toDouble() })
        val magnitude2 = sqrt(flattenedArray2.sumByDouble { (it * it).toDouble() })

        // Prevent division by zero
        if (magnitude1 == 0.0 || magnitude2 == 0.0) return 0.0

        return dotProduct / (magnitude1 * magnitude2)
    }

    // Function to compare two audio files using MFCC features
    fun compareMFCC(filePathOg: File, filePathSelf: File): Double {
        // Verify file existence
        if (!filePathOg.exists()) {
            println("Error: The original file does not exist at path: ${filePathOg.absolutePath}")
            return -1.0  // Or handle as appropriate
        }
        if (!filePathSelf.exists()) {
            println("Error: The self file does not exist at path: ${filePathSelf.absolutePath}")
            return -1.0  // Or handle as appropriate
        }

        println("Comparing files: ${filePathOg.name} vs ${filePathSelf.name}")

        initializeWithFile(filePathOg)
        initializeWithFile(filePathSelf)

        // Check if audio files can be loaded properly
        checkWavFile(filePathOg)
        checkWavFile(filePathSelf)

        val ogAudio = MFCCFeatures(filePathOg)
        val selfAudio = MFCCFeatures(filePathSelf)

        if (ogAudio.isEmpty() || selfAudio.isEmpty()) {
            println("Error: One or both MFCC arrays are empty!")
            return 0.0
        }

        // Normalize MFCC features
        val normalizedOgAudio = normalizeMFCC(ogAudio)
        val normalizedSelfAudio = normalizeMFCC(selfAudio)

        // Equalize the lengths of the two MFCC arrays
        val (equalizedOgAudio, equalizedSelfAudio) = equalizeLength(normalizedOgAudio, normalizedSelfAudio)

        // Calculate cosine similarity
        val cosineSimilarityValue = cosineSimilarity(equalizedOgAudio, equalizedSelfAudio)

        // Convert cosine similarity to a percentage
        val similarityPercentage = 0.5 * (cosineSimilarityValue + 1) * 100

        // Return similarity percentage
        return similarityPercentage
    }

    // Helper function to check if WAV file can be loaded
    fun checkWavFile(file: File) {
        println("Checking file: ${file.absolutePath}")
        if (!file.exists()) {
            println("Error: The file does not exist at path: ${file.absolutePath}")
            return
        }

        try {
            val wavFile = WavFile.openWavFile(file)
            val sampleRate = wavFile.sampleRate
            val audioDuration = wavFile.duration
            println("WAV File: sampleRate=$sampleRate, duration=$audioDuration")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error: Failed to open WAV file.")
        }
    }

    // Helper function to equalize the length of two arrays (used for MFCC arrays)
    fun equalizeLength(
        array1: Array<FloatArray>,
        array2: Array<FloatArray>
    ): Pair<Array<FloatArray>, Array<FloatArray>> {
        println("Array 1 size: ${array1.size}")
        println("Array 2 size: ${array2.size}")

        // Check for consistency in inner array sizes
        checkInnerArrayConsistency(array1, "array1")
        checkInnerArrayConsistency(array2, "array2")

        // Get the maximum length of both arrays
        val maxLength = maxOf(array1.size, array2.size)

        // Pad arrays to the maximum length
        val paddedArray1 = padArray(array1.copyOf(), maxLength)  // Copy before padding
        val paddedArray2 = padArray(array2.copyOf(), maxLength)  // Copy before padding

        return Pair(paddedArray1, paddedArray2)
    }

        // Function to ensure all inner arrays have the same size
    fun checkInnerArrayConsistency(array: Array<FloatArray>, arrayName: String) {
        if (array.isEmpty()) {
            throw IllegalArgumentException("$arrayName is empty")
        }

        val innerSize = array[0].size
        println("$arrayName inner size: $innerSize")

        for (i in array.indices) {
            if (array[i].size != innerSize) {
                println("Inconsistent inner array size detected in $arrayName at index $i: expected $innerSize, found ${array[i].size}")
                throw IllegalArgumentException("$arrayName has inconsistent inner array sizes")
            }
        }
    }

    // Function to pad arrays with zeros up to the target length
    fun padArray(array: Array<FloatArray>, targetLength: Int): Array<FloatArray> {
        // Get the size of the inner array (assuming all rows are the same size)
        val rowLength = array[0].size

        println("Padding array to target length $targetLength with row length $rowLength")

        // Create a new array of the target length
        return Array(targetLength) { rowIndex ->
            if (rowIndex < array.size) {
                array[rowIndex]
            } else {
                FloatArray(rowLength) { 0f }  // Pad with zeros (same row length)
            }
        }
    }
}