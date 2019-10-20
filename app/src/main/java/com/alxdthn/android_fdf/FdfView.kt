package com.alxdthn.android_fdf

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.*
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class FdfView: View {
	companion object {
		private const val PI_X2 = 6.28318f
	}

	private val paint = Paint()
	private val map = mutableListOf<MutableList<Vertex>>()
	private val print = mutableListOf<Vertex>()
	private lateinit var scaleGestureDetector: ScaleGestureDetector
	private lateinit var gestureDetector: GestureDetector

	private var winWidth = 0f
	private var winHeight = 0f
	private var sideSize = 0f

	private var scaleFactor = 1f
	private var angleX = 0.523599f
	private var angleY = 0.523599f
	private var angleZ = 0f
	private var maxZ: Float? = null

	private var lastPosX = 0f
	private var lastPosY = 0f

	private var posX = 0f
	private var posY = 0f

	private var isTouched = false

	lateinit var file: String
	private lateinit var input: String
	var colorTop = 0
	var colorBottom = 0

	constructor(context: Context): super(context) {
		init(context)
	}

	@JvmOverloads
	constructor(context: Context, attrs: AttributeSet, defStyle: Int = 0): super(context, attrs, defStyle) {

		val typedArray = context.obtainStyledAttributes(attrs, R.styleable.FdfView, defStyle, 0)

		typedArray.apply {
			file = getString(R.styleable.FdfView_file) ?: ""
			colorTop = getColor(R.styleable.FdfView_colorTop, 0)
			colorBottom = getColor(R.styleable.FdfView_colorBottom, 0)
		}
		typedArray.recycle()

		init(context)
	}

	private fun init(context: Context) {
		scaleGestureDetector = ScaleGestureDetector(context, ScaleGestureListener())
		gestureDetector = GestureDetector(context, GestureListener())

		getInput()
		readInput()
		normalizeMap()
		setConnections()
		preparePrint()

		paint.color = Color.CYAN
		paint.isAntiAlias = true
		paint.style = Paint.Style.STROKE
	}

	private fun getInput() {
		var buffer: ByteArray? = null
		val stream: InputStream
		try {
			stream = context.assets.open(file)
			val size = stream.available()
			buffer = ByteArray(size)
			stream.read(buffer)
			stream.close()
		} catch (e: IOException) {
			e.printStackTrace()
		}
		input = String(buffer!!)
	}

	private fun readInput() {
		val inputSplit = input.split("\n").filter { it.isNotEmpty() }
		var yCounter = 0f

		for (row in inputSplit) {
			val rowSplit = row.split(" ").filter { it.isNotEmpty() }
			val rowFloat = rowSplit.map { it.toFloat() }
			val vertexRow = mutableListOf<Vertex>()
			var xCounter = 0f

			for (value in rowFloat) {
				if (maxZ == null) {
					maxZ = value
				} else if (maxZ != null && value > maxZ!!) {
					maxZ = value
				}
				val vertex = Vertex(xCounter++, yCounter, value, Render(0f, 0f, 0f, 0), mutableListOf(), false)
				vertexRow.add(vertex)
			}
			yCounter++
			map.add(vertexRow)
		}
	}

	private fun normalizeMap() {
		val startX = (map[0].size / 2f - 0.5f) * -1
		var startY = map.size / 2f - 0.5f
		val startZ = maxZ!! / 2f

		for (row in map) {
			for (value in row) {
				value.x += startX
				value.y = startY
				value.z -= startZ
			}
			startY -= 1
		}
	}

	private fun setConnections() {
		var y = 0

		while (y < map.size) {
			var x = 0
			while (x < map[y].size) {
				if (x < map[y].size - 1) {
					map[y][x].connections.add(map[y][x + 1])
					map[y][x + 1].connections.add(map[y][x])
				}
				if (y < map.size - 1) {
					map[y][x].connections.add(map[y + 1][x])
					map[y + 1][x].connections.add(map[y][x])
				}
				x++
			}
			y++
		}
	}

	private fun preparePrint() {
		for (row in map) {
			for (vertex in row) {
				print.add(vertex)
			}
		}
	}

	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		super.onLayout(changed, left, top, right, bottom)

		winWidth = width.toFloat()
		winHeight = height.toFloat()

		getSizeSize()
		render()
	}

	private fun getSizeSize() {
		sideSize = (if (winHeight < winWidth) {
			winHeight / max(map[0].size, map.size)
		} else {
			winWidth / max(map[0].size, map.size)
		} - 0.5f).toInt().toFloat()
	}

	private fun render() {
		paint.strokeWidth = scaleFactor
		for (vertex in print) {
			renderVertex(vertex)
		}
		print.sortBy { it.render.z }
		invalidate()
	}

	private fun renderVertex(vertex: Vertex) {
		setSize(vertex)

		if (angleX != 0f) {
			rotateX(vertex.render)
		}
		if (angleY != 0f) {
			rotateY(vertex.render)
		}
		if (angleZ != 0f) {
			rotateZ(vertex.render)
		}
		vertex.render.x = winWidth / 2f + vertex.render.x
		vertex.render.y = winHeight / 2f - vertex.render.y
		vertex.render.color = if (vertex.z == 0f) colorBottom else colorTop
	}

	private fun rotateY(render: Render) {
		val angleCos = cos(angleY)
		val angleSin = sin(angleY)
		val tmpX = render.x
		val tmpZ = render.z

		render.x = tmpX * angleCos + tmpZ * angleSin
		render.z = -tmpX * angleSin + tmpZ * angleCos
	}

	private fun rotateX(render: Render) {
		val angleCos = cos(angleX)
		val angleSin = sin(angleX)
		val tmpY = render.y
		val tmpZ = render.z

		render.y = tmpY * angleCos + tmpZ * angleSin
		render.z = -tmpY * angleSin + tmpZ * angleCos
	}

	private fun rotateZ(render: Render) {
		val angleCos = cos(angleZ)
		val angleSin = sin(angleZ)
		val tmpX = render.x
		val tmpY = render.y

		render.x = tmpX * angleCos - tmpY * angleSin
		render.y = tmpX * angleSin + tmpY * angleCos
	}

	private fun setSize(vertex: Vertex) {
		vertex.render.x = vertex.x * sideSize * scaleFactor
		vertex.render.y = vertex.y * sideSize * scaleFactor
		vertex.render.z = vertex.z * sideSize * scaleFactor
	}

	override fun onDraw(canvas: Canvas) {
		paint.color = colorTop
		drawLines(canvas)
	}

	private fun drawLines(canvas: Canvas) {
		for (vertex in print) {
			for (connection in vertex.connections) {
				if (!connection.isPrinted) {
					bresenhem(canvas, vertex.render, connection.render)
				}
			}
			vertex.isPrinted = true
		}
		for (vertex in print) {
			vertex.isPrinted = false
		}
	}

	private fun bresenhem(canvas: Canvas, p1: Render, p2: Render) {
		val deltaX = abs(p2.x - p1.x)
		val deltaY = abs(p2.y - p1.y)
		var error = deltaX - deltaY
		var error2: Float
		val point = Render(p1.x, p1.y, p1.z, p1.color)
		val dirX = if (p1.x < p2.x) 1f else -1f
		val dirY = if (p1.y < p2.y) 1f else -1f

		while (point.x <= p2.x && point.y <= p2.y) {
			setPixel(canvas, point, p1, p2, deltaX, deltaY)
			error2 = error * 2
			if (error > -deltaY) {
				error -= deltaY
				point.x += dirX
			} else if (error2 < deltaX) {
				error += deltaX
				point.y += dirY
			}
		}
	}

	private fun setPixel(canvas: Canvas, point: Render, start: Render, end: Render, deltaX: Float, deltaY: Float) {
		//point.color = getGradient(point, start, end, deltaX, deltaY)

		canvas.drawPoint(point.x, point.y, paint)
	}

	private fun getGradient(point: Render, start: Render, end: Render, deltaX: Float, deltaY: Float): Int {
		if (point.color == end.color) {
			return point.color
		}
		val percent = if (deltaX > deltaY) {
			percentage(start.x, end.x, point.x)
		}
		else {
			percentage(start.y, end.y, point.y)
		}
		val red = getLight((start.color shr 16) and 0xFF, (end.color shr 16) and 0xFF, percent)
		val green = getLight((start.color shr 8) and 0xFF, (end.color shr 8) and 0xFF, percent)
		val blue = getLight(start.color and 0xFF, end.color and 0xFF, percent)
		return (red shl 16) or (green shl 8) or blue
	}

	private fun percentage(start: Float, end: Float, cur: Float): Float {
		val placement = cur - start
		val distance = end - start

		return if (distance == 0f) 1.0f else placement / distance
	}

	private fun getLight(start: Int, end: Int, percent: Float): Int {
		return ((1 - percent) * start + percent * end).toInt()
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(event: MotionEvent): Boolean {
		when (event.actionMasked ) {
			MotionEvent.ACTION_DOWN -> {
				if (event.pointerCount == 1) {
					lastPosX = posX
					lastPosY = posY

					posX = event.x
					posY = event.y
				} else if (event.pointerCount == 2) {
					isTouched = true
				}
			}
			MotionEvent.ACTION_UP -> {
				if (event.pointerCount == 1) {
					isTouched = false
				}
			}
		}
		if (gestureDetector.onTouchEvent(event)) return true
		scaleGestureDetector.onTouchEvent(event)
		return true
	}

	inner class ScaleGestureListener: ScaleGestureDetector.SimpleOnScaleGestureListener() {
		private var preScaleFactor = 1f

		override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
			preScaleFactor = scaleFactor
			isTouched = true

			return super.onScaleBegin(detector)
		}

		override fun onScale(detector: ScaleGestureDetector): Boolean {
			scaleFactor = preScaleFactor * detector.scaleFactor
			render()
			return super.onScale(detector)
		}
	}

	inner class GestureListener: GestureDetector.SimpleOnGestureListener() {
		override fun onScroll(
			e1: MotionEvent,
			e2: MotionEvent,
			distanceX: Float,
			distanceY: Float
		): Boolean {
			if (scaleGestureDetector.isInProgress || isTouched) {
				return false
			}
			lastPosX = posX
			lastPosY = posY

			posX = e2.x
			posY = e2.y
			angleY -= (lastPosX - posX) / 300.0f
			angleX += (lastPosY - posY) / 300.0f
			if (angleY > PI_X2 || angleY < -PI_X2) {
				angleY = 0f
			}
			if (angleX > PI_X2 || angleX < -PI_X2) {
				angleX = 0f
			}
			render()
			return super.onScroll(e1, e2, distanceX, distanceY)
		}
	}
}