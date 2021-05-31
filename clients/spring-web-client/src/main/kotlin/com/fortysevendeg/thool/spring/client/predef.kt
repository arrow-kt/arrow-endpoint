package com.fortysevendeg.thool.spring.client

import com.fortysevendeg.thool.model.Method
import org.springframework.http.HttpMethod

// Extract method, and use GET as default
public fun Method.method(): HttpMethod? =
  when (this) {
    Method.GET -> HttpMethod.GET
    Method.HEAD -> HttpMethod.HEAD
    Method.POST -> HttpMethod.POST
    Method.PUT -> HttpMethod.PUT
    Method.DELETE -> HttpMethod.DELETE
    Method.OPTIONS -> HttpMethod.OPTIONS
    Method.PATCH -> HttpMethod.PATCH
    Method.TRACE -> HttpMethod.TRACE
    Method.CONNECT -> null
    else -> null
  }
