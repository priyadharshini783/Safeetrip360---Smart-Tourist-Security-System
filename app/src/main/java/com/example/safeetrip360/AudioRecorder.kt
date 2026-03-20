import android.media.MediaRecorder
import android.content.Context
import android.util.Log
import java.io.File

object AudioRecorder {
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false

    fun startRecording(context: Context) {
        if (isRecording) return // Already recording

        try {
            // Create a file in the app's secret cache
            val file = File(context.externalCacheDir, "SOS_Evidence_${System.currentTimeMillis()}.3gp")

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            Log.d("AudioRecorder", "Recording started: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start recording", e)
        }
    }

    fun stopRecording() {
        try {
            if (isRecording) {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaRecorder = null
                isRecording = false
                Log.d("AudioRecorder", "Recording stopped")
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error stopping recording", e)
        }
    }
}