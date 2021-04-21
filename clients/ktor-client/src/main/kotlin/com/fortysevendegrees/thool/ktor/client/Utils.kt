package com.fortysevendegrees.thool.ktor.client

import com.fortysevendegrees.thool.model.Method
import io.ktor.http.HttpMethod

fun Method.toMethod(): HttpMethod =
  when (this) {
    Method.GET -> HttpMethod.Get
    Method.HEAD -> HttpMethod.Head
    Method.POST -> HttpMethod.Post
    Method.PUT -> HttpMethod.Put
    Method.DELETE -> HttpMethod.Delete
    Method.OPTIONS -> HttpMethod.Options
    Method.PATCH -> HttpMethod.Patch
    Method.CONNECT -> HttpMethod("CONNECT")
    Method.TRACE -> HttpMethod("TRACE")
    else -> HttpMethod(value)
  }
