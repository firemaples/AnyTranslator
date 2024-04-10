package tw.firemaples.onscreenocr.screenshot

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import tw.firemaples.onscreenocr.R
import tw.firemaples.onscreenocr.log.FirebaseEvent
import tw.firemaples.onscreenocr.pages.setting.SettingManager
import tw.firemaples.onscreenocr.pref.AppPref
import tw.firemaples.onscreenocr.utils.BitmapCache
import tw.firemaples.onscreenocr.utils.Constants
import tw.firemaples.onscreenocr.utils.Logger
import tw.firemaples.onscreenocr.utils.UIUtils
import tw.firemaples.onscreenocr.utils.Utils
import tw.firemaples.onscreenocr.utils.setReusable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object ScreenExtractor {
    private val context: Context by lazy { Utils.context }
    private val logger: Logger by lazy { Logger(ScreenExtractor::class) }

    private var keepMediaProjection: Boolean = true

    private var mediaProjectionIntent: Intent? = null

    val isGranted: Boolean
        get() = mediaProjectionIntent != null

    private val handler: Handler by lazy {
        val thread = HandlerThread("Thread-${ScreenExtractor::class.simpleName}")
        thread.start()
        Handler(thread.looper)
    }

    private var projection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var lastScreenSize: Point = Point()

    private val screenDensityDpi: Int
        get() = UIUtils.displayMetrics.densityDpi

    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            logger.debug("MPCallback, onStop()")
        }

        override fun onCapturedContentResize(width: Int, height: Int) {
            super.onCapturedContentResize(width, height)
            logger.debug("MPCallback, onCapturedContentResize(): ${width}x$height")
        }

        override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
            super.onCapturedContentVisibilityChanged(isVisible)
            logger.debug("MPCallback, onCapturedContentVisibilityChanged(): $isVisible")
        }
    }

    fun onMediaProjectionGranted(intent: Intent, keepMediaProjection: Boolean) {
        releaseAllResources()
        mediaProjectionIntent = intent.clone() as Intent
        this.keepMediaProjection = keepMediaProjection
    }

    fun release() {
        releaseAllResources()
        virtualDisplay?.release()
        mediaProjectionIntent = null
    }

    @Throws(
        IllegalStateException::class,
        IllegalArgumentException::class,
        TimeoutCancellationException::class
    )
    suspend fun extractBitmapFromScreen(parentRect: Rect, cropRect: Rect): Bitmap {
        logger.debug("extractBitmapFromScreen(), parentRect: $parentRect, cropRect: $cropRect")

        val fullBitmap = doCaptureScreen()

        return try {
            cropBitmap(fullBitmap, parentRect, cropRect)
        } finally {
            fullBitmap.setReusable()
        }
    }

    @SuppressLint("WrongConstant")
    @Throws(
        IllegalStateException::class,
        IllegalArgumentException::class,
        TimeoutCancellationException::class,
    )
    private suspend fun doCaptureScreen(): Bitmap {
        var bitmap: Bitmap
        withContext(Dispatchers.Default) {
            if (!keepMediaProjection) {
                releaseAllResources()
            }

            val mpIntent = mediaProjectionIntent
            if (mpIntent == null) {
                logger.warn("The mediaProjectionIntent is null")
                throw IllegalStateException("The media projection intent is not initialized")
            }

            val mpManager =
                context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

            try {
                if (projection == null) {
                    logger.debug("Create MediaProjection")
                    projection =
                        mpManager.getMediaProjection(Activity.RESULT_OK, mpIntent.clone() as Intent)
                            .apply {
                                registerCallback(mediaProjectionCallback, handler)
                            }
                }

                val projection = projection
                if (projection == null) {
                    logger.warn("Retrieve projection failed, projection is null")
                    throw IllegalStateException("Retrieving media projection failed")
                }

                val screenSize = UIUtils.realSize
                val screenConfigurationChanged = lastScreenSize != screenSize
                lastScreenSize = screenSize
                val width = screenSize.x
                val height = screenSize.y

                var imageReader: ImageReader? = this@ScreenExtractor.imageReader
                if (imageReader == null || screenConfigurationChanged) {
                    logger.debug("Create ImageReader")
                    imageReader =
                        ImageReader.newInstance(width, height, AppPref.imageReaderFormat, 2)
                    imageReaderReady = false
                    this@ScreenExtractor.imageReader = imageReader
                }

                var virtualDisplay: VirtualDisplay? = virtualDisplay
                if (virtualDisplay == null) {
                    logger.debug("Create VirtualDisplay")
                    virtualDisplay = projection.createVirtualDisplay(
                        "screen-mirror",
                        width, height, screenDensityDpi,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                        imageReader.surface, null, null
                    )
                    this@ScreenExtractor.virtualDisplay = virtualDisplay
                } else if (screenConfigurationChanged) {
                    // Update configuration changes
                    logger.debug("screenConfigurationChanged: $lastScreenSize")
                    virtualDisplay.resize(width, height, screenDensityDpi)
                    virtualDisplay.surface = imageReader.surface
                }
                val captured = withTimeout(SettingManager.timeoutForCapturingScreen) {
                    logger.debug("awaitForBitmap")
                    imageReader.awaitForBitmap(screenSize)
                }

                if (captured == null) {
                    logger.warn("The captured image is null")
                    if (!keepMediaProjection) {
                        releaseAllResources()
                    }
                    throw IllegalStateException("No image data found")
                }
                bitmap = captured

                logger.debug("Bitmap size: ${bitmap.width}x${bitmap.height}, screen size: ${width}x$height")
            } catch (e: Throwable) {
                logger.warn(t = e)

                val message = e.message ?: e.localizedMessage
                if (message != null) {
                    val match = Constants.regexForImageReaderFormatError.find(message)
                    val formatValue = match?.groupValues?.get(1)?.toIntOrNull()
                    if (formatValue != null) {
                        val msg =
                            "Format not matched error found, change the image reader format from ${AppPref.imageReaderFormat} to $formatValue"
                        logger.warn(msg)
                        AppPref.imageReaderFormat = formatValue
                        FirebaseEvent.logException(Exception(msg, e))

                        throw Exception(context.getString(R.string.msg_image_reader_format_unmatched))
                    }
                }

                throw e
            } finally {
                if (!keepMediaProjection) {
                    releaseAllResources()
                }
            }
        }

        return bitmap
    }

    private fun releaseAllResources() {
        logger.debug("releaseAllResources()")
        try {
            imageReader?.setOnImageAvailableListener(null, null)
        } catch (e: Exception) {
            // ignore exceptions
        }
        try {
            imageReader?.close()
        } catch (e: Exception) {
            // ignore exceptions
        }
        imageReader = null
        try {
            virtualDisplay?.release()
        } catch (e: Exception) {
            // ignore exceptions
        }
        virtualDisplay = null
        try {
            projection?.stop()
        } catch (e: Exception) {
            // ignore exceptions
        }
        projection = null
    }

    @Throws(IllegalArgumentException::class)
    private fun cropBitmap(bitmap: Bitmap, parentRect: Rect, cropRect: Rect): Bitmap {
        logger.debug(
            "cropBitmap(), " +
                    "bitmap: ${bitmap.width}x${bitmap.height}, " +
                    "parentRect: $parentRect, cropRect: $cropRect"
        )

        val top = parentRect.top + cropRect.top
        val bottom = parentRect.top + cropRect.bottom

        val left = parentRect.left + cropRect.left
        val right = parentRect.left + cropRect.right

        val rect = Rect(left, top, right, bottom)

        val width = rect.width().coerceAtMost(bitmap.width - rect.left)
        val height = rect.height().coerceAtMost(bitmap.height - rect.top)

        @Suppress("ComplexCondition")
        if (width <= 0 || height <= 0 || rect.left < 0 || rect.top < 0) {
            val msg = "Crop attributes are illegal, " +
                    "bitmap size: ${bitmap.width}x${bitmap.height}, " +
                    "parentRect: $parentRect, cropRect: $cropRect, rect: $rect, " +
                    "width: $width, height: $height"
            logger.warn(msg)
            FirebaseEvent.logException(IllegalStateException(msg))
        }

        val cropped = Bitmap.createBitmap(bitmap, rect.left, rect.top, width, height)
        logger.debug("cropped bitmap: ${cropped.width}x${cropped.height}")

        return cropped
    }

    private var imageReaderReady = false
    private suspend fun ImageReader.awaitForBitmap(screenSize: Point): Bitmap? =
        suspendCancellableCoroutine {
            if (imageReaderReady) {
                logger.debug("reader is ready, acquireLatestImage directly")
                val image = acquireLatestImage()
                val bitmap = image.decodeBitmap(screenSize)
                image.close()
                logger.debug("Latest bitmap: $bitmap")
                it.resume(bitmap)
                return@suspendCancellableCoroutine
            }

            logger.debug("suspendCancellableCoroutine: Started")
            var counter = 0
            val resumed = AtomicBoolean(false)
            setOnImageAvailableListener({ reader ->
                logger.info("onImageAvailable()")
                if (resumed.get()) {
                    reader.setOnImageAvailableListener(null, null)
                    return@setOnImageAvailableListener
                }
                var image: Image? = null
                try {
                    image = reader.acquireLatestImage()
                    val bitmap = image.decodeBitmap(screenSize)
                    if (!bitmap.isWholeBlack()) {
                        reader.setOnImageAvailableListener(null, null)
                        if (resumed.getAndSet(true))
                            return@setOnImageAvailableListener
                        if (keepMediaProjection) {
                            imageReaderReady = true
                        }
                        it.resume(bitmap)
                    } else {
                        logger.info("Image is whole black, increase counter: $counter")
                        bitmap.setReusable()
                        if (counter >= Constants.EXTRACT_SCREEN_MAX_RETRY) {
                            reader.setOnImageAvailableListener(null, null)
                            if (resumed.getAndSet(true))
                                return@setOnImageAvailableListener
                            it.resume(null)
                        } else {
                            counter++
                        }
                    }
                } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                    logger.warn("Error when acquire image", t = e)
                    reader.setOnImageAvailableListener(null, null)
                    if (resumed.getAndSet(true))
                        return@setOnImageAvailableListener
                    it.resumeWithException(e)
                } finally {
                    try {
                        image?.close()
                    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                        // Ignore closing failed
                        logger.warn("Error while closing image", e)
                        FirebaseEvent.logException(e)
                    }
                }
            }, handler)
            it.invokeOnCancellation {
                logger.warn("awaitForBitmap cancelled")
                resumed.set(true)
                setOnImageAvailableListener(null, null)
            }
        }

    @Throws(IllegalArgumentException::class)
    private fun Image.decodeBitmap(screenSize: Point): Bitmap =
        with(planes[0]) {
            val screenWidth = screenSize.x
            val screenHeight = screenSize.y
//            val deviceWidth = screenWidth
//            val rowPadding = rowStride - pixelStride * deviceWidth
            val bufferedBitmap = BitmapCache.getReusableBitmapOrCreate(
                width = rowStride / pixelStride,
//                width = screenWidth + rowPadding / pixelStride,
                height = screenHeight,
                config = Bitmap.Config.ARGB_8888,
                fixedSize = true,
            ).apply {
                // rewind() before using the buffer to fix:
                // RuntimeException: Buffer not large enough for pixels
                buffer.rewind()
                copyPixelsFromBuffer(buffer)
            }

            if (bufferedBitmap.width > screenWidth) {
                Bitmap.createBitmap(bufferedBitmap, 0, 0, screenWidth, screenHeight).also {
                    bufferedBitmap.setReusable()
                }
            } else {
                if (bufferedBitmap.width < screenWidth) {
                    val msg =
                        "BufferedBitmap is less than screenWidth, " +
                                "buffer size: ${bufferedBitmap.width}x${bufferedBitmap.height}, " +
                                "screenSize: ${screenWidth}x$screenHeight "
                    logger.warn(msg)
                    FirebaseEvent.logException(IllegalStateException(msg))
                }
                bufferedBitmap
            }
        }

    private fun Bitmap.isWholeBlack(): Boolean {
        for (x in 0 until width) {
            for (y in 0 until height) {
                val color = getPixel(x, y)
                val red = Color.red(color)
                val green = Color.green(color)
                val blue = Color.blue(color)
                val alpha = Color.alpha(color)
                if (red or green or blue or alpha != 0)
                    return false
            }
        }

        return true
    }
}
