package com.example.wallpaper.ui.rx

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.wallpaper.exceptions.InvalidNameException
import com.example.wallpaper.exceptions.InvalidPasswordException
import com.example.wallpaper.ui.base.BaseViewModel
import com.example.wallpaper.utils.checkString
import com.jakewharton.rxbinding2.widget.TextViewTextChangeEvent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


@SuppressLint("CheckResult")
class RxViewModel @Inject constructor(
) : BaseViewModel() {

    private val _textLiveData = MutableLiveData<String>()

    val textLiveData: LiveData<String>
        get() = _textLiveData

    private val disposable: CompositeDisposable by lazy {
        CompositeDisposable()
    }

    fun deBounceOperator(observable: Observable<TextViewTextChangeEvent>) {
        disposable.add(observable.debounce(3, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                _textLiveData.value = it.text().toString()
            })
    }

    fun zipOperator() {
        val observable = Observable.just(1, 2, 3, 4, 5)
        val observable2 = Observable.just("item1", "item2", "item3")

        val builder = StringBuilder()

        disposable.add(
            Observable.zip(observable, observable2, BiFunction<Int, String, Any> { t1, t2 ->
                builder.append("$t1  $t2 \n")
                Timber.d("$t1  $t2")
            }).observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    _textLiveData.value = builder.toString()
                })
    }

    fun backOffRepeatOperator() {
        var value = 0
        disposable.add(Observable.just(1)
            .observeOn(AndroidSchedulers.mainThread())
            .repeatUntil {
                value == 10
            }
            .subscribe {
                Timber.d(value.toString())
                value++
            })
    }

    fun formValidation(
        userNameObservable: Observable<String>,
        passwordObservable: Observable<String>
    ): Observable<Pair<Int, Exception?>> {
        return Observable.combineLatest(
            userNameObservable,
            passwordObservable,
            BiFunction<String, String, Pair<Int, Exception?>> { name, password ->
                Timber.d(isValidForm(name, password).toString())
                isValidForm(name, password)
            })
    }

    private fun isValidForm(name: String, password: String): Pair<Int, Exception?> {
        val checkString = checkString(password)
        return when {
            name.isEmpty() -> Pair(0, InvalidNameException())
            password.isEmpty() -> Pair(0, InvalidPasswordException())
            checkString.second -> Pair(4, null)
            checkString.first == 1 -> Pair(1, InvalidPasswordException())
            checkString.first == 2 -> Pair(2, InvalidPasswordException())
            checkString.first == 3 -> Pair(3, InvalidPasswordException())
            else -> Pair(0, null)
        }
    }

    fun combineLatestOperator() {
        val observable1 = Observable.interval(100, TimeUnit.MILLISECONDS)
        val observable2 = Observable.interval(50, TimeUnit.MILLISECONDS)

        disposable.add(
            Observable.combineLatest<Long, Long, Any>(
                observable1,
                observable2,
                BiFunction { t1, t2 ->
                    Timber.d("$t1  and  $t2")
                }).subscribe { item -> Timber.d(item.toString()) })

        Thread.sleep(1000)
    }

    override fun onCleared() {
        super.onCleared()
        if (!disposable.isDisposed)
            disposable.dispose()
    }

}