package com.yjy.data.remote.adapter

import com.yjy.domain.model.ApiResult
import okhttp3.Request
import okio.Timeout
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.lang.reflect.Type

internal class ApiResultCallAdapter<R>(
    private val successType: Type
) : CallAdapter<R, Call<ApiResult<R>>> {
    override fun adapt(call: Call<R>): Call<ApiResult<R>> = ApiResultCall(call, successType)

    override fun responseType(): Type = successType
}

private class ApiResultCall<R>(
    private val delegate: Call<R>,
    private val successType: Type
) : Call<ApiResult<R>> {

    override fun enqueue(callback: Callback<ApiResult<R>>) = delegate.enqueue(
        object : Callback<R> {

            override fun onResponse(call: Call<R>, response: Response<R>) {
                callback.onResponse(this@ApiResultCall, Response.success(response.toApiResult()))
            }

            private fun Response<R>.toApiResult(): ApiResult<R> {
                // Http 에러 응답
                if (!isSuccessful) {
                    val errorBody = errorBody()!!.string()
                    return ApiResult.Failure.HttpError(
                        code = code(),
                        message = message(),
                        body = errorBody
                    )
                }

                // Body가 존재하는 Http 성공 응답
                body()?.let { body -> return ApiResult.successOf(body) }

                // successType이 Unit인 경우 Body가 존재하지 않더라도 성공으로 간주합니다.
                return if (successType == Unit::class.java) {
                    @Suppress("UNCHECKED_CAST")
                    ApiResult.successOf(Unit as R)
                } else {
                    ApiResult.Failure.UnknownApiError(
                        IllegalStateException(
                            "응답 코드는 ${code()}이지만, response body 값이 null 입니다.\n" +
                            "만일 null값이 의도된것이라면, 반환 형식을 ApiResult<Unit>과 같이 Unit으로 선언하세요."
                        )
                    )
                }
            }

            override fun onFailure(call: Call<R?>, throwable: Throwable) {
                val error = if (throwable is IOException) {
                    ApiResult.Failure.NetworkError(throwable)
                } else {
                    ApiResult.Failure.UnknownApiError(throwable)
                }
                callback.onResponse(this@ApiResultCall, Response.success(error))
            }
        }
    )

    override fun timeout(): Timeout = delegate.timeout()

    override fun isExecuted(): Boolean = delegate.isExecuted

    override fun clone(): Call<ApiResult<R>> = ApiResultCall(delegate.clone(), successType)

    override fun isCanceled(): Boolean = delegate.isCanceled

    override fun cancel() = delegate.cancel()

    override fun execute(): Response<ApiResult<R>> =
        throw UnsupportedOperationException("This adapter does not support sync execution")

    override fun request(): Request = delegate.request()
}