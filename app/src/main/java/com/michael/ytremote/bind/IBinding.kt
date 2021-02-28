package com.michael.bindit

import io.reactivex.rxjava3.disposables.Disposable

enum class BindingMode {
    OneWay,
    OneWayToSource,
    TwoWay,
}

enum class BoolConvert {
    Staright,   // true --> true
    Inverse,    // true --> false
}

interface IBinding : Disposable {
    val mode:BindingMode
}

