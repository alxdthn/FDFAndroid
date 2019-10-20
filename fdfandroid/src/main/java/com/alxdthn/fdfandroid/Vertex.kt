package com.alxdthn.fdfandroid

class Render(
	var x: Float,
	var y: Float,
	var z: Float,
	var color: Int
)

class Vertex(
	var x: Float,
	var y: Float,
	var z: Float,
	val render: Render,
	val connections: MutableList<Vertex>,
	var isPrinted: Boolean
)