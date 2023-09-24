package tw.firemaples.onscreenocr.translator.mymemory

import tw.firemaples.onscreenocr.utils.Logger
import tw.firemaples.onscreenocr.utils.Utils

object MyMemoryTranslatorAPI {
    private val logger: Logger by lazy { Logger(this::class) }
    private val apiService: MyMemoryAPIService by lazy {
        Utils.retrofit.create(MyMemoryAPIService::class.java)
    }

    suspend fun translate(text: String, from: String, to: String, email: String?): Result<String> {
        logger.debug("Start translate, text: $text, from: $from, to: $to")
        val result = apiService.translate(
            text = text,
            langPair = "$from|$to",
            email = email,
        )
        logger.debug("Translate result: $result")

        if (!result.isSuccessful) {
            return Result.failure(
                IllegalStateException(
                    "API failed(${result.code()}): ${result.errorBody()?.toString()}"
                )
            )
        }

        return result.body()?.let { response ->
            if (response.isSuccess()) {
                val translatedText = response.responseData.translatedText
                logger.debug("Got translation result: $translatedText")
                Result.success(translatedText)
            } else {
                Result.failure(
                    Exception(
                        "Got response failed status(${response.responseStatus}), quota finished: ${response.quotaFinished}, detail: ${response.responseDetails}"
                    )
                )
            }
        } ?: Result.failure(IllegalStateException("Got null body"))
    }
}
