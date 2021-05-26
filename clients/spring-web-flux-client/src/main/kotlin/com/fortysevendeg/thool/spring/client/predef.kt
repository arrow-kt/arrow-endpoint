package com.fortysevendeg.thool.spring.client

import com.fortysevendeg.thool.model.Method
import org.springframework.http.HttpMethod

// Extract method, and use GET as default
public fun Method.method(): HttpMethod? =
  when (this.value) {
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
