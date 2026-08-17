package com.tarzo.ai.util

/**
 * A sealed class representing the result of an asynchronous operation.
 * Provides Success, Error, and Loading states with typed data.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
        is Loading -> throw IllegalStateException("Result is still loading")
    }

    fun exceptionOrNull(): Throwable? = when (this) {
        is Error -> exception
        else -> null
    }

    fun errorMessageOrNull(): String? = when (this) {
        is Error -> message ?: exception.message
        else -> null
    }

    fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> try {
            Success(transform(data))
        } catch (e: Exception) {
            Error(e)
        }
        is Error -> Error(exception, message)
        is Loading -> Loading
    }

    fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> try {
            transform(data)
        } catch (e: Exception) {
            Error(e)
        }
        is Error -> Error(exception, message)
        is Loading -> Loading
    }

    fun getOrElse(defaultValue: (Throwable) -> @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> defaultValue(exception)
        is Loading -> defaultValue(IllegalStateException("Loading"))
    }

    fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    fun onError(action: (Throwable) -> Unit): Result<T> {
        if (this is Error) action(exception)
        return this
    }

    fun onLoading(action: () -> Unit): Result<T> {
        if (this is Loading) action()
        return this
    }

    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun error(exception: Throwable, message: String? = null): Result<Nothing> = Error(exception, message)
        fun <T> loading(): Result<T> = Loading
    }
}

fun <T> Result<T>.fold(
    onSuccess: (T) -> Unit,
    onError: (Throwable, String?) -> Unit,
    onLoading: () -> Unit
) {
    when (this) {
        is Result.Success -> onSuccess(data)
        is Result.Error -> onError(exception, message)
        is Result.Loading -> onLoading()
    }
}
