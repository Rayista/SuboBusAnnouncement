package org.rayista.subobus.ui

import org.rayista.subobus.i18n.AppStrings

/**
 * 线路显示名：只有结尾是数字时才自动加“路”（英文不加上缀），
 * 例如 1 -> “1 路” / “1”，通勤1号 -> “通勤1号”（原样）。
 */
internal fun routeLabel(route: String, t: AppStrings): String =
    if (route.isNotEmpty() && route.last().isDigit()) route + t.routeSuffix else route
