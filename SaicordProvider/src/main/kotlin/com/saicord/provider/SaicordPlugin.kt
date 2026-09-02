package com.saicord.provider

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import android.content.Context

/**
 * Saicord Plugin Registration
 * This class registers both Bengali and Hindi providers with CloudStream.
 */
@CloudstreamPlugin
class SaicordPlugin : com.lagradost.cloudstream3.plugins.Plugin() {
    /**
     * Initialize the plugin and register providers
     */
    override fun initialize(context: Context) {
        // Register Bengali provider
        registerMainAPI(SaicordBn())

        // Register Hindi provider
        registerMainAPI(SaicordHi())
    }
}
