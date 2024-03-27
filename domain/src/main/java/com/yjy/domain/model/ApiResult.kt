package com.yjy.domain.model

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>

    sealed interface Failure : ApiResult<Nothing> {
        data class HttpError(val code: Int, val message: String, val body: String) : Failure

        data class NetworkError(val throwable: Throwable) : Failure

        data class UnknownApiError(val throwable: Throwable) : Failure

        fun safeThrowable(): Throwable = when (this) {
            is HttpError -> IllegalStateException("$message $body")
            is NetworkError -> throwable
            is UnknownApiError -> throwable
        }
    }

    fun isSuccess(): Boolean = this is Success
    fun isFailure(): Boolean = this is Failure

    fun successOrThrow(): T {
        throwOnFailure()
        return (this as Success).data
    }
    fun failureOrThrow(): Failure {
        throwOnSuccess()
        return this as Failure
    }
    companion object {
        fun <R> successOf(result: R): ApiResult<R> = Success(result)
    }
}
inline fun <T> ApiResult<T>.onSuccess(action: (value: T) -> Unit): ApiResult<T> {
    if (isSuccess()) action(successOrThrow())
    return this
}
inline fun <T> ApiResult<T>.onFailure(action: (error: ApiResult.Failure) -> Unit): ApiResult<T> {
    if (isFailure()) action(failureOrThrow())
    return this
}

internal fun ApiResult<*>.throwOnSuccess() {
    if (this is ApiResult.Success) throw IllegalStateException("Cannot be called under Success conditions.")
}
internal fun ApiResult<*>.throwOnFailure() {
    if (this is ApiResult.Failure) throw safeThrowable()
}

inline fun <T, R> ApiResult<T>.map(transform: (value: T) -> R): ApiResult<R> =
    when (this) {
        is ApiResult.Success -> ApiResult.Success(transform(data))
        is ApiResult.Failure -> this
    }