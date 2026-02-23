package app.launch0.helper

import app.launch0.data.AppModel

interface AppFilterHelper {
    fun onAppFiltered(items:List<AppModel>)
}