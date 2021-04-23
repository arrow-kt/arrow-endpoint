package com.fortysevendegrees.thool.spring.client

import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.model.Method
import org.springframework.http.HttpMethod

// Extract method, and use GET as default
public fun Endpoint<*, *, *>.method(): HttpMethod? =
  when (input.method()?.value ?: Method.GET.value) {
    Method.GET.value -> HttpMethod.GET
    Method.HEAD.value -> HttpMethod.HEAD
    Method.POST.value -> HttpMethod.POST
    Method.PUT.value -> HttpMethod.PUT
    Method.DELETE.value -> HttpMethod.DELETE
    Method.OPTIONS.value -> HttpMethod.OPTIONS
    Method.PATCH.value -> HttpMethod.PATCH
    Method.TRACE.value -> HttpMethod.TRACE
    Method.CONNECT.value -> null
    else -> null
  }

public fun String.trimLastSlash(): String =
  if (this.lastOrNull() == '/') dropLast(1) else this
