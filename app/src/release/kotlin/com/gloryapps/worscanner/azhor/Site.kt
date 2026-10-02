package com.gloryapps.worscanner.azhor

import com.gloryapps.worscanner.BuildConfig

/** The site a release links to and sends scans to, at the address the build was given. */
fun site(): AzhorApi = HttpAzhorApi(BuildConfig.AZHOR_URL)
