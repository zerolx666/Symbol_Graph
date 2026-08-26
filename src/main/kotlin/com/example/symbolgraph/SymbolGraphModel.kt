package com.example.symbolgraph

data class GraphNode(
    val id: String,
    val title: String,
    val fileName: String,
    val path: String,
    val offset: Int,
    val definition: Boolean
)

data class GraphModel(val definition: GraphNode, val usages: List<GraphNode>)
