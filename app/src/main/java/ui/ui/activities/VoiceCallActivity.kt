package ui.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.whatsapp.R
import com.nexmo.client.NexmoClient
import com.nexmo.client.voice.Call
import com.nexmo.client.voice.CallEvent
import com.nexmo.client.voice.VoiceName
import com.nexmo.client.voice.ncco.Ncco
import com.nexmo.client.voice.ncco.TalkAction
import java.io.File


class VoiceCallActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_call)



        val client: NexmoClient = NexmoClient.builder()
            .applicationId("4d6b8a46-09e8-4186-9a12-05a96bdece92")
            .privateKeyPath("./private.key")
            .build()

        val ncco = Ncco(
            TalkAction
                .builder("This is a text-to-speech test message.")
                .voiceName(VoiceName.JOEY)
                .build()
        )

        val TO_NUMBER = "201156188928"
        val FROM_NUMBER = "201140315364"

        val result: CallEvent = client
            .voiceClient
            .createCall(Call(TO_NUMBER, FROM_NUMBER, ncco))

        println(result.conversationUuid)
    }
}