package com.saicord

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class SaicordBnPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(SaicordBn())
    }
}
