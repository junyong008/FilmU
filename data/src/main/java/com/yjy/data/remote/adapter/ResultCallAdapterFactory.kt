package com.yjy.data.remote.adapter

import com.yjy.domain.model.ApiResult
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class ResultCallAdapterFactory : CallAdapter.Factory() {
    override fun get(
        returnType: Type, annotations: Array<out Annotation>, retrofit: Retrofit
    ): CallAdapter<*, *>? {
        if (getRawType(returnType) != Call::class.java) return null
        check(returnType is ParameterizedType) {
            val name = parseTypeName(returnType)
            "Return 타입은 $name<Foo> 또는 $name<out Foo>로 정의되어야 합니다."
        }

        val wrapperType = getParameterUpperBound(0, returnType)
        if (getRawType(wrapperType) != ApiResult::class.java) return null
        check(wrapperType is ParameterizedType) {
            val name = parseTypeName(returnType)
            "Return 타입은 $name<ResponseBody>로 정의되어야 합니다."
        }

        val bodyType = getParameterUpperBound(0, wrapperType)
        return ApiResultCallAdapter<Any>(bodyType)
    }

    private fun parseTypeName(type: Type) = type.toString().split(".").last()
}