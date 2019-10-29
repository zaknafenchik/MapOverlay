package megakit.miranda.myapplication

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.maps.model.LatLng


private const val minZoomScale = 11f
private const val maxZoomScale = 16f

private const val minScale = 0.5f
private const val maxScale = 1f
private const val carAnimationDuration = 2000L

class MapOverlayView : View {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
            context,
            attrs,
            defStyleAttr
    )

    private val pathPoints = arrayListOf<PointF>()
    private val rectF = RectF()

    private val paint = Paint()
    private val path = Path()

    private var carBitmap: Bitmap? = null
    private var scaledCarBitmap: Bitmap? = null
    private var carPointF: PointF? = null

    private var lastWidth = 0
    private var lastHeight = 0

    private val pointNW = PointF()
    private val pointNE = PointF()
    private val pointSW = PointF()
    private val pointSE = PointF()


    private var leftLine = Line(pointNW, pointSW)
    private var rightLine = Line(pointNE, pointSE)
    private var topLine = Line(pointNW, pointNE)
    private var bottomLine = Line(pointSW, pointSE)

    private val line = Line()
    private var zoom = 0F
    private var carBearing = 0F
    private var carLatLng: LatLng? = null
    private var isCarAnimation = false
    private var startCarAnimPointF: PointF? = null
    private var endCarAnimPointF: PointF? = null
    private var startAnim = 0L
    private val interpolator = LinearInterpolator()
    private var startAnimBearing = 0F

    init {
        paint.strokeWidth = 8F
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLUE
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (lastWidth != measuredWidth || lastHeight != measuredHeight) {
            lastWidth = measuredWidth
            lastHeight = measuredHeight

            rectF.set(0.0F, 0.0F, measuredWidth.toFloat(), measuredHeight.toFloat())

            pointNE.x = measuredWidth.toFloat()
            pointSW.y = measuredHeight.toFloat()
            pointSE.x = measuredWidth.toFloat()
            pointSE.y = measuredHeight.toFloat()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        path.reset()
        drawRoute(canvas)
        drawCar(canvas)
    }

    private fun drawCar(canvas: Canvas) {
        val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_car, null)


        if (!isCarAnimation) {
            scaledCarBitmap = drawableToBitmap(drawable!!, carBearing)
            carPointF?.let {
                canvas.drawBitmap(
                        scaledCarBitmap!!, it.x - (scaledCarBitmap!!.width / 2),
                        it.y - (scaledCarBitmap!!.height / 2), null
                )
            }
            return
        }

        val deltaX = endCarAnimPointF!!.x - carPointF!!.x
        val deltaY = endCarAnimPointF!!.y - carPointF!!.y


        val elapsed = SystemClock.uptimeMillis() - startAnim
        val t = interpolator.getInterpolation(elapsed.toFloat() / carAnimationDuration)

        if (t < 1.0) {
            val x = (endCarAnimPointF!!.x - startCarAnimPointF!!.x) * t + startCarAnimPointF!!.x - deltaX
            val y = (endCarAnimPointF!!.y - startCarAnimPointF!!.y) * t + startCarAnimPointF!!.y - deltaY

            val deltaBearing = carBearing - startAnimBearing
            val bearing = startAnimBearing + deltaBearing * t
            scaledCarBitmap = drawableToBitmap(drawable!!, bearing)


            path.reset()
            path.moveTo(x, y)
            path.lineTo(pathPoints[0].x, pathPoints[0].y)
            canvas.drawPath(path, paint)
            canvas.drawBitmap(
                    scaledCarBitmap!!, x - (scaledCarBitmap!!.width / 2),
                    y - (scaledCarBitmap!!.height / 2), null)

            invalidate()
        } else {

            isCarAnimation = false
            scaledCarBitmap = drawableToBitmap(drawable!!, carBearing)
            canvas.drawBitmap(
                    scaledCarBitmap!!, carPointF!!.x - (scaledCarBitmap!!.width / 2),
                    carPointF!!.y - (scaledCarBitmap!!.height / 2), null
            )
        }
    }

    private fun drawRoute(canvas: Canvas) {
        var isStarted = false

        for (index in pathPoints.indices) {

            val x = pathPoints[index].x
            val y = pathPoints[index].y
            if (index + 1 < pathPoints.size && !isStarted) {
                if (!rectF.contains(x, y) && !rectF.contains(pathPoints[index + 1].x, pathPoints[index + 1].y)) {
                    line.p1 = pathPoints[index + 1]
                    line.p2 = pathPoints[index]
                    if (getIntersectionPoint(line) == null) {
                        continue
                    } else {
                        drawLine(pathPoints[index], pathPoints[index + 1])
                        break
                    }
                }
            }

            if (isStarted) {
                if (!rectF.contains(x, y) && !rectF.contains(pathPoints[index - 1].x, pathPoints[index - 1].y)) {
                    continue
                }
            }

            if (!isStarted) {
                if (!rectF.contains(x, y) && index + 1 < pathPoints.size) {
                    line.p1 = pathPoints[index + 1]
                    line.p2 = pathPoints[index]
                    val intersectionPoint = getIntersectionPoint(line) ?: continue

                    path.moveTo(intersectionPoint.x, intersectionPoint.y)
                } else {
                    path.moveTo(x, y)
                }

                isStarted = true

            } else {
                if (!rectF.contains(x, y)) {
                    line.p1 = pathPoints[index - 1]
                    line.p2 = pathPoints[index]
                    val intersectionPoint = getIntersectionPoint(line) ?: continue
                    path.lineTo(intersectionPoint.x, intersectionPoint.y)
                } else {
                    path.lineTo(x, y)
                }
            }
        }

        canvas.drawPath(path, paint)
    }

    private fun drawLine(start: PointF, end: PointF) {
        line.p1 = start
        line.p2 = end
        val startIntersection = getIntersectionPoint(line)
        path.moveTo(startIntersection!!.x, startIntersection.y)

        val endIntersection = getIntersectionPoint(line, true)
        path.lineTo(endIntersection!!.x, endIntersection.y)
    }

    private fun getIntersectionPoint(line: Line, isReverse: Boolean = false) =
        if (isReverse) {
            Intersection.detect(topLine, line) ?: Intersection.detect(bottomLine, line)
            ?: Intersection.detect(rightLine, line) ?: Intersection.detect(leftLine, line)
        } else {
            Intersection.detect(leftLine, line) ?: Intersection.detect(rightLine, line)
            ?: Intersection.detect(bottomLine, line) ?: Intersection.detect(topLine, line)
        }


    fun drawPath(points: List<PointF>) {
        pathPoints.clear()
        pathPoints.addAll(points)
    }

    fun drawCar(point: PointF, zoom: Float, bearing: Double, latLng: LatLng) {
        this.zoom = zoom


        if (carLatLng != latLng && carPointF != null && !isCarAnimation) {
            isCarAnimation = true
            startAnim = SystemClock.uptimeMillis()
            endCarAnimPointF = point
            startCarAnimPointF = carPointF
            startAnimBearing = carBearing
        }
        carBearing = bearing.toFloat()
        carPointF = point
        carLatLng = latLng
        invalidate()
    }

    private fun drawableToBitmap(drawable: Drawable, bearing: Float): Bitmap {
        if (carBitmap == null) {
            carBitmap = if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                Bitmap.createBitmap(
                        drawable.intrinsicWidth,
                        drawable.intrinsicHeight,
                        Bitmap.Config.ARGB_8888
                )
            }
        }

        val scale = obtainScale()
        val scaleBitmap = Bitmap.createScaledBitmap(
                carBitmap!!,
                (drawable.intrinsicWidth * scale).toInt(),
                (drawable.intrinsicHeight * scale).toInt(),
                false
        )
        val matrix = Matrix()
        matrix.postRotate(bearing)
        val copy = scaleBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(copy)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return Bitmap.createBitmap(copy, 0, 0, scaleBitmap.width, scaleBitmap.height, matrix, true)
    }

    private fun obtainScale(): Float {
        val deltaZoom = maxZoomScale - minZoomScale
        val deltaScale = maxScale - minScale
        return deltaScale / deltaZoom * (zoom - minZoomScale) + minScale
    }
}