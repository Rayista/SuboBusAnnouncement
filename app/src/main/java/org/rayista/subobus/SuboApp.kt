package org.rayista.subobus

import android.app.Application
import org.rayista.subobus.vm.BusViewModel

/**
 * 应用级单例。
 *
 * 报站状态、GPS 监听、播放器都放在这里，而不是绑定在 Activity 上，
 * 这样按返回键退出界面、或者系统回收界面之后，后台服务仍能继续报站。
 */
class SuboApp : Application() {

    val busVm: BusViewModel by lazy { BusViewModel(this) }
}
