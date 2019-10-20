package com.alxdthn.android_fdf

class Render(
	var x: Float,
	var y: Float,
	var z: Float
)

class Vertex(
	var x: Float,
	var y: Float,
	var z: Float,
	val render: Render,
	val connections: MutableList<Vertex>,
	var isPrinted: Boolean
)