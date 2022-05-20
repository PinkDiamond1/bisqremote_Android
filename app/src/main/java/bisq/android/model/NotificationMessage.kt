package bisq.android.model

import android.util.Log
import bisq.android.database.BisqNotification
import bisq.android.util.CryptoUtil
import bisq.android.util.DateUtil
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import java.text.ParseException
import java.util.*

class NotificationMessage(private var notification: String?) {

    private lateinit var magicValue: String
    private lateinit var initializationVector: String
    private lateinit var encryptedPayload: String
    private lateinit var decryptedPayload: String

    lateinit var bisqNotification: BisqNotification

    companion object {
        private const val TAG = "NotificationMessage"
        const val BISQ_MESSAGE_ANDROID_MAGIC = "BisqMessageAndroid"
    }

    init {
        parseNotification()
        decryptNotificationMessage()
        deserializeNotificationMessage()
    }

    private fun parseNotification() {
        try {
            val array = notification?.split("\\|".toRegex())?.dropLastWhile { it.isEmpty() }
                ?.toTypedArray()
            if (array == null || array.size != 3) {
                throw ParseException("Invalid format", 0)
            }
            magicValue = array[0]
            initializationVector = array[1]
            encryptedPayload = array[2]
            if (magicValue != BISQ_MESSAGE_ANDROID_MAGIC) {
                throw ParseException("Invalid magic value", 0)
            }
            if (initializationVector.length != 16) {
                throw ParseException("Invalid initialization vector (must be 16 characters)", 0)
            }
        } catch (e: ParseException) {
            val message = "Failed to parse notification; ${e.message}"
            Log.e(TAG, message)
            throw ParseException(message, e.errorOffset)
        }
    }

    private fun decryptNotificationMessage() {
        try {
            decryptedPayload = CryptoUtil(Device.instance.key!!).decrypt(
                encryptedPayload, initializationVector
            )
        } catch (e: Exception) {
            val message = "Failed to decrypt notification"
            Log.e(TAG, "$message: $encryptedPayload")
            throw Exception(message)
        }
    }

    private fun deserializeNotificationMessage() {
        val gsonBuilder = GsonBuilder()
        gsonBuilder.registerTypeAdapter(Date::class.java, DateUtil())
        val gson = gsonBuilder.create()
        try {
            bisqNotification = gson.fromJson(decryptedPayload, BisqNotification::class.java)
        } catch (e: JsonSyntaxException) {
            val message = "Failed to deserialize notification"
            Log.e(TAG, "$message: $decryptedPayload")
            throw Exception(message)
        }
    }

}
